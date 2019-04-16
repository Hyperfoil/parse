package perf.parse.factory;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import perf.parse.Eat;
import perf.parse.MatchRule;
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
    public void blank(){
        JsonBuilder jsonBuilder = new JsonBuilder();

        boolean matched = f.blank().apply(new CheatChars(""),jsonBuilder,null);
        assertTrue("blank should match an empty string",matched);
    }

    @Test
    public void header(){
        Json root = f.header()
                .setRule(MatchRule.Repeat)
                .eat(Eat.Line)
                .apply("Name     Value");
        assertEquals("expect 2 headers:\n"+root.toString(2),2,root.getJson("header").size());

        root = f.header()
                .setRule(MatchRule.Repeat)
                .eat(Eat.Line)
                .apply("Time (s)  CreateVehicleEJB  CreateVehicleWS");
        assertEquals("expect 3 headers",3,root.getJson("header").size());
    }

    @Test
    public void title(){
        Json root = f.title().apply("Title: SPECjEnterprise2010 Detailed Results");
        assertEquals("title","SPECjEnterprise2010 Detailed Results",root.getString("title"));
    }

    @Test
    public void section(){
        Json root = f.section().apply("Section: Benchmark Information");
        assertEquals("section","Benchmark Information",root.getString("section"));
    }


}
