package io.hyperfoil.tools.parse;

import io.hyperfoil.tools.yaup.json.Json;
import io.hyperfoil.tools.yaup.json.JsonValidator;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ParseCommandTest {

    @Test @Ignore
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
        assertFalse("expect errors:\n"+errors.toString(2),errors.isEmpty());
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
        assertTrue("unexpect errors:\n"+errors.toString(2),errors.isEmpty());
    }
}
