# Parse
Use regex patterns to build json from text files


## Warnings
* I use this to parse all the logs from a benchmark run but that does not mean the API is stable.
* The result json from any factory is subject to change :)
* `Merge`, `Value`, and `Rule` enums have overlapping concerns and should probably be refactored.
* `Value.NestLength` and `Value.NestPeerless` are not well tested
* caveat emptor


## Examples
There are existing Factories in perf.parse.factory for common log formats. For example, parsing jdk-9+ gclogs
```Java
Jep271Factory factory = new Jep271Factory();
Parser parser = factory.newParser();
final Json gc = new Json();//will end up as an array of gc events
parser.add(gc::add); //save all the gc events
parser.add((json)->System.out.println(json.toString(2)); //JsonConsumer logs each json
Files.lines(Paths.get("server.gclog")).forEach(parser::onLine) //pass each line to the parser
Files.write(Paths.get("gc.json"), gc.toString(2).getBytes(), StandardOpenOption.CREATE);
```

If you don't find a parser and just need to parse a unique file you can add Exp directly to the parser. Each named capture group will be added to the json

```Java
Parser parser = new Parser();
parser.add((json)->System.out.println(json.toString(2)); //JsonConsumer logs each json
parser.add(new Exp("tsValue","(?<timestamp>\\d+),(?<value>\\d+)").set(Merge.PreClose));
//Merge.PreClose creates a new result json and sends the previous one to the JsonConsumers
Files.lines(Paths.get("server.gclog")).forEach(parser::onLine) //pass each line to the parser
```

## Exp
Exp are java regex that update the input line and the result json when the regex is found. The regex pattern needs to use named capture groups to identify values for the json e.g. `(?<size>\\d+)` and do not need to match the entire line but can use `^` and `$` to force the match location.

After the Exp pattern is found in the line the Exp will "eat" (remove) part of the line. The default is to "eat" the part of the line that matched the pattern but Exp can also eat nothing, everything, everything up to the match, or a fixed width from the match. See `perf.parse.Eat`

The next step is to update the result json with the captured name-value pairs. The default is to collect each name-value pair. This means the first match with set `{name : value}` and a second match will change it to `{name : [value,value2]}`. See `perf.parse.Merge`

There are several other configuration options for shaping the result json. The API is not static and can certainly be improved. If you are trying to create a certain json shape then take a look at `perf.parse.ExpRule` which list all the existing actions that can occur after the match and before the result json is updated.

## Values
One of the first json shaping issues is wanting to change the value type for the name-value pair. The default is to treat the value as a Number if `\\d+` or `\\d+\\.\\d+` match the value, otherwise it is a `String`. Check `perf.parse.ValueType` for the currently supported value types. There are 2 ways to set the Value type for a named capture:

```Java
  Exp("javaApi","(?<size>\\d+[kmgtKMGT][bB]?").set("name",Value.KMG); //Java API
  Exp("patternApi","(?<size:KMG>\\d+[kmgtKMGT][bB]?"); //pattern API
```
The pattern API should work for all the Value types except `Value.Key`. Key is when you want to have a pattern `(?<alpha>\\S+)=(?<bravo>.*)` and want the resulting json to be `{alphaValue : bravoValue}` instead of `{alpha: alphaValue, bravo: bravoValue}`. `Value.Key` requires the Java API
```Java
  Exp("name","(?<alpha\\S+)=(?<bravo>.*)").set("alpha","bravo");
```


## How I actually use this
The above examples are great for testing new Exp and verifying result json structure. In practice, I want to scan a collection of files with an expected folder structure (e.g. the archive.zip from a jenkins run) and build a result json from all the content. This where we use `perf.parse.file.FileRule` and the helper `perf.parse.file.RuleBuilder` .
```Java
//Build a list of all the file patterns and what to do when the pattern matches
List<FileRule> = Arrays.asList(
new RuleBuilder("printGc")//the old gc log format from JDK < 9
    .path(".*?/(?<serverName>[^\\./]+)/.*?\\.gclog?")
    .header(1)
    .findHeader("JRE (1.")
    .nest("${{serverName}}.gclog")
    .text(new PrintGcFactory()::newParser),
new RuleBuilder("jep271")//the new gc log format for JDK > 9
    .path(".*?/(?<serverName>[^\\./]+)/.*?\\.gclog?")
    .header(1)
    .avoidHeader("JRE (1.")
    .nest("${{serverName}}.gclog")
    .text(new Jep271Factory()::newParser),
new RuleBuilder("dstat")//any dstat.logs from the run
    .path(".*?archive/run/(?<serverName>[^\\./]+).+?/dstat\\.log?")
    .nest("${{serverName}}.dstat")
    .text(new DstatFactory()::newParser),
new RuleBuilder("run.json")//qdup specific log file that includes timing information
    .path("run.json")
    .nest("qdup")
    .json((json)->{//this (json)->json runs after convering the entire file into json
        Json rtrn = new Json();
        if(json.has("latches")){//latches are the wait-for and signal
            rtrn.set("latches",json.get("latches"));
        }
        if(json.has("state")){//state tree includes most of the run configuration
            rtrn.set("state",json.get("state"));
        }
        return rtrn;
    }),
new RuleBuilder("run.xml") //faban run rule
    .path("run.xml")//this will match any filename with run.xml in it (e.g. test.run.xml)
    .nest("faban.run")
    .xml(),//turn the entire run.xml into json
new RuleBuilder("finalFlags")//contains the output from -XX:+PrintFlagsFinal
    .path(".*?/container.log$")
    .nest("java.flags")
    .text(//custom Exp for what I want to capture from the logs
        new Exp("globalflags","\\[Global flags\\]")//start of the global flag output
            .enables("globalFlags"),
        new Exp("jvmFlag","\\s*(?<type>\\S+) (?<name>\\S+)\\s{2,}= (?<value>\\S*)\\s+\\{(?<scope>[^\\}]+)\\} \\{(?<source>[^\\}]+)\\}")
            .requires("globalFlags")
            .set(Merge.PreClose),
            //PreClose tells the parser to send the current json to consumers
        new Exp("notJvmFlag","^\033\\[")//looks for ther terminal control prefix
            .requires("globalFlags")
            .disables("globalFlags")//turns off globalFlags matching
    ),
new RuleBuilder("summary.xml")
    .path(".*?/summary.xml$")
    .nest("faban.xml")
    .xml(),
new RuleBuilder("jboss-cli")
    .path(".*?archive/run/(?<serverName>[^\\./]+).+?/cli\\.(?<name>.*?)\\.log?")
    .nest("${{serverName}}.pmi.${{name}}")
    .path((path)->{//custom file parsing
        Json json = new Json(true);
        new BufferedReader(new InputStreamReader(FileUtility.getInputStream(path))).lines().forEach(line->{
            try {
                if(line.trim().isEmpty()){//catches accidential empty lines

                }else if (line.matches("\\d+")) {//unix timestamps echo'd to file
                    json.add(new Json());
                    json.getJson(json.size() - 1).set("timestamp", Long.parseLong(line));
                } else {//the only other content is the json from jboss-cli
                    Json data = Json.fromString(line);
                    json.getJson(json.size() - 1).set("data", data);
                }
            }catch(Exception e){
                e.printStackTrace();
            }
        });
        return json;
    })
);
//That should be enough example FileRules, now we find the files to scan
//normally we use a ThreadPool for the following but single threaded is easier to read

String scanFolder = "/tmp/";
String destFolder = "/tmp/.data/";

long start = System.currentTimeMillis();
FileUtility.getFiles(scanFolder,".zip",true)//I save the archive.zip's from jenkins
    .stream()
    .filter(path->{
        String fileName = Paths.get(path).getFileName().toString().replaceFirst("\\.[^\\.]+$","");
        //don't rescan a zip if we already have the result json
        return !Paths.get(destFolder,fileName+".data.gz").toFile().exists();
    })
    .forEach(path->{
        String fileName = Paths.get(path).getFileName().toString().replaceFirst("\\.[^\\.]+$","");
        String destPath = Paths.get(destFolder,fileName+".data.gz").toAbsolutePath().toString();
        //get a list of FileUtility paths for scanning
        //FileUtility can recognize a path with ARCHIVE_KEY as an entry in an archive
        //and will get the text content from that entry
        List<String> entries = FileUtility.getArchiveEntries(path).stream().map(entry->path+FileUtility.ARCHIVE_KEY+entry).collect(Collectors.toList());

        //this is the part where we would normally use multiple threads
        Json result = new Json();//one result per zip
        for(String entry : entries){
            for(FileRule rule : fileRules){
                rule.apply(entry,(nest,json)->{
                    System.out.println(nest+" :: "+json.size());
                    result.set(nest,json);
                });
            }
        }
        try (Writer writer = new OutputStreamWriter(new GZIPOutputStream(new FileOutputStream(destPath)), "UTF-8")) {
            writer.write(result.toString(2));
        } catch (IOException e) {
            e.printStackTrace();
        }
    });
long stop = System.currentTimeMillis();
System.out.println("Elapsed "+ StringUtil.durationToString(stop-start));

```