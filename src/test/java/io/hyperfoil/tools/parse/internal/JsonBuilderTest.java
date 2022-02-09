package io.hyperfoil.tools.parse.internal;

import io.hyperfoil.tools.parse.json.JsonBuilder;
import org.junit.Assert;
import org.junit.Test;
import io.hyperfoil.tools.yaup.json.Json;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class JsonBuilderTest {

    @Test
    public void clearTargets_name(){
        JsonBuilder builder = new JsonBuilder();
        builder.getTarget().set("Name","one");
        builder.pushTarget(new Json(),"two");
        builder.getTarget().set("Name","two");
        builder.setContext("mark",true);
        builder.pushTarget(new Json(),"three");
        builder.getTarget().set("Name","three");

        builder.clearTargets("two");

        Assert.assertEquals("size",1,builder.size());
        Assert.assertFalse("no mark in context",builder.hasContext("mark",true));
    }

    @Test
    public void popTarget_name(){
        JsonBuilder builder = new JsonBuilder();
        builder.getTarget().set("Name","one");
        builder.pushTarget(new Json(),"two");
        builder.getTarget().set("Name","two");
        builder.setContext("mark",true);
        builder.pushTarget(new Json(),"three");
        builder.getTarget().set("Name","three");

        Json pop = builder.popTarget("two");

        Assert.assertEquals("size",2,builder.size());
        Assert.assertEquals("pop.Name==two","two",pop.getString("Name"));
        Assert.assertFalse("no mark in context",builder.hasContext("mark",true));
    }

    @Test
    public void namedTargetIndex_topMatch(){
        JsonBuilder builder = new JsonBuilder();
        builder.getTarget().set("Name","one");
        builder.pushTarget(new Json(),"foo");
        builder.getTarget().set("Name","two");
        builder.pushTarget(new Json(),"foo");
        builder.getTarget().set("Name","three");

        int index = builder.namedTargetIndex("foo");
        Assert.assertEquals("top index",2,index);
    }

    @Test
    public void hasContext_recursive(){
        JsonBuilder builder = new JsonBuilder();
        builder.pushTarget(new Json(false));
        builder.pushTarget(new Json(false));
        builder.setContext("child",4);
        builder.pushTarget(new Json(false));
        builder.pushTarget(new Json());

        boolean hasChild = builder.hasContext("child",true);
        Assert.assertTrue("hasChild",hasChild);
    }

    @Test
    public void peekTarget_pused_1(){
        JsonBuilder builder = new JsonBuilder();
        builder.getTarget().set("Name","one");
        builder.pushTarget(new Json());
        builder.getTarget().set("Name","two");

        Json peek = builder.peekTarget(1);

        Assert.assertFalse("peek should not be null",peek==null);
        Assert.assertEquals("Name:"+peek,"one",peek.getString("Name"));
    }

    @Test
    public void peekTarget_pushed_3(){
        JsonBuilder builder = new JsonBuilder();
        builder.getTarget().set("Name","one");
        builder.pushTarget(new Json());
        builder.getTarget().set("Name","two");
        builder.pushTarget(new Json());
        builder.getTarget().set("Name","three");
        builder.pushTarget(new Json());
        builder.getTarget().set("Name","four");
        Json peek = builder.peekTarget(1);

        Assert.assertFalse("peek should not be null",peek==null);
        Assert.assertEquals("Name:"+peek,"three",peek.getString("Name"));
    }

    @Test
    public void getContextInteger_recursive(){
        JsonBuilder builder = new JsonBuilder();
        builder.setContext("Name",0);
        builder.pushTarget(new Json());
        builder.setContext("Name",1);
        builder.pushTarget(new Json());
        builder.setContext("Name",2);
        builder.pushTarget(new Json());

        int value = builder.getContextInteger("Name",true);
        Assert.assertEquals("Name:\n"+builder.debug(true),2,value);

    }
}
