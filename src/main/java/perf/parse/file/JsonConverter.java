package perf.parse.file;

import perf.yaup.json.Json;

import java.util.function.Function;

/**
 * Reads the content of the file as json
 */
public class JsonConverter implements Function<String, Json> {
    @Override
    public Json apply(String s) {
        return Json.fromFile(s);
    }
}
