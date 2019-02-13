# Parse
Use regex patterns to build json from text files


## Warnings
* I use this to parse all the logs from a benchmark run but that doesn't mean the API is stable.
* `Merge`, `Value`, and `Rule` enums have overlapping concerns and should probably be refactored.
* `Value.NestLength` and `Value.NestPeerless` are not well tested
* The result json from any factory is subject to change :)


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
//Merge.PreClose creates a new result json and will sends the previous one to the JsonConsumers
Files.lines(Paths.get("server.gclog")).forEach(parser::onLine) //pass each line to the parser
```

## Exp
Exp are java regex that update the input line and the result json when the regex is found. The regex pattern needs to use named capture groups to identify values for the json e.g. `(?<size>\\d+)` and do not need to match the entire line but can use `^` and `$` to force the match location.

After the Exp pattern is found in the line the Exp will "eat" (remove) part of the line. The default is to "eat" the part of the line that matched the pattern but Exp can also eat nothing, everything, everything up to the match, or a fixed width from the match. See `perf.parse.Eat`

The next step is to update the result json with the captured name-value pairs. The default is to collect each name-value pair. This means the first match with set `{name : value}` and a second match will change it to `{name : [value,value2]}`. See `perf.parse.Merge`

There are several other configuration options for shaping the result json. The API is not static and can certainly be improved. If you are trying to create a certain json shape then take a look at `perf.parse.Rule` which list all the existing actions that can occur after the match and before the result json is updated.

## Values
One of the first json shaping issues is wanting to change the value type for the name-value pair. The default is to treat the value as a Number if `\\d+` or `\\d+\\.\\d+` match the value, otherwise it is a `String`. Check `perf.parse.Value` for the currently supported value types. There are 2 ways to set the Value type for a named capture:

```Java
  Exp("javaApi","(?<size>\\d+[kmgtKMGT][bB]?").set("name",Value.KMG); //Java API
  Exp("patternApi","(?<size:KMG>\\d+[kmgtKMGT][bB]?"); //pattern API
```
The pattern API should work for all the Value types except `Value.Key`. Key is when you want to have a pattern `(?<alpha>\\S+)=(?<bravo>.*)` and want the resulting json to be `{alphaValue : bravoValue}` instead of '{alpha: alphaValue, bravo: bravoValue}`. `Value.Key` requires the Java API
```Java
  Exp("key","(?<alpha\\S+)=(?<bravo>.*)").set("alpha","bravo");
```