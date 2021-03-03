package io.hyperfoil.tools.parse;

import io.hyperfoil.tools.yaup.json.Json;
import org.junit.Test;

import java.util.Arrays;

public class JsFunctionTest {


    @Test
    public void execute_multiline(){
        JsStringFunction jsFunction = new JsStringFunction("  (path)=>{\n" +
                "    const rtrn = new Json(true);\n" +
                "    FileUtility.stream(path).forEach(line => {\n" +
                "      if(line.trim() === \"\"){\n" +
                "\n" +
                "      } else if (line.match( /^\\d+$/ ) ) {\n" +
                "        rtrn.add(new Json());\n" +
                "        rtrn.getJson(rtrn.size() - 1).set(\"timestamp\", parseInt(line));\n" +
                "      } else {\n" +
                "        rtrn.getJson(rtrn.size() - 1).set(\"data\", Json.fromString(line));\n" +
                "      }\n" +
                "    });\n" +
                "    return rtrn;\n" +
                "  }"
        );

        Json response = jsFunction.apply("/home/wreicher/perfWork/mwperf/eap/2010/286/run/mwperf-server03.perf.lab.eng.rdu2.redhat.com/cli.datasource.log");
        System.out.println(response);
    }
}
