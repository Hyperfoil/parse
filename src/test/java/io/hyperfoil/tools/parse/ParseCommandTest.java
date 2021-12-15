package io.hyperfoil.tools.parse;

import io.hyperfoil.tools.yaup.json.Json;
import io.hyperfoil.tools.yaup.json.JsonValidator;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ParseCommandTest {

    @Test
    public void getValidator_asText_key(){
        JsonValidator validator = ParseCommand.getValidator();

        Json json = Json.fromYaml(
            "name: \"sar_summary\"\n" +
            "path: \".*?/(?<hostName>[^/\\\\.]+)[^/]*/[^/]*filename[^/]*\"\n" +
            "nest: \"${{hostName}}.cpu-utilization\"\n" +
            "asText:\n" +
            "  - name: 'data'\n" +
            "    pattern: \"(?<cpu>\\\\d+) (?<user>\\\\d+\\\\.\\\\d+) (?<system>\\\\d+\\\\.\\\\d+)\"\n" +
            "    key: cpu"
        );
        Json errors = validator.validate(json);
        assertFalse("expect errors:\n"+errors.toString(2),errors.isEmpty());
    }
}
