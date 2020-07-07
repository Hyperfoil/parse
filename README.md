# Parse
Use regex patterns to build json from text files


## Warnings
* I use this to parse all the logs from a benchmark run but that does not mean the API is stable.
* The result json from any factory is subject to change :)
* caveat emptor

## Building
The parse project depends on yaup which as of now is un-published
```bash
git clone https://github.com/Hyperfoil/yaup.git
cd yaup
mvn clean install
```
It would be wise to re-build yaup from master so long as the dependency is SNAPSHOT then build parse with
`mvn clean package`

Parse creates two artifacts: 
- a normal jar for java development 
- an uber jar to use with `java -jar` without specifying a classpath.

## Arguments
- `-s, --source` specifies a single source folder or archive (zip, gzip, etc) to parse
- `-d, --destination` used with `--source` to specify where to save the resulting json
- `-b, --batch` a `,` separated list of sources to parse (separately).
This will use a default output file pattern based on the source name
(e.g. `./test/` or `./test.zip` will be saved as `./test.json` )
- `-t, --threads` set the maximum number of threads to use for parsing sources.
Each source will only use 1 thread (because of parser state) but in `--batch` this can limit concurrent heap demand
`default = # cores`
-`-r, --rules` a `,` separated list of rule files (`yaml` or `json`) to use instead of the default parse rules


## Rules
The library was created as a java utility but also supports `yaml` or `json` rule definitions with `javascript` taking the place of `java` code.

There are a collection of default rules in `resources/defaultRules.yaml` which are loaded when `--rules` are not specified and act as example rules.
There is also `resources/filerule-schema.json` to use with the vscode Red Hat yaml plugin to validate any rule files.

Each rule works in 3 phases
- find matching files in the `source`
- scan each file to create json
- merge the json into the result json for the `source` 
  
### matching files
```yaml
path: java regex with named capture groups e.g. /(?<name>[^/]+)/
headerLines: number of lines to scan for findHeader and avoidHeader, 
findHeader: java regex (or list of regex). Will match if at least one regex matches
avoidHeader: java regex (or list of regex). Will NOT match if any match (overrides findHeader)
```
### merge json
```yaml
nest: json.path.${{name}}
```
The parse creates a single json object from all of the rules that match a source.
Rules specify the key path to where the rule's output should be located. The above example
will create `{json : {path : {foo :...} } }` if the path matched `/foo/` from the source.

Using `${{name}}` will merge the value of from the rule with the value from any previous match.
If a previous rule created `{json : {path : {foo : {a : 100} } } }` and the current rule has `{b : 200}`
then using `${{...}}` will result in `{json : {path: {foo : {a : 100, b : 200} } } }`

### create json
This technically happens before merge json but it by far the more complicated step so safe it for last.
The `nest:` key can also depend on `path:` so it is normally shortly after `path:` in the rule defintion.

The first step in creating the json is to specify how the rule should handle the file content.
We currently have the following options:
* `asText` process the text file line by line. This the most common for logs and custom formats
* `asXml` read the file as an xml document then convert it to`json`
* `asJson` read entire document as `json`
* `asPath` Use the fully qualified file path as an argument to a `javascript` method `(path)=>{...}`
* `asContent` read the entire content of the file into a single string. use this sparingly, e.g. for reading `java -version > /tmp/java-version.log`
* `asJbossCli` read the entire document as the output of a jboss-cli command.
Jboss-cli can output as json so this will be deprecated.

#### asContent
Reads the entire file into a string and adds it to the rule `json` under the key
```yaml
asContent: key
```
### asPath
This expects a javascript function that accepts the fully qualified path the the file as the only argument.
The example could also be created with `exp` but it illustates creating 'java' objects in 'javascript' 
```yaml
asPath: |
  (path)=>{
    //do something in javascript with access to StringUtil, FileUtility, Exp, ExpMerge, Xml, Json
    const rtrn = new Json(); //const because we are in javascript
    FileUtility.stream(path).forEach(line => { //javascript uses => not -> 
      if(line.trim() === ""){
      } else if (line.match( /^\d+$/ )) {  // lines from date +%s >> log
        rtrn.add(new Json()); //start a new entry
        rtrn.getJson(rtrn.size() - 1).set("timestamp", parseInt(line)); //javascript parseInt()
      } else {
        rtrn.getJson(rtrn.size() - 1).set("data", Json.fromString(line)); //logged json output from curl to 1 line
      }
    }    
    return rtrn; //this is the output of the rule for the merge step
  }  
```
#### asXml | asJson
This reads the entire document into `xml` | `json`. If the value of the tag is an empty string 
then the entire document is merged according to `nest:` as `json`.
For xml that means attributes will be `@attributes` and the node value will be `text()`
```yaml
asXml: '' # empty string means merge the entire document
```
The other option is to `filter` the document to create a new `json` output for the rule. 
Filters have the following options
```yaml
name: just a useful name for logging / debugging
path: either jsonpath or xpath to the value or sub-document
nest: where to place the resulting json
#choose between children, regex, or exp
children: an array of filter to apply to each match from path before using result
regex: java regex applied to the string version of the current output
exp: an array of exp to use on the string version of the current output
# finally
result: either a javascript function or json using ${{key.path}} to substitue values from curent output
```
`path` can be jsonpath for both `asXml` and `asJson` but xpath will only work with `asXml`
jsonpath **must** start with `$.` and xpath **must** start with `/`

`nest` specifies the key path to where the result should be merged. 
This `nest` does not support `${{name}}` substitution 

`children` subsequent `filter` that will run on each match from `path` 
This is useful to extract a values from one level in the document then use `children`
to extract values further down the document structure.

`exp` see `asText` for how to create `exp` 
 
#### asText
Most rules scan each line of a file looking for patterns to create structured `json`.
`asText` can be used as either the name of an existing file parser factory or a list of parse expressions (`exp`)

The existing file parser factories:
* `csvFactory` each row will become `{header1: value, header2: ...}`
* `dstatFactory` identify the groups and stats then each row is `{group.stat: value...}` 
* `jep271Factory` gc logs from jdk11+
* `jmapHistoFactory` parses `jmap -histo <pid>` output
* `jstackFactory` parsers `jstack <pid>` output
* `printGcFactory` gc logs from jdk < 11
* `serverLogFactory` parsers the default `server.log` format
* `substrateGcFactory` parses the gc logs from substrateVm (quarkus)
* `wrkFactory` parses wrk output files (e.g. `wrk http://lcoalhost:8080/api`)
* `xanFactory` parses faban xan files (e.g. `xan.details`)

If you need to parse a different format line by line then you need to create a list of `exp`

## Exp
Exp are the key to how parse creates structured json from text files. 
Each `exp` specifies a java regex `pattern` to match against part of the input line
then what to do when the pattern matches. We start with the initial settings:
```yaml
name: exp name used for debug / logging #optional but helpful
pattern: java regex pattern with optional capture groups
```
`exp` need to be told where to try and match the input line.
The top level `exp` will start with the first character of the line then by default their
childen start from where the parent match finished but they can also match before the parent or the entire line
```yaml
range: EntireLine | BeforeParent | AfterParent # default is AfterParent
```
`exp` have a set of `rules` that define common actions to take when an `exp` matches the line.
The first group of rules are those that are invoked before the `exp` tries to add anything to the result json.
These rules follow the `PreX` naming convention.
* `PreClose` close (finish) the current json object and start a new json object.
* `PrePopTarget` remove the current target or remove targets until they match the name for the rule
```yaml
rules:
- PrePopTarget #remove the current target (unless it is the root of the json)
- PrePopTarget: namedTarget # remove targets until target or until the root of the json
```
* PreClearTarget: reset the target json to the root of the current json
* TargetRoot: temporarily target the root for this `exp` (and children) without changing the targets

The next phase is to determine where to merge the values from the pattern matching.
The default location is the current target json but that can be changed with `nest`
```yaml
nest: json.path.${{name}}.$[[otherName]]
```
`nest` for an `exp` is similar to nest from the `rule` and `filter` with the addition of `$[[...]]` syntax
The usual `${{name}}` syntax will merge keys if two objects have the same `name` 
but `$[[name]]` will treat each object as a separete entry in an array.
For example: if `{a: 100}` and `{b: 200}` both used `nest: ${{name}}`
then the result would be
```json
{"name": {"a": 100, "b": 200}}
```
but if they both used `nest: $[[name]]` then the result would be
```json
{"name": [{"a": 100}, {"b": 200}]}
```
At this point any named capture groups from the pattern are merged into the current json target
along with any `with` name and value pairs. The name from the capture group is the key and the value is automatically converted based on the string value
* integer numbers `-?\\d{1,16}` are converted to `Long`
* decimal numbers are converted to `Double`
* memory size patterns `(\\d+\\.?\\d*[bBkKmMgGtT])` are converted into `Long` number of bytes
* json like patterns `{...}` or `[...]` are converted into `json` 
You can override the default by setting a type in the `pattern`
or by setting the field type under `fields` 
```yaml
name: exampleExp
pattern: /(?<name:type>[^/]+)/(?<otherName>[^/]+)/
with:
  anotherName: 10 #add this to the json when merging exampleExp 
fields:
    otherName:
      type: Auto | String | KMG | Integer | Decimal | Json
      merge: Auto # default is Auto but read on for merge options
``` 
`exp` will often try to add the same field name to a json object that already has a value.
In this case the `merge` setting controls how multiple matches are handled.
* `Auto` the first match is set to `{key: value}` but multiple matches yield `{key: [value, value2]}`
* `BooleanKey` set `{[name]: true}` where name is the name of the capture group from `pattern`
* `BooleanValue` set `{[value] : true}` where value is the matched value as a string
* `TargetId` starts a new json object if `currentTarget[name] !== value`. Useful with eventId in multi-line logging
* `Count` the number of times the capture group matched
* `Add` convert the `value` to a number and add it to `currentTarget[name]`
* `List` create a list of values even if there is only one value. e.g. `{[name] : [value]}`
* `Set` like list but only adds unique values
* `Key` use `value` as the `name` for the referenced capture group.
* `First` only save the first `value`
* `Last` only save the last `value`
* `TreeSibling` use the string length of `value` to create a tree where children are under `name`
successive matches of the same length are treated as sibling branches
* `TreeMerging` same as `TreeSibling` except successive matches are merged into one branch   

Once the `values` are merged into the json the `exp` can enable and disable tags.
Tags are just string values used to turn `exp` on or off so they only try and match lines
when appropriate. For example: verboseGC parsers turn off the g1gc parser support if the log is `Using Shenandoah`
```yaml
requires: ["g1gc"]
enables: ["g1gc-verbose"]
disables: ["parallelGc","shenandoah"]
```
We didn't mention `requires` earlier but `exp` will first check that the all the `requires` tags
are enabled before attempting to match the line. This lets us disable `exp` that would otherwise
match lines from the wrong part of the source file.

The next step if for the `exp` to modify the line. `exp` remove the matched part of the line by default so both children
`exp` and any subsequent sibling `exp` do not have to construct regex to avoid that part of the line.
The line modification is configured through `eat` and has the following options
```yaml
name: eatExample
eat: 10 #will eat 10 characters from the start of the match
eat: None | Match | ToMatch | Line
```
* `None` - do not modify the line
* `Match` - the `default` behaviour of removing the section the `exp` matched
* `ToMatch` - removes everything up to the **end** of the match
* `Line` - remove the entire line. This prevents other `exp` from parsing the line but will take effect after children `exp` try and parse the line
  
The next step is to allow all of the children `exp` to parse the remaining line but first
all pre-children rules are invoked. At the moment those are:
* `PushTarget` set the current json object (from `nest`) as the target json object for subsequent `exp`
```yaml
rules:
 - PushTarget # pushes the target without a name
 - PushTarget: name # uses a name for later PopTarget calls
```  

Now the children `exp` parse the remaining line the order they are defined. 
All of the children will run and can be re-run if the `exp` has the `RepeatChildren` rule.
`RepeatChildren` will repeat all of the children if any one of the children matched the line
Be sure to modify the line when using `RepeatChildren` or parsing will never end
 
At this point the `exp` can re-run along with everything up to and including the children `exp`
if the `exp` has the `Repeat` rule.
```yaml
rules: [ Repeat ]
``` 
`Repeat` includes checking `requires` matching the remaining line... 
everything up to and including the children matching the line as well 

Finally any post-children rules are applied. Those rules follow the `PostX` naming convention
* `PostClose` consider the current json finished and start a new json for the next `exp`
* `PostPopTarget` change back to the previous target for the next `exp`. This can accept a `name` just like `PrePopTarget`
* `PostClearTarget` clear all targets back to the root json  


The process is repeated for each `exp` until all exp do not match or the input line is empty
