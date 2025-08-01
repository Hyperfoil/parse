package io.hyperfoil.tools.parse.cli;

import io.hyperfoil.tools.parse.Exp;
import io.hyperfoil.tools.parse.file.FileRule;
import io.hyperfoil.tools.parse.file.Filter;
import io.hyperfoil.tools.parse.file.MatchCriteria;
import io.hyperfoil.tools.parse.yaml.ExpConstruct;
import io.hyperfoil.tools.parse.yaml.FileRuleConstruct;
import io.hyperfoil.tools.parse.yaml.FilterConstruct;
import io.hyperfoil.tools.parse.yaml.MatchCriteriaConstruct;
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
import org.jboss.logging.Logger;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.nodes.Tag;
import picocli.AutoComplete;
import picocli.CommandLine;

import java.io.BufferedReader;
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

@QuarkusMain
@CommandLine.Command(name="parse", description = "parse text documents into structured json", mixinStandardHelpOptions = true, subcommands={CommandLine.HelpCommand.class, AutoComplete.GenerateCompletion.class})
public class ParsePico  implements Callable<Integer>, QuarkusApplication {

    final static Logger logger = Logger.getLogger(MethodHandles.lookup().lookupClass());

    private class RuleRunner implements Runnable {

        private final Semaphore semaphore;
        private final List<FileRule> fileRules;
        private final SystemTimer systemTimer;
        private String sourcePath;
        private final int maxPermits;

        public RuleRunner(List<FileRule> fileRules, SystemTimer systemTimer, Semaphore semaphore, int maxPermits) {
            this.fileRules = fileRules;
            this.systemTimer = systemTimer;
            this.semaphore = semaphore;
            this.maxPermits = maxPermits;
        }

        public void setSourcePath(String sourcePath) {
            this.sourcePath = sourcePath;
        }

        @Override
        public void run() {
            //https://github.com/oracle/graaljs/issues/764
            System.setProperty("polyglotimpl.DisableClassPathIsolation", "true");

            if (!Files.exists(Paths.get(sourcePath))) {
                logger.error("cannot access " + sourcePath);
                return;
            }
            int neededMb = 0;
            try {
                neededMb = (int) (FileUtility.getInputSize(sourcePath) / (1024 * 1024));
            } catch (Exception e) {
                e.printStackTrace();
            }
            int acquire = Math.min(maxPermits, 2 * neededMb);
            try {
                semaphore.acquire(acquire);
                try {
                    logger.infof("Starting %s", sourcePath);
                    SystemTimer runnerTimer = systemTimer.start(sourcePath, true);
                    List<String> entries = FileUtility.isArchive(sourcePath) ?
                            FileUtility.getArchiveEntries(sourcePath).stream().map(entry -> sourcePath + FileUtility.ARCHIVE_KEY + entry).collect(Collectors.toList()) :
                            FileUtility.getFiles(sourcePath, "", true, scanArchives);
                    Json result = new Json(false);

                    for (String entry : entries) {
                        SystemTimer entryTimer = runnerTimer.start(entry);
                        for (FileRule rule : fileRules) {
                            try {
                                entryTimer.start(rule.getName());
                                rule.apply(entry, (nest, json) -> {
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
                                        } else if (StringUtil.isJsFnLike(nest)) {
                                            try {
                                                StringUtil.jsEval(nest, result, json, state);
                                            } catch (JsException jse) {
                                                logger.error("Nest javascript error for rule=" + rule.getName() + "\n" + nest, jse);

                                            }

                                        } else {
                                            Json.chainMerge(result, nest, json);
                                        }
                                    } catch (Throwable e) {
                                        logger.error("Exception applying rule=" + rule.getName() + " entry=" + entry, e);
                                    }
                                });
                            } catch (Throwable e) {
                                logger.error("Exception for rule=" + rule.getName() + " entry=" + entry, e);
                            }
                        }
                    }
                    runnerTimer.start("merging");
                    if (result.isEmpty()) {
                        logger.errorf("failed to match rules to %s", sourcePath);
                    }
                    String sourceDestination = todo.size() > 1 ? null : destination;
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
                            sourceDestination = sourcePath.endsWith("/") ? sourcePath.substring(0, sourcePath.length() - 1) + ".json" : sourcePath + ".json";
                        }
                    }
                    logger.infof("writing to %s", sourceDestination);
                    Path parentPath = Paths.get(sourceDestination).toAbsolutePath().getParent();
                    if (!parentPath.toFile().exists()) {
                        parentPath.toFile().mkdirs();
                    }
                    try {
                        Files.write(Paths.get(sourceDestination), result.toString(prettyPrint).getBytes());
                    } catch (IOException e) {
                        logger.errorf("failed to write to %s", sourceDestination);
                        //e.printStackTrace();
                    }

                    runnerTimer.stop();
                } catch (Exception e) {
                    logger.error("un-handled exception from scanner thread", e);
                } finally {
                    semaphore.release(acquire);
                }
            } catch (InterruptedException e) {
                logger.error("interrupted while running scanner thread");
                //e.printStackTrace();
            }
        }
    }

    static class SourceGroup {
        //@Option(shortName = 's', name="source",description = "source of files to scan, supports folder or archive")
        @CommandLine.Option(names = {"-s","--source"},description = "source of files to scan, supports folder or archive")
        String source;

        //@OptionList(shortName = 'b',name="batch",description = "batch of files to individually scan")
        @CommandLine.Option(names = {"-b","--batch"},description = "batch of files to individually scan")
        Set<String> batch;

    }
    @CommandLine.ArgGroup(exclusive = true, multiplicity = "1")
    SourceGroup scanSource;

    @CommandLine.Option(names={"-t", "--threads"}, defaultValue = "-1")
    Integer threadCount;

    //@Option(name = "disableDefault", description = "disables default rules", hasValue = false)
    @CommandLine.Option(names = {"--disableDefaults"},description = "disables default rules", defaultValue = "false")
    boolean disableDefault;

    @CommandLine.Option(names = {"--scanArchives"}, description = "scan inside archives (tar, gzip, ...)", defaultValue = "false")
    boolean scanArchives;

    @CommandLine.Option(names = {"--warn-unparsed"}, description = "warn when there are unparsed portions of a line", defaultValue = "false")
    boolean warnUnparsed;

    //@OptionList(shortName = 'r', name = "rules", description = "parse rule definitions")
    @CommandLine.Option(names= {"-r", "--rules"},description = "parse rule definitions")
    Set<String> config;


    //@Option(shortName = 'd', name="destination",description = "destination for the resulting json")
    @CommandLine.Option(names = {"-d","--destination"}, description = "destination for the resulting json")
    String destination;

    //@OptionGroup(shortName = 'S', name="state",description = "state variables for patterns",defaultValue = {  })
    @CommandLine.Option(names = { "-S", "--state"}, description = "state variables for patterns")
    Map<String,String> state;

    @CommandLine.Option(names = {"--pretty-print"}, description = "number of spaces for pretty-printing. 0 disables pretty-print", defaultValue = "0")
    int prettyPrint;


    private Set<String> todo = new LinkedHashSet<>();

    @Override
    public Integer call() throws Exception {
        SystemTimer systemTimer = new SystemTimer("fileparse");
        List<FileRule> fileRules = new ArrayList<>();
        JsonValidator validator = getValidator();
        systemTimer.start("load config");
        if(config== null ||  config.isEmpty()) {
            config = new HashSet<>();
        }
        //Load default rules
        if(!disableDefault) {
            logger.info("loading default rules");
            try (InputStreamReader fileStream = new InputStreamReader(ParsePico.class.getClassLoader().getResourceAsStream("defaultRules.yaml"))) {
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
                                if(warnUnparsed){

                                }

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
        if(scanSource.batch == null || scanSource.batch.isEmpty()){
            if (scanSource.source == null || scanSource.source.isEmpty() || !Files.exists(Paths.get(scanSource.source))) {
                logger.error("source cannot be found: " + scanSource.source);
                return 1;
            }else{
                todo.add(scanSource.source);
            }
        }
        if(scanSource.batch != null){
            todo.addAll(scanSource.batch);
        }
        if(todo.isEmpty()){
            logger.error("source and batch did not resolve to existing files");
            return 1;
        }
        if(config != null){
            int ec = config.stream().mapToInt(configPath->{
                logger.info("loading: "+configPath);
                Json loaded = configPath.endsWith("yaml") || configPath.endsWith("yml") ? Json.fromYamlFile(configPath) : Json.fromFile(configPath);
                if (loaded.isEmpty()) {
                    logger.errorf("failed to load content from %s", configPath);
                    return 1;
                } else if (loaded.isArray()) {
                    return loaded.stream().mapToInt(entry -> {
                        Object value = entry.getValue();
                        if (value instanceof Json) {
                            Json valueJson = (Json) value;
                            Json errors = validator.validate(valueJson);
                            if (!errors.isEmpty()) {
                                logger.error("Errors\n"+errors.toString(2));
                                return 1;
                            }
                            FileRule rule = FileRule.fromJson(valueJson, Json.toObjectMap(Json.fromMap(state)));
                            if(warnUnparsed){

                            }
                            if (rule != null) {
                                fileRules.add(rule);
                            }
                        } else {
                            logger.errorf("cannot create rule from %s", value.toString());
                            return 1;
                        }
                        return 0;
                    }).max().orElse(0);

                } else {
                    Json errors = validator.validate(loaded);
                    if (!errors.isEmpty()) {
                        logger.error("Errors\n"+errors.toString(2));
                        return 1;
                    }
                    FileRule rule = FileRule.fromJson(loaded, Json.toObjectMap(Json.fromMap(state)));
                    fileRules.add(rule);
                }
                return 0;
            }).max().orElse(0);
            if(ec > 0){
                return ec;
            }
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

        for(String sourcePath : todo){
            RuleRunner ruleRunner = new RuleRunner(fileRules,parseTimer, heapSemaphore, heapMb);
            ruleRunner.setSourcePath(sourcePath);
            executorService.submit(ruleRunner);
        }

        executorService.shutdown();
        executorService.awaitTermination(1,TimeUnit.DAYS);
        systemTimer.stop();
        try {
            Files.write(Paths.get(System.getProperty("user.dir"),"parse-timers.json"),systemTimer.getJson().toString().getBytes());
        } catch (IOException e) {
            logger.error("failed to write parse-timers");
        }
        return 0;
    }


    @Override
    public int run(String... args) throws Exception {
        System.setProperty("polyglotimpl.DisableClassPathIsolation", "true");
        CommandLine cmd = new CommandLine(new ParsePico());
        CommandLine gen = cmd.getSubcommands().get("generate-completion");
        gen.getCommandSpec().usageMessage().hidden(true);
        return cmd.execute(args);
    }

    public static JsonValidator getValidator() {
        try (InputStreamReader fileStream = new InputStreamReader(ParsePico.class.getClassLoader().getResourceAsStream("filerule-schema.json"))) {
            try (BufferedReader reader = new BufferedReader(fileStream)) {
                String content = reader.lines().collect(Collectors.joining("\n"));
                Json schemaJson = Json.fromString(content);
                JsonValidator validator = new JsonValidator(schemaJson);
                return validator;
            }
        } catch (IOException e) {
            e.printStackTrace();
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