package io.hyperfoil.tools.parse;

import io.hyperfoil.tools.yaup.json.Json;
import org.junit.Test;

import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.*;

public class ExpRuleTest {

    @Test
    public void repeat(){
        Exp exp = new Exp(",?(?<v>[^,$]+)").addRule(ExpRule.Repeat);

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
    public void asentry_prepoptarget_pushtarget(){
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
