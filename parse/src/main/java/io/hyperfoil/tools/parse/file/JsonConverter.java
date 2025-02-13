package io.hyperfoil.tools.parse.file;

import io.hyperfoil.tools.yaup.json.Json;

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
