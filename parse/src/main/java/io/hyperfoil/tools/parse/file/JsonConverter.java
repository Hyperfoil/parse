package io.hyperfoil.tools.parse.file;

import io.hyperfoil.tools.yaup.json.Json;

import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Reads the content of the file as json
 */
public class JsonConverter implements BiFunction<String, Json, Json> {
    @Override
    public Json apply(String s,Json state) {
        return Json.fromFile(s);
    }
}
