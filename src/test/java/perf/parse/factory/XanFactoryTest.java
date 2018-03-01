package perf.parse.factory;

import org.junit.Assert;
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
    public void headerExp(){
        JsonBuilder b = new JsonBuilder();
        Exp p = f.headerExp();
        p.apply(new CheatChars("Name     Value"),b,null);
        System.out.println(b.getRoot().toString(2));

        assertEquals("expect 2 headers",2,b.getRoot().getJson("header").size());

        b.reset();
        p.apply(new CheatChars("Time (s)  CreateVehicleEJB  CreateVehicleWS"),b,null);
        assertEquals("expect 3 headers",3,b.getRoot().getJson("header").size());

    }

    @Test
    public void titleExp(){
        JsonBuilder b = new JsonBuilder();
        Exp p = f.titleExp();
        p.apply(new CheatChars("Title: SPECjEnterprise2010 Detailed Results"),b,null);
        assertTrue("root should include title",b.getRoot().has("title"));
        assertEquals("title","SPECjEnterprise2010 Detailed Results",b.getRoot().getString("title"));
    }

    @Test
    public void sectionExp(){
        JsonBuilder b = new JsonBuilder();
        Exp p = f.sectionExp();
        p.apply(new CheatChars("Section: Benchmark Information"),b,null);
        assertTrue("root should include section",b.getRoot().has("section"));
        assertEquals("section","Benchmark Information",b.getRoot().getString("section"));
    }


}
