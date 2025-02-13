package io.hyperfoil.tools.parse.cli;

import io.hyperfoil.tools.yaup.json.Json;
import io.hyperfoil.tools.yaup.json.JsonValidator;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


public class ParseCommandTest {

    @Test @Disabled
    public void getValidator_asText_key(){
        JsonValidator validator = ParseCommand.getValidator();

        Json json = Json.fromYaml(
            "name: \"sar_summary\"\n" +
            "path: \".*?/(?<hostName>[^/\\\\.]+)[^/]*/[^/]*filename[^/]*\"\n" +
            "nest: \"${{hostName}}.cpu-utilization\"\n" +
            "asText:\n" +
            "  - name: 'data'\n" +
            "    pattern: \"(?<cpu>\\\\d+) (?<user>\\\\d+\\\\.\\\\d+) (?<system>\\\\d+\\\\.\\\\d+)\"\n" +
            "    additional_key: should_not_be_here"
        );
        Json errors = validator.validate(json);
        assertFalse(errors.isEmpty(),"expect errors:\n"+errors.toString(2));
    }
    @Test
    public void getValidator_asJson_null(){
        JsonValidator validator = ParseCommand.getValidator();
        Json json = Json.fromYaml(
                "name: \"sar_summary\"\n" +
                        "path: \".*?/(?<hostName>[^/\\\\.]+)[^/]*/[^/]*filename[^/]*\"\n" +
                        "nest: \"${{hostName}}.cpu-utilization\"\n" +
                        "asJson:"

        );
        Json errors = validator.validate(json);
        assertTrue(errors.isEmpty(),"unexpect errors:\n"+errors.toString(2));
    }
}
