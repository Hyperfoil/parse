package io.hyperfoil.tools.parse;

import io.hyperfoil.tools.yaup.json.Json;
import io.hyperfoil.tools.yaup.json.JsonValidator;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ParseCommandTest {

    @Test @Ignore
    public void getValidator_asText_key(){
        JsonValidator validator = io.hyperfoil.tools.parse.ParseCommand.getValidator();

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
        Assert.assertFalse("expect errors:\n"+errors.toString(2),errors.isEmpty());
    }
}
