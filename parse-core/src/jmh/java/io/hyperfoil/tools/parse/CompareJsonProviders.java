package io.hyperfoil.tools.parse;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.ReadContext;
import com.jayway.jsonpath.spi.json.JacksonJsonProvider;
import org.openjdk.jmh.annotations.*;
import io.hyperfoil.tools.yaup.file.FileUtility;
import io.hyperfoil.tools.yaup.json.YaupJsonProvider;

@State(Scope.Benchmark)
public class CompareJsonProviders {

    @Param({"$[-1].datestamp","$[*][?(@.commandLine)].commandLine"})
    public String path;

    public String content = FileUtility.readFile("//tmp/test.json");
    public Configuration yaup = Configuration.defaultConfiguration().jsonProvider(new YaupJsonProvider());
    public Configuration other = Configuration.defaultConfiguration().jsonProvider(new JacksonJsonProvider());
    public Object yaupObject = yaup.jsonProvider().parse(content);
    public Object otherObject = other.jsonProvider().parse(content);

    @Setup(Level.Iteration)
    public void doSetup(){
    }

    @Benchmark
    public void yaup() {
        ReadContext ctx = JsonPath.parse(yaupObject,yaup);
        JsonPath jsonPath = JsonPath.compile(path);
        Object found = ctx.read(jsonPath);
        assert found!=null;
    }

    @Benchmark
    public void other(){
        ReadContext ctx = JsonPath.parse(otherObject, other);
        JsonPath jsonPath = JsonPath.compile(path);
        Object found = ctx.read(jsonPath);
        assert found!=null;
    }

}
