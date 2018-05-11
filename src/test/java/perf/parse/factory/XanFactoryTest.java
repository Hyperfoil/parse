package perf.parse.factory;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import perf.parse.Exp;
import perf.parse.internal.CheatChars;
import perf.parse.internal.JsonBuilder;
import perf.yaup.json.Json;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class XanFactoryTest {

    private static XanFactory f;

    @BeforeClass
    public static void staticInit(){
        f = new XanFactory();
    }

    @Before
    public void reset(){}

    @Test
    public void header(){
        Exp p = f.headerExp();
        Json root;
        root = p.apply("Name     Value");

        assertEquals("expect 2 headers",2,root.getJson("header").size());


        root = p.apply("Time (s)  CreateVehicleEJB  CreateVehicleWS");
        assertEquals("expect 3 headers",3,root.getJson("header").size());

    }

    @Test
    public void title(){
        JsonBuilder b = new JsonBuilder();
        Exp p = f.title();
        p.apply(new CheatChars("Title: SPECjEnterprise2010 Detailed Results"),b,null);
        assertTrue("root should include title",b.getRoot().has("title"));
        assertEquals("title","SPECjEnterprise2010 Detailed Results",b.getRoot().getString("title"));
    }

    @Test
    public void section(){
        JsonBuilder b = new JsonBuilder();
        Exp p = f.section();
        p.apply(new CheatChars("Section: Benchmark Information"),b,null);
        assertTrue("root should include section",b.getRoot().has("section"));
        assertEquals("section","Benchmark Information",b.getRoot().getString("section"));
    }


}
