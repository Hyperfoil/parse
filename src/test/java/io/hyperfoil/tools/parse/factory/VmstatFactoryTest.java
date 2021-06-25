package io.hyperfoil.tools.parse.factory;

import io.hyperfoil.tools.parse.ExpRule;
import io.hyperfoil.tools.parse.Parser;
import io.hyperfoil.tools.yaup.json.Json;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import static junit.framework.TestCase.*;

public class VmstatFactoryTest {

    private static VmstatFactory f;

    @BeforeClass
    public static void staticInit(){ f = new VmstatFactory();}

    @Test
    public void headerGroup(){
        Json result = f.headerGroup().addRule(ExpRule.Repeat).apply("procs -----------memory---------- ---swap-- -----io---- -system-- ------cpu-----");
        assertNotNull("result should not be null",result);
        assertTrue("result should have header key\n"+result.toString(2),result.has("header"));
        assertTrue("result.header should be json\n"+result.toString(2),result.get("header") instanceof Json);
        Json header = result.getJson("header");
        assertEquals("header should have 6 entries\n"+header.toString(2),6,header.size());
    }

    @Test
    public void no_args(){
        Parser p = f.newParser();
        final List<Json> found = new LinkedList<>();
        p.add(found::add);

        p.onLine("procs -----------memory---------- ---swap-- -----io---- -system-- ------cpu-----");
        p.onLine(" r  b   swpd   free   buff  cache   si   so    bi    bo   in   cs us sy id wa st");
        p.onLine(" 1  0      0 374678496   5264 6738556    0    0     0     0    0    0  0  0 100  0  0");
        p.close();
    }

    @Test @Ignore
    public void timestamp_wide_oneHeader(){
        Parser p = f.newParser();
        final List<Json> found = new LinkedList<>();
        p.add(found::add);

        p.onLine("procs -----------------------memory---------------------- ---swap-- -----io---- -system-- --------cpu-------- -----timestamp-----");
        p.onLine(" r  b         swpd         free         buff        cache   si   so    bi    bo   in   cs  us  sy  id  wa  st                 EST");
        p.onLine(" 0  0            0    375174272         5264      6898320    0    0     0     0    0    0   0   0 100   0   0 2021-02-23 20:40:58");

        p.close();

//        found.forEach(json->{
//            System.out.println(json.toString(2));
//        });
    }
}
