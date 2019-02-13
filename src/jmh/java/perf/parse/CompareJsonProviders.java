package perf.parse;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.ReadContext;
import com.jayway.jsonpath.spi.json.JacksonJsonProvider;
import org.openjdk.jmh.annotations.*;
import perf.yaup.file.FileUtility;
import perf.yaup.json.YaupJsonProvider;

@State(Scope.Benchmark)
public class CompareJsonProviders {

    @Param({"$[-1].datestamp","$[*][?(@.commandLine)].commandLine"})
    public String path;

    public String content = FileUtility.readFile("/home/wreicher/perfWork/jdcasey/2018-05-09/all.json");
    public Configuration yaup = Configuration.defaultConfiguration().jsonProvider(new YaupJsonProvider());
    public Configuration other = Configuration.defaultConfiguration().jsonProvider(new JacksonJsonProvider());
    public Object yaupObject = yaup.jsonProvider().parse(content);
    public Object otherObject = other.jsonProvider().parse(content);

    @Setup(Level.Iteration)
    public void doSetup(){
        System.out.println("doSetup");
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
