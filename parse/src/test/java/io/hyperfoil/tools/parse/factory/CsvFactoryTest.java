package io.hyperfoil.tools.parse.factory;

import io.hyperfoil.tools.parse.Parser;
import io.hyperfoil.tools.yaup.json.Json;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class CsvFactoryTest {

    private static CsvFactory f;

    @BeforeClass
    public static void staticInit() { f = new CsvFactory(); }


    @Test
    public void header_with_parenthesis_and_percent_sign(){
        Parser p = f.newParser();
        final List<Json> found = new LinkedList<>();
        p.add(found::add);

        p.onLine("\"Benchmark\",\"Mode\",\"Threads\",\"Samples\",\"Score\",\"Score Error (99.9%)\",\"Unit\"");
        p.onLine("\"foo\",\"thrpt\",1,15,3959.204321,99.361981,\"ops/s\"");
        p.onLine("\"bar\",\"thrpt\",1,15,47278.187481,1523.003762,\"ops/s\"");
        p.onLine("\"baz\",\"thrpt\",1,15,128.572278,2.898348,\"ops/s\"");
        p.close();

        assertEquals("expected number of entries:"+found,1,found.size());
        Object entry = found.get(0);
        assertTrue("entry should be json",entry instanceof Json);
        Json json = (Json)entry;
        assertTrue("json.header should exist"+json,json.has("header"));
        assertTrue("json.header should be json",json.get("header") instanceof Json);
        Json header = (Json)json.get("header");
        assertEquals("expect 7 headers "+header,7,header.size());
        assertTrue("json.data should exist"+json,json.has("data"));
        assertTrue("json.data should be json",json.get("data") instanceof Json);
        Json data = (Json)json.get("data");
        assertEquals("expect 3 data entries "+data,3,data.size());
    }
}
