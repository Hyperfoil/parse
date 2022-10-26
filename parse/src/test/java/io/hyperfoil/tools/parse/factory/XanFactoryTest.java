package io.hyperfoil.tools.parse.factory;

import io.hyperfoil.tools.parse.Eat;
import io.hyperfoil.tools.parse.ExpRule;
import io.hyperfoil.tools.parse.internal.CheatChars;
import io.hyperfoil.tools.parse.json.JsonBuilder;
import io.hyperfoil.tools.yaup.json.Json;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

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
        Assert.assertTrue("blank should match an empty string",matched);
    }

    @Test
    public void header(){
        Json root = f.header()
                .addRule(ExpRule.Repeat)
                .eat(Eat.Line)
                .apply("Name     Value");
        Assert.assertEquals("expect 2 headers:\n"+root.toString(2),2,root.getJson("header").size());

        root = f.header()
                .addRule(ExpRule.Repeat)
                .eat(Eat.Line)
                .apply("Time (s)  CreateVehicleEJB  CreateVehicleWS");
        Assert.assertEquals("expect 3 headers",3,root.getJson("header").size());
    }

    @Test
    public void title(){
        Json root = f.title().apply("Title: SPECjEnterprise2010 Detailed Results");
        Assert.assertEquals("title","SPECjEnterprise2010 Detailed Results",root.getString("title"));
    }

    @Test
    public void section(){
        Json root = f.section().apply("Section: Benchmark Information");
        Assert.assertEquals("section","Benchmark Information",root.getString("section"));
    }


}
