package perf.parse.file;

import perf.yaup.file.FileUtility;
import perf.yaup.json.Json;

import java.util.function.Function;

/**
 * Reads the content of the file as a collection of Jboss-Cli output and returns a Json object from it
 */
public class JbossCliConverter implements Function<String,Json> {
    @Override
    public Json apply(String s) {
        return Json.fromJbossCli(FileUtility.readFile(s));
    }
}
