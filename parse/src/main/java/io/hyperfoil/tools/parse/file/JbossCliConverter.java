package io.hyperfoil.tools.parse.file;

import io.hyperfoil.tools.yaup.file.FileUtility;
import io.hyperfoil.tools.yaup.json.Json;

import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Reads the content of the file as a collection of Jboss-Cli output and returns a Json object from it
 */
public class JbossCliConverter implements BiFunction<String,Json,Json> {
    @Override
    public Json apply(String s,Json state) {
        return Json.fromJbossCli(FileUtility.readFile(s));
    }
}
