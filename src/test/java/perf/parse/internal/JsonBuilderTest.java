package perf.parse.internal;

import org.junit.Test;
import perf.yaup.json.Json;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class JsonBuilderTest {

    @Test
    public void clearTargets_name(){
        JsonBuilder builder = new JsonBuilder();
        builder.getTarget().set("name","one");
        builder.pushTarget(new Json(),"two");
        builder.getTarget().set("name","two");
        builder.setContext("mark",true);
        builder.pushTarget(new Json(),"three");
        builder.getTarget().set("name","three");

        builder.clearTargets("two");

        assertEquals("size",1,builder.size());
        assertFalse("no mark in context",builder.hasContext("mark",true));
    }

    @Test
    public void popTarget_name(){
        JsonBuilder builder = new JsonBuilder();
        builder.getTarget().set("name","one");
        builder.pushTarget(new Json(),"two");
        builder.getTarget().set("name","two");
        builder.setContext("mark",true);
        builder.pushTarget(new Json(),"three");
        builder.getTarget().set("name","three");

        Json pop = builder.popTarget("two");

        assertEquals("size",2,builder.size());
        assertEquals("pop.name==two","two",pop.getString("name"));
        assertFalse("no mark in context",builder.hasContext("mark",true));
    }

    @Test
    public void namedTargetIndex_topMatch(){
        JsonBuilder builder = new JsonBuilder();
        builder.getTarget().set("name","one");
        builder.pushTarget(new Json(),"foo");
        builder.getTarget().set("name","two");
        builder.pushTarget(new Json(),"foo");
        builder.getTarget().set("name","three");

        int index = builder.namedTargetIndex("foo");
        assertEquals("top index",2,index);
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
        assertTrue("hasChild",hasChild);
    }

    @Test
    public void peekTarget_1(){
        JsonBuilder builder = new JsonBuilder();
        builder.getTarget().set("name","one");
        builder.pushTarget(new Json());
        builder.getTarget().set("name","two");
        builder.pushTarget(new Json());
        builder.getTarget().set("name","three");
        builder.pushTarget(new Json());
        builder.getTarget().set("name","four");
        Json peek = builder.peekTarget(1);

        assertFalse("peek should not be null",peek==null);
        assertEquals("name:"+peek,"three",peek.getString("name"));
    }

    @Test
    public void getContextInteger_recursive(){
        JsonBuilder builder = new JsonBuilder();
        builder.setContext("name",0);
        builder.pushTarget(new Json());
        builder.setContext("name",1);
        builder.pushTarget(new Json());
        builder.setContext("name",2);
        builder.pushTarget(new Json());

        int value = builder.getContextInteger("name",true);
        assertEquals("name:\n"+builder.debug(true),2,value);

    }
}
