package io.hyperfoil.tools.parse.cli;

import io.hyperfoil.tools.parse.file.FileRule;
import io.hyperfoil.tools.parse.file.Filter;
import io.hyperfoil.tools.parse.file.MatchCriteria;
import io.hyperfoil.tools.parse.yaml.ExpConstruct;
import io.hyperfoil.tools.parse.yaml.FileRuleConstruct;
import io.hyperfoil.tools.parse.yaml.FilterConstruct;
import io.hyperfoil.tools.parse.yaml.MatchCriteriaConstruct;
import io.hyperfoil.tools.yaup.AsciiArt;
import io.hyperfoil.tools.yaup.Sets;
import io.hyperfoil.tools.yaup.StringUtil;
import io.hyperfoil.tools.yaup.file.FileUtility;
import io.hyperfoil.tools.yaup.json.Json;
import io.hyperfoil.tools.yaup.json.JsonValidator;
import io.hyperfoil.tools.yaup.json.graaljs.JsException;
import io.hyperfoil.tools.yaup.time.SystemTimer;
import io.hyperfoil.tools.yaup.yaml.MapRepresenter;
import io.hyperfoil.tools.yaup.yaml.OverloadConstructor;
import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;
import io.quarkus.runtime.Quarkus;
import org.slf4j.ext.XLogger;
import org.slf4j.ext.XLoggerFactory;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.nodes.Tag;
import picocli.CommandLine;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

//@CommandDefinition(name = "parse",description = "parse text files into structured json")
@QuarkusMain
@CommandLine.Command(name = "parse", mixinStandardHelpOptions = true, version = "0.1.13",
        description = "parse structured json from text files")
public class ParseCommand implements Callable<Integer>, QuarkusApplication {

   final static XLogger logger = XLoggerFactory.getXLogger(MethodHandles.lookup().lookupClass());

   private class RuleRunner implements Runnable{

      private final Semaphore semaphore;
      private final List<FileRule> fileRules;
      private final SystemTimer systemTimer;
      private String sourcePath;
      private final int maxPermits;

      public RuleRunner(List<FileRule> fileRules, SystemTimer systemTimer, Semaphore semaphore, int maxPermits){
         this.fileRules = fileRules;
         this.systemTimer = systemTimer;
         this.semaphore = semaphore;
         this.maxPermits = maxPermits;
      }
      public void setSourcePath(String sourcePath){
         this.sourcePath = sourcePath;
      }

      @Override
      public void run() {
         if( !Files.exists(Paths.get(sourcePath)) ){
            logger.error("cannot access "+sourcePath);
            return;
         }
         int neededMb = 0;
         try {
            neededMb = (int) (FileUtility.getInputSize(sourcePath) / (1024 * 1024));
         }catch(Exception e){
            e.printStackTrace();
         }
         int acquire = Math.min(maxPermits,2*neededMb);
         try {
            semaphore.acquire(acquire);
            try {
               logger.info("Starting {}", sourcePath);
               SystemTimer thisTimer = systemTimer.start(sourcePath, true);
               List<String> entries = FileUtility.isArchive(sourcePath) ?
                  FileUtility.getArchiveEntries(sourcePath).stream().map(entry -> sourcePath + FileUtility.ARCHIVE_KEY + entry).collect(Collectors.toList()) :
                  FileUtility.getFiles(sourcePath, "", true);

               Json result = new Json();

               for (String entry : entries) {
                  for (FileRule rule : fileRules) {
                     try {
                        rule.apply(entry, thisTimer, (nest, json) -> {
                           try {
                              if (nest == null || nest.trim().isEmpty()) {
                                 if (!json.isArray()) {
                                    if (result.isEmpty() || !result.isArray()) {
                                       result.merge(json);
                                    } else { //result is an array with entries
                                       result.add(json);
                                    }
                                 } else { //json is an array
                                    if (result.isEmpty() || result.isArray()) {
                                       json.forEach((Consumer<Object>) result::add);
                                    } else {//result is an object but json is an array
                                       logger.error("cannot add an array to an object without a key");
                                    }
                                 }
                              } else if (StringUtil.isJsFnLike(nest)){
                                 try {
                                    StringUtil.jsEval(nest,result,json,state);
                                 }catch (JsException jse){
                                    logger.error(jse.getMessage());
                                 }

                              } else {
                                 Json.chainMerge(result, nest, json);
                              }
                           }catch(Throwable e){
                              logger.error("Exception applying rule=" + rule.getName() + " entry=" + entry,e);
                           }
                        });
                     } catch (Throwable e) {
                        logger.error("Exception for rule=" + rule.getName() + " entry=" + entry,e);
                     }
                  }
               }
               if (result.isEmpty()) {
                  logger.error("failed to match rules to {}", sourcePath);
               }
               String sourceDestination = batch.size() > 1 ? null : destination;
               if (sourceDestination == null || sourceDestination.isEmpty()) {
                  if (FileUtility.isArchive(sourcePath)) {
                     sourceDestination = sourcePath;
                     if (sourceDestination.endsWith(".zip")) {
                        sourceDestination = sourceDestination.substring(0, sourceDestination.lastIndexOf(".zip"));
                     }
                     if (sourceDestination.endsWith(".tar.gz")) {
                        sourceDestination = sourceDestination.substring(0, sourceDestination.lastIndexOf(".tar.gz"));
                     }
                     sourceDestination = sourceDestination + ".json";
                  } else {
                     Path path = Paths.get(sourcePath);
                     Path effectivePath = path;
                     if (!path.isAbsolute()) {
                        Path base = Paths.get("");
                        effectivePath = base.resolve(path).toAbsolutePath().normalize();
                     }
                     if(effectivePath.toFile().isDirectory()){
                        sourceDestination = effectivePath.toFile().getName()+".json";
                     }else{
                        sourceDestination = sourcePath.endsWith("/") ? sourcePath.substring(0, sourcePath.length() - 1) + ".json" : sourcePath + ".json";
                     }
                  }
               }
               logger.info("writing to {}", sourceDestination);
               Path parentPath = Paths.get(sourceDestination).toAbsolutePath().getParent();
               if (!parentPath.toFile().exists()) {
                  parentPath.toFile().mkdirs();
               }
               try {
                  Files.write(Paths.get(sourceDestination), result.toString(0).getBytes());
               } catch (IOException e) {
                  logger.error("failed to write to {}", sourceDestination);
                  e.printStackTrace();
               }

               thisTimer.stop();
            }catch(Exception e){
               e.printStackTrace();
            }finally{
               semaphore.release(acquire);
            }


         } catch (InterruptedException e) {
            e.printStackTrace();
         }



      }
   }
   //@Option(shortName = 't', name="threads", description = "number of parallel threads for parsing sources",defaultValue = "-1")
   @CommandLine.Option(names={"-t", "--threads"}, defaultValue = "-1")
   Integer threadCount;

   //@Option(name = "disableDefault", description = "disables default rules", hasValue = false)
   @CommandLine.Option(names = {"--disableDefaults"},description = "disables default rules")
   Boolean disableDefault = false;


   //@OptionList(shortName = 'r', name = "rules", description = "parse rule definitions")
   @CommandLine.Option(names= {"-r", "--rules"},description = "parse rule definitions")
   Set<String> config;

   //@Option(shortName = 's', name="source",description = "source of files to scan, supports folder or archive")
   @CommandLine.Option(names = {"-s","--source"},description = "source of files to scan, supports folder or archive")
   String source;

   //@OptionList(shortName = 'b',name="batch",description = "batch of files to individually scan")
   @CommandLine.Option(names = {"-b","--batch"},description = "batch of files to individually scan")
   Set<String> batch;

   //@Option(shortName = 'd', name="destination",description = "destination for the resulting json")
   @CommandLine.Option(names = {"-d","--destination"}, description = "destination for the resulting json")
   String destination;

   //@OptionGroup(shortName = 'S', name="state",description = "state variables for patterns",defaultValue = {  })
   @CommandLine.Option(names = { "-S", "--state"}, description = "state variables for patterns")
   Map<String,String> state;


   @Override
   public Integer call() throws Exception {
      SystemTimer systemTimer = new SystemTimer("fileparser");
      List<FileRule> fileRules = new ArrayList<>();
      JsonValidator validator = getValidator();
      systemTimer.start("load config");
      if(config== null ||  config.isEmpty()) {
         config = new HashSet<>();
      }
      //Load default rules
      if(!disableDefault) {
         logger.info("loading default rules");
         try (InputStreamReader fileStream = new InputStreamReader(ParseCommand.class.getClassLoader().getResourceAsStream("defaultRules.yaml"))) {
            try (BufferedReader reader = new BufferedReader(fileStream)) {
               String content = reader.lines().collect(Collectors.joining("\n"));

               Json loaded = Json.fromYaml(content);
               if (loaded.isArray()) {
                  loaded.forEach(entry -> {
                     if (entry instanceof Json) {
                        Json entryJson = (Json) entry;
                        Json errors = validator.validate(entryJson);
                        if (!errors.isEmpty()) {
                           logger.error("Errors\n" + errors.toString(2));
                           System.exit(1);
                        }
                        FileRule rule = FileRule.fromJson(entryJson, Json.toObjectMap(Json.fromMap(state)));
                        if (rule != null) {
                           fileRules.add(rule);
                        }
                     } else {
                        logger.error("cannot load rules from " + entry);
                     }

                  });
               } else {
                  FileRule rule = FileRule.fromJson(loaded, Json.toObjectMap(Json.fromMap(state)));
                  if (rule != null) {
                     fileRules.add(rule);
                  }
               }

            }
         } catch (IOException e) {
            e.printStackTrace();
         }
      }

      if(batch == null){
         batch = new LinkedHashSet<>();
      }
      if(batch.isEmpty()){
         if (source == null || source.isEmpty() || !Files.exists(Paths.get(source))) {
            logger.error("source cannot be found: " + source);
            return 1;
         }else{
            batch.add(source);
         }
      }else{
         if(source != null && !source.isEmpty()){
            batch.add(source);
         }
      }
      if(config != null){
         config.forEach(configPath->{
            Json loaded = configPath.endsWith("yaml") || configPath.endsWith("yml") ? Json.fromYamlFile(configPath) : Json.fromFile(configPath);
            if (loaded.isEmpty()) {
               logger.error("failed to load content from {}", configPath);
            } else if (loaded.isArray()) {
               loaded.forEach(entry -> {
                  if (entry instanceof Json) {
                     Json entryJson = (Json) entry;
                     Json errors = validator.validate(entryJson);
                     if (!errors.isEmpty()) {
                        logger.error("Errors\n"+errors.toString(2));
                        System.exit(1);
                     }
                     FileRule rule = FileRule.fromJson(entryJson, Json.toObjectMap(Json.fromMap(state)));
                     if (rule != null) {
                        fileRules.add(rule);
                     }
                  } else {
                     logger.error("cannot create rule from {}", entry.toString());
                     System.exit(1);
                  }
               });
            } else {
               Json errors = validator.validate(loaded);
               if (!errors.isEmpty()) {
                  logger.error("Errors\n"+errors.toString(2));
                  System.exit(1);
               }
               FileRule rule = FileRule.fromJson(loaded, Json.toObjectMap(Json.fromMap(state)));
               fileRules.add(rule);
            }
         });
      }
      //loaded all the file rules
      if(fileRules.isEmpty()){
         logger.error("failed to load any rules");
         return 1;
      }

      int heapMb = (int)(Runtime.getRuntime().maxMemory() / (1024*1024) );

      Semaphore heapSemaphore = new Semaphore(heapMb);

      int numOfThread = threadCount !=null && threadCount > 0 ? threadCount : Runtime.getRuntime().availableProcessors();
      int blockQueueSize = 2*numOfThread;
      BlockingQueue<Runnable> blockingQueue = new ArrayBlockingQueue<Runnable>(blockQueueSize);
      RejectedExecutionHandler rejectedExecutionHandler = new ThreadPoolExecutor.CallerRunsPolicy();
      ExecutorService executorService =  new ThreadPoolExecutor(numOfThread, numOfThread,
         0L, TimeUnit.MINUTES, blockingQueue, rejectedExecutionHandler);
      SystemTimer parseTimer = systemTimer.start("parse");

      for(String sourcePath : batch){
         RuleRunner ruleRunner = new RuleRunner(fileRules,parseTimer, heapSemaphore, heapMb);
         ruleRunner.setSourcePath(sourcePath);
         executorService.submit(ruleRunner);
      }
      executorService.shutdown();
      executorService.awaitTermination(1,TimeUnit.DAYS);
      systemTimer.stop();

      logger.info(systemTimer.getJson().toString(2));

      return 0;
   }
   @Override
   public int run(String[] args) {
//      AeshRuntimeRunner.builder().command(ParseCommand.class).args(args).execute();
      String cmdLineSyntax = "";
      cmdLineSyntax =
      "java -jar " +
         (new File(ParseCommand.class
            .getProtectionDomain()
            .getCodeSource()
            .getLocation()
            .getPath()
         )).getName() +
         " " +
         cmdLineSyntax;
      int exitCode = new CommandLine(new ParseCommand()).execute(args);
      if(exitCode!=0){
         CommandLine.usage(new ParseCommand(), System.out);
      }
      return exitCode;
   }

   public static JsonValidator getValidator() {
      try (InputStreamReader fileStream = new InputStreamReader(ParseCommand.class.getClassLoader().getResourceAsStream("filerule-schema.json"))) {
         try (BufferedReader reader = new BufferedReader(fileStream)) {
            String content = reader.lines().collect(Collectors.joining("\n"));

            Json schemaJson = Json.fromString(content);

            JsonValidator validator = new JsonValidator(schemaJson);
            return validator;
         }
      } catch (IOException e) {
         e.printStackTrace();
         System.exit(1);
      }
      return null;
   }

   public static Yaml getYaml() {
      OverloadConstructor constructor = new OverloadConstructor();
      constructor.setExactMatchOnly(false);
      MapRepresenter mapRepresenter = new MapRepresenter();
      DumperOptions dumperOptions = new DumperOptions();
      dumperOptions.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
      dumperOptions.setWidth(1024);
      dumperOptions.setIndent(2);
      Yaml yaml = new Yaml(constructor, mapRepresenter, dumperOptions);
      ExpConstruct expConstruct = new ExpConstruct();
      constructor.addConstruct(new Tag("exp"), expConstruct);
      constructor.addConstruct(new Tag(Exp.class), expConstruct);
      mapRepresenter.addMapping(Exp.class, ExpConstruct.MAPPING);

      FileRuleConstruct fileRuleConstruct = new FileRuleConstruct();
      constructor.addConstruct(new Tag("rule"), fileRuleConstruct);
      constructor.addConstruct(new Tag(FileRule.class), fileRuleConstruct);
      constructor.addMapKeys(new Tag("rule"), Sets.of("asText"));
      constructor.addMapKeys(new Tag("rule"), Sets.of("asJbossCli"));
      constructor.addMapKeys(new Tag("rule"), Sets.of("asJson"));
      constructor.addMapKeys(new Tag("rule"), Sets.of("asXml"));
      constructor.addMapKeys(new Tag("rule"), Sets.of("asPath"));


      //TODO FileRuleConstruct.MAPPING

      MatchCriteriaConstruct matchCriteriaConstruct = new MatchCriteriaConstruct();
      constructor.addConstruct(new Tag("match"), matchCriteriaConstruct);
      constructor.addConstruct(new Tag(MatchCriteria.class), matchCriteriaConstruct);
      //TODO MatchCriteriaConstruct.MAPPING

      FilterConstruct filterConstruct = new FilterConstruct();
      constructor.addConstruct(new Tag("filter"), filterConstruct);
      constructor.addConstruct(new Tag(Filter.class), filterConstruct);
      //TODO FilterConstruct.MAPPING


      return yaml;
   }


}
