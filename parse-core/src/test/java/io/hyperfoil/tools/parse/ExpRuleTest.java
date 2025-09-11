package io.hyperfoil.tools.parse;

import io.hyperfoil.tools.yaup.json.Json;
import org.junit.Test;

import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.*;

public class ExpRuleTest {


    @Test
    public void preClose(){
        Exp exp = new Exp("(?<key>[^=]+)=(?<value>\\S+)")
            .addRule(ExpRule.PreClose);
        Parser parser = new Parser();
        parser.add(exp);
        
        final List<Json> found = new LinkedList<>();
        parser.add(found::add);
        parser.onLine("one=uno");
        assertEquals("pre close should not close an empty root", 0,found.size());
        Json root = parser.getBuilder().getRoot();
        assertTrue("root should have key "+root,root.has("key"));
        parser.onLine("two=dos");
        assertEquals("pre close should close a non-empty root", 1,found.size());
        root = parser.getBuilder().getRoot();
        assertTrue("root should have key "+root,root.has("key"));
    }
    @Test
    public void postClose(){
        Exp exp = new Exp("(?<key>[^=]+)=(?<value>\\S+)")
            .addRule(ExpRule.PostClose);
        Parser parser = new Parser();
        parser.add(exp);
        
        final List<Json> found = new LinkedList<>();
        parser.add(found::add);
        parser.onLine("one=uno");
        assertEquals("post close should close after match", 1,found.size());
        Json root = parser.getBuilder().getRoot();
        assertTrue("root should be empty "+root,root.isEmpty());
        Json j = found.get(0);
        assertTrue("found should have key "+j,j.has("key"));
        parser.onLine("two=dos");
        assertEquals("post close should close after a match", 2,found.size());
        root = parser.getBuilder().getRoot();
        assertTrue("root should be empty "+root,root.isEmpty());
    }

    @Test
    public void prePopTarget(){
        Exp exp = new Exp("(?<key>[^=]+)=(?<value>\\S+)")
            .addRule(ExpRule.PrePopTarget);
        Parser parser = new Parser();
        parser.add(exp);
        
        final List<Json> found = new LinkedList<>();
        Json root = parser.getBuilder().getRoot();
        Json newTarget = new Json();
        newTarget.set("foo", "bar");
        root.set("sneak", newTarget);
        parser.getBuilder().pushTarget(newTarget);
        
        parser.add(found::add);
        parser.onLine("one=uno");

        root = parser.getBuilder().getRoot();
        Json target = parser.getBuilder().getTarget();

        assertFalse("target should not have foo "+target,target.has("foo"));
        assertTrue("root should have key "+root,root.has("key"));
        assertTrue("root should have value "+root,root.has("value"));
        assertTrue("root should have sneaky "+root,root.has("sneak"));
    }

    @Test
    public void postPopTarget(){
        Exp exp = new Exp("(?<key>[^=]+)=(?<value>\\S+)")
            .addRule(ExpRule.PostPopTarget);
        Parser parser = new Parser();
        parser.add(exp);
        
        final List<Json> found = new LinkedList<>();
        Json root = parser.getBuilder().getRoot();
        Json newTarget = new Json();
        newTarget.set("foo", "bar");
        root.set("sneak", newTarget);
        parser.getBuilder().pushTarget(newTarget);
        
        parser.add(found::add);
        parser.onLine("one=uno");

        root = parser.getBuilder().getRoot();
        Json target = parser.getBuilder().getTarget();

        assertFalse("target should not have foo "+target,target.has("foo"));
        assertFalse("target should not have key "+target,target.has("key"));
        assertFalse("target should not have value "+target,target.has("value"));
        assertFalse("root should not key "+root,root.has("key"));
        assertFalse("root should not value "+root,root.has("value"));
        assertTrue("root should have sneaky "+root,root.has("sneak"));
        Json sneak = root.getJson("sneak");
        assertTrue("sneak should have key "+sneak,sneak.has("key"));
        assertTrue("sneak should have value "+sneak,sneak.has("value"));
    }


    @Test
    public void pushTarget_with_nest(){
        Exp push = new Exp("(?<key>[^=]+)=(?<value>\\S+)")
            .nest("foo")
            .addRule(ExpRule.PushTarget);

        Parser parser = new Parser();
        parser.add(push);

        final List<Json> found = new LinkedList<>();
        parser.add(found::add);
        parser.onLine("one=uno");
        Json target = parser.getBuilder().getTarget();
        Json root = parser.getBuilder().getRoot();
        
        assertNotEquals("root and target should not be equal\nroot="+root+"\ntarget="+target, root,target);
        assertTrue("target should have key "+target,target.has("key"));
        assertTrue("target should have value "+target,target.has("value"));
        assertTrue("root should have foo "+root,root.has("foo"));
    }
    @Test
    public void repeat(){
        Exp exp = new Exp(",?(?<v>[^,$]+)")
            .addRule(ExpRule.Repeat);
        Json found = exp.apply("a,b,c");
        assertNotNull("should match input",found);
        assertTrue("json should be a map",!found.isArray());
        assertTrue("json should have a child v",found.has("v"));
        assertTrue("json.v should be an array "+found.get("v"),found.get("v") instanceof Json);
        Json v = found.getJson("v");
        assertEquals("json.v should have 3 entries",3,v.size());
        assertEquals("json.v[0] should be a","a",v.get(0));
        assertEquals("json.v[1] should be b","b",v.get(1));
        assertEquals("json.v[2] should be c","c",v.get(2));
    }

    @Test
    public void repeatChildren(){
        Exp exp = new Exp(",?(?<p>[^,$]+)")
            .addRule(ExpRule.RepeatChildren)
            .add(new Exp(",?(?<c>[^,$]+)"));
        Json found = exp.apply("a,b,c");
        assertTrue("found should have p "+found,found.has("p"));
        assertTrue("found should have c "+found,found.has("c"));
        assertTrue("found.p should be a string",found.get("p") instanceof String);
        assertTrue("found.c should be json",found.get("c") instanceof Json);
        Json c = found.getJson("c");
        assertEquals("c should have 2 entries",2,c.size());
    }
    @Test
    public void repeat_and_repeatChildren(){
        Exp exp = new Exp(",?(?<p>[^,$]+)")
            .addRule(ExpRule.Repeat)
            .addRule(ExpRule.RepeatChildren)
            .add(new Exp(",?(?<c>[^,$]+)"));
        Json found = exp.apply("a,b,c");
        assertTrue("found should have p "+found,found.has("p"));
        assertTrue("found should have c "+found,found.has("c"));
        //children should repeat before self
        assertTrue("found.p should be a string",found.get("p") instanceof String);
        assertTrue("found.c should be json",found.get("c") instanceof Json);
        Json c = found.getJson("c");
        assertEquals("c should have 2 entries",2,c.size());
    }    
    @Test
    public void repeat_with_child(){
        Exp exp = new Exp(",?(?<p>[^,$]+)")
            .addRule(ExpRule.Repeat)
            .add(new Exp(",?(?<c>[^,$]+)"));
        Json found = exp.apply("a,b,c,d");
        assertTrue("found should have p "+found,found.has("p"));
        assertTrue("found should have c "+found,found.has("c"));
        //children should repeat before self
        assertTrue("found.p should be json",found.get("p") instanceof Json);
        Json p = found.getJson("p");
        assertEquals("p should have 2 entries",2,p.size());
        assertTrue("found.c should be json",found.get("c") instanceof Json);
        Json c = found.getJson("c");
        assertEquals("c should have 2 entries",2,c.size());
    }    

    /**
     * PrePopTarget + Repeat should be the same as Repeat when there isn't a PushTarget or nest
     */
    @Test
    public void repeat_prePopTarget_empty_target(){
        Exp exp = new Exp(",?(?<v>[^,$]+)").addRule(ExpRule.Repeat).addRule(ExpRule.PrePopTarget);

        Json found = exp.apply("a,b,c");
        assertNotNull("should match input",found);
        assertTrue("json should be a map",!found.isArray());
        assertTrue("json should have a child v",found.has("v"));
        assertTrue("json.v should be an array "+found.get("v"),found.get("v") instanceof Json);
        Json v = found.getJson("v");
        assertEquals("json.v should have 3 entries",3,v.size());
        assertEquals("json.v[0] should be a","a",v.get(0));
        assertEquals("json.v[1] should be b","b",v.get(1));
        assertEquals("json.v[2] should be c","c",v.get(2));

    }

    @Test
    public void asEntry_prePopTarget_pushTarget(){
        Exp push = new Exp("(?<key>[^=]+)=(?<value>\\S+)")
            .setMerge(ExpMerge.AsEntry)
//            .addRule(ExpRule.PrePopTarget)
//            .addRule(ExpRule.PushTarget)
            ;
        Exp metoo = new Exp("(?<name>[^_]+)_(?<rank>\\S+)")
                .nest("option")
                .setMerge(ExpMerge.AsEntry)
            ;

        Parser parser = new Parser();
        parser.add(push);
        parser.add(metoo);

        final List<Json> found = new LinkedList<>();
        parser.add(found::add);

        parser.onLine("one=uno");
        parser.onLine("a_apple");
        parser.onLine("b_banana");
        parser.onLine("two=dos");
        parser.onLine("c_cat");
        parser.onLine("d_dog");
        parser.close();

        
        assertEquals("should only emit 1 json",1,found.size());

        Json parsed = found.get(0);

        Json expected = Json.fromString("[{\"value\":\"uno\",\"key\":\"one\",\"option\":[{\"name\":\"a\",\"rank\":\"apple\"},{\"name\":\"b\",\"rank\":\"banana\"}]},{\"value\":\"dos\",\"key\":\"two\",\"option\":[{\"name\":\"c\",\"rank\":\"cat\"},{\"name\":\"d\",\"rank\":\"dog\"}]}]\n");
        assertEquals("json should be an array matching expected",expected,parsed);
    }
}
