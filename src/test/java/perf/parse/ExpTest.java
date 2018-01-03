package perf.parse;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Ignore;
import org.junit.Test;
import perf.parse.internal.CheatChars;
import perf.parse.internal.JsonBuilder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 *
 */
public class ExpTest {

    @Test
    public void valueFrom(){
        assertEquals("Default value should be Key", Value.Key, Value.from("fooooooo"));
        assertEquals("Failed to identify Value.Number", Value.Number, Value.from(Value.Number.getId()));
    }
    @Test
    public void eatFrom(){
        assertEquals("default Eat should be Width", Eat.Width, Eat.from(1));
        assertEquals("default Eat should be Width", Eat.Width, Eat.from(10));
        assertEquals("Failed to identify Eat.None", Eat.None, Eat.from(Eat.None.getId()));
    }
    @Test
    public void parseKMG(){

        assertEquals("wrong value for 1", Math.pow(1024.0,0), Exp.parseKMG("1"), 0.000);
        assertEquals("wrong value for 8b (expected 1 byte)",Math.pow(1024.0,0), Exp.parseKMG("8b"),0.000);
        assertEquals("wrong value for 1k",Math.pow(1024.0,1), Exp.parseKMG("1k"),0.000);
        assertEquals("wrong value for 1m",Math.pow(1024.0,2), Exp.parseKMG("1m"),0.000);
        assertEquals("wrong value for 1g",Math.pow(1024.0,3), Exp.parseKMG("1g"),0.000);
        assertEquals("wrong value for 1t",Math.pow(1024.0,4), Exp.parseKMG("1t"),0.000);

    }

    @Test
    public void groupOrder(){
        JsonBuilder b = new JsonBuilder();
        Exp p = new Exp("kv","(?<key>\\w+)=(?<value>\\w+)").group("first").group("second");
        p.apply(new CheatChars("foo=bar"),b,null);
        assertTrue("Groupings should be FiFo starting at root", b.getRoot().has("first"));
        assertTrue("Groupings should be FiFo starting at root", b.getRoot().getJSONObject("first").has("second"));

    }

    @Test
    public void keyGroup(){
        JsonBuilder b = new JsonBuilder();
        Exp p = new Exp("kv","(?<key>\\w+)=(?<value>\\w+)")
                .key("key");
        p.apply(new CheatChars("foo=bar"),b,null);
        assertTrue("Should be grouped by value of key field (foo)", b.getRoot().has("foo"));

    }
    @Test
    public void extendGroup(){
        JsonBuilder b = new JsonBuilder();
        JSONObject status = new JSONObject();
        b.getRoot().put("status",status);

        Exp p = new Exp("kv","(?<key>\\w+)=(?<value>\\w+)").extend("status").group("lock").set(Merge.Entry);
        p.apply(new CheatChars("foo=bar"),b,null);
        p.apply(new CheatChars("fizz=fuzz"),b,null);

        assertTrue("Status should have <lock> as child object", status.has("lock"));
    }
    @Test
    @Ignore
    public void valueNestLength(){

        //TODO nestLength is broken

        JsonBuilder b = new JsonBuilder();
        Exp p = new Exp("nest","(?<nest>\\s*)(?<name>\\w+)");
                p.set("nest", Value.NestLength);
        b.getRoot().accumulate("name","root");

        p.apply(new CheatChars("parent1"),b,null);
        p.apply(new CheatChars(" child1"),b,null);
        p.apply(new CheatChars(" child2"),b,null);
        p.apply(new CheatChars("parent2"),b,null);

    }

    @Test
    public void valueNumber(){
        JsonBuilder b = new JsonBuilder();
        Exp p = new Exp("kv","(?<key>\\w+)=(?<value>\\w+)").set("value", Value.Number);
        p.apply(new CheatChars("age=23"),b,null);

        assertEquals(23,b.getRoot().getInt("value"));
    }
    @Test
    public void valueKMG(){
        JsonBuilder b = new JsonBuilder();
        Exp p = new Exp("kv","(?<key>\\w+)=(?<value>\\w+)").set("value", Value.KMG);
        p.apply(new CheatChars("age=1G"),b,null);
        assertEquals(Math.pow(1024.0,3),b.getRoot().getInt("value"),0.000);
    }

    @Test
    public void valueCount(){
        JsonBuilder b = new JsonBuilder();
        Exp p = new Exp("kv","(?<key>\\w+)=(?<value>\\w+)").set("value", Value.Count);
        p.apply(new CheatChars("age=old"),b,null);
        assertEquals(1,b.getRoot().getInt("old"));
        p.apply(new CheatChars("age=old"),b,null);
        assertEquals(2,b.getRoot().getInt("old"));
    }
    @Test
    public void valueSum(){
        JsonBuilder b = new JsonBuilder();
        Exp p = new Exp("kv","(?<key>\\w+)=(?<value>\\w+)").set("value", Value.Sum);
        p.apply(new CheatChars("age=23"),b,null);
        assertEquals(23,b.getRoot().getInt("value"));
        p.apply(new CheatChars("age=23"),b,null);
        assertEquals(46,b.getRoot().getInt("value"));
    }
    @Test
    public void valueKey(){
        JsonBuilder b = new JsonBuilder();
        Exp p = new Exp("kv","(?<key>\\w+)=(?<value>\\w+)").set("key", "value");
        p.apply(new CheatChars("age=23"),b,null);

        assertTrue("Should turn value of <key> to the key for <value>", b.getRoot().has("age"));
    }
    @Test
    public void valueBoolanKey(){
        JsonBuilder b = new JsonBuilder();
        Exp p = new Exp("kv","(?<key>\\w+)=(?<value>\\w+)").set("key", Value.BooleanKey);
        p.apply(new CheatChars("age=23"),b,null);
        assertEquals("value should be a boolean",true,b.getRoot().getBoolean("key"));
    }
    @Test
    public void valueBoolanValue(){
        JsonBuilder b = new JsonBuilder();
        Exp p = new Exp("kv","(?<key>\\w+)=(?<value>\\w+)").set("key", Value.BooleanValue);
        p.apply(new CheatChars("age=23"),b,null);
        assertEquals("value should be a boolean",true,b.getRoot().getBoolean("age"));
    }
    @Test
    public void valuePosition(){
        JsonBuilder b = new JsonBuilder();
        Exp p = new Exp("kv","(?<key>\\w+)=(?<value>\\w+)").set("key", Value.Position);
        p.apply(new CheatChars("012345 age=23"),b,null);
        assertEquals("should equal the offset from start of line", 7, b.getRoot().getInt("key"));
    }
    @Test
    public void valueString(){
        JsonBuilder b = new JsonBuilder();
        Exp p = new Exp("kv","(?<key>\\w+)=(?<value>\\w+)").set("value", Value.String);
        p.apply(new CheatChars("age=23"),b,null);
        p.apply(new CheatChars("age=23"),b,null);

        assertEquals("<value> should use string concat rather than list append", "2323", b.getRoot().getString("value"));
    }
    @Test
    public void valueList(){
        JsonBuilder b = new JsonBuilder();
        Exp p = new Exp("kv","(?<key>\\w+)=(?<value>\\w+)").set("value", Value.List);
        p.apply(new CheatChars("age=23"),b,null);

        p.apply(new CheatChars("age=23"),b,null);

        assertTrue("<key> should be treated as a list by default",b.getRoot().get("key") instanceof JSONArray);
        assertTrue("<value> should be treated as a list",b.getRoot().get("value") instanceof JSONArray);
    }

    @Test
    public void eatMatch(){
        CheatChars line = new CheatChars("age=23, age=24, age=26");
        JsonBuilder b = new JsonBuilder();
        Exp p = new Exp("kv","(?<key>\\w+)=(?<value>\\w+)").eat(Eat.Match);
        p.apply(line, b, null);
        assertEquals("should remove the matched string", ", age=24, age=26", line.toString());
    }
    @Test
    public void eatWidth(){
        CheatChars line = new CheatChars("age=23, age=24, age=26");
        JsonBuilder b = new JsonBuilder();
        Exp p = new Exp("kv","(?<key>\\w+)=(?<value>\\w+)").eat(8);
        p.apply(line,b,null);
        assertEquals("should remove the matched string","age=24, age=26",line.toString());
    }
    @Test
    public void eatLine(){
        CheatChars line = new CheatChars("age=23, age=24, age=26");
        JsonBuilder b = new JsonBuilder();
        Exp p = new Exp("kv","(?<key>\\w+)=(?<value>\\w+)").eat(Eat.Line);
        p.apply(line,b,null);
        assertEquals("should remove the matched string","",line.toString());
    }

    @Test
    public void lineStart(){
        JsonBuilder b = new JsonBuilder();
        Exp p = new Exp("kv", "(?<key>\\w+)=(?<value>\\w+)")
                .add( new Exp("-","(?<sk>\\w+)\\-(?<sv>\\w+)").set(Rule.LineStart) );

        p.apply(new CheatChars("a-b key=value foo-bar"),b,null);

        assertEquals("should match first <sk>-<sv> not the last","a",b.getRoot().getString("sk"));
    }

    @Test
    public void repeat(){
        JsonBuilder b = new JsonBuilder();
        Exp p = new Exp("num","(?<num>\\d+)").set(Rule.Repeat);
        p.apply(new CheatChars("1 2 3 4"),b,null);
        assertEquals("num should be an array with 4 elements",4,b.getRoot().getJSONArray("num").length());
    }
    @Test
    public void pushContext(){
        JsonBuilder b = new JsonBuilder();
        Exp p = new Exp("num","(?<num>\\d+)").group("pushed").set(Rule.PushContext);
        p.apply(new CheatChars(" 1 "), b, null);
        assertTrue("context should not equal root",b.getRoot()!=b.getCurrentContext());
    }
    @Test
    public void popContext(){
        JsonBuilder b = new JsonBuilder();
        JSONObject lost = new JSONObject();
        lost.put("lost","lost");
        b.setCurrentContext(lost);
        Exp p = new Exp("num","(?<num>\\d+)").group("pushed").set(Rule.PopContext);
        p.apply(new CheatChars(" 1 "),b,null);

        assertTrue("context should not equal starting context", lost != b.getCurrentContext());
        assertTrue("fields should be applied to context before pop",lost.has("pushed"));
    }
    @Test
    public void avoidContext(){
        JsonBuilder b = new JsonBuilder();
        JSONObject lost = new JSONObject();
        lost.put("lost","lost");
        b.setCurrentContext(lost);
        Exp p = new Exp("num","(?<num>\\d+)").group("pushed").set(Rule.AvoidContext);
        p.apply(new CheatChars(" 1 "), b, null);

        assertTrue("context should not equal starting context",lost!=b.getCurrentContext());
        assertTrue("fields should be applied to context before pop", b.getCurrentContext().has("pushed"));
    }
    @Test
    public void clearContext(){
        JsonBuilder b = new JsonBuilder();
        JSONObject first = new JSONObject();
        first.put("ctx", "lost");
        JSONObject second = new JSONObject();
        second.put("ctx","second");
        b.setCurrentContext(first);
        b.setCurrentContext(second);
        Exp p = new Exp("num","(?<num>\\d+)").group("pushed").set(Rule.ClearContext);
        p.apply(new CheatChars(" 1 "),b,null);
        assertTrue("context should not equal starting context",b.getRoot()==b.getCurrentContext());
        assertTrue("fields should be applied to context before pop", second.has("pushed"));
    }

    @Test
    public void mergeNewStart(){
        JsonBuilder b = new JsonBuilder();
        Exp p = new Exp("num","(?<num>\\d+)").set(Merge.NewStart).set("num",Value.Number);
        p.apply(new CheatChars(" 1 "),b,null);
        p.apply(new CheatChars(" 2 "), b, null);

        assertEquals("matches should not be combined", 2, b.getRoot().getInt("num"));
        assertTrue("previous match should be moved to a closed root",b.wasClosed());
    }
    @Test
    public void mergeEntry(){
        JsonBuilder b = new JsonBuilder();
        Exp p = new Exp("num","(?<num>\\d+)").group("nums").set(Merge.Entry);
        p.apply(new CheatChars(" 1 "),b,null);
        p.apply(new CheatChars(" 2 "), b, null);
        assertEquals("nums shoudl have 2 entries",2,b.getRoot().getJSONArray("nums").length());
    }
    @Test
    public void mergeExtend(){
        JsonBuilder b = new JsonBuilder();
        Exp p = new Exp("kv", "(?<key>\\w+)=(?<value>\\w+)").set("key","value").group("kv").set(Merge.Extend);
        p.apply(new CheatChars(" age=23 "),b,null);
        p.apply(new CheatChars(" size=small "),b,null);
        assertEquals("<kv> should be and array of length 1",1,b.getRoot().getJSONArray("kv").length());

    }
    @Test
    public void mergeCollection(){
        JsonBuilder b = new JsonBuilder();
        Exp p = new Exp("kv", "(?<key>\\w+)=(?<value>\\w+)").set("key","value").group("kv").set(Merge.Collection);
        p.apply(new CheatChars(" age=23 "),b,null);
        p.apply(new CheatChars(" size=small "),b,null);

        assertTrue("<kv> should have kv.size",b.getRoot().getJSONObject("kv").has("size"));
        assertTrue("<kv> should have kv.age",b.getRoot().getJSONObject("kv").has("age"));
    }
}
