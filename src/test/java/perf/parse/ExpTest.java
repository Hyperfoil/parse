package perf.parse;

import org.junit.Ignore;
import org.junit.Test;
import perf.parse.internal.CheatChars;
import perf.parse.internal.DropString;
import perf.parse.internal.JsonBuilder;
import perf.yaup.json.Json;

import java.util.function.Function;

import static org.junit.Assert.*;

/**
 *
 */
public class ExpTest {

    Function<String, DropString> factory = CheatChars::new;

    @Test
    public void testInsuranceDriverStat(){
        ExpOld test = new ExpOld("InsuranceDriver","(?<seconds>\\d+\\.\\d{2})s - InsuranceDriver: ")
            .add(new ExpOld("stat"," (?<key>[^\\s=]+)=").set(Rule.Repeat).group("stat").set(Merge.Entry)
                .add(new ExpOld("value","^/?(?<value>-|\\d+\\.\\d{3})").set(Rule.Repeat)))
            .add(new ExpOld("group","(?<group>[^/]+)[/|$]").set(Rule.Repeat))
            ;

        DropString input = factory.apply("1800.02s - InsuranceDriver: REST - Accept Quote/REST - Add Vehicle/REST - Delete Vehicle/REST - Login/REST - Logout/REST - Register/REST - Register Invalid/REST - Unregister/REST - Update User/REST - View Insurance/REST - View Quote/REST - View User/REST - View Vehicle CThru=114.592/130.791/50.430/427.272/424.505/41.431/11.166/27.232/52.030/51.597/132.058/78.661/170.655 OThru=114.527/132.157/51.557/427.414/427.291/42.737/10.774/26.162/52.975/49.549/132.237/79.503/164.191 CErr=0.000/0.000/0.000/0.000/0.000/0.000/0.000/0.000/0.000/0.000/0.000/0.000/0.000 CResp=0.009/0.003/0.004/0.001/0.000/0.002/0.003/0.003/0.004/0.001/0.002/0.002/0.001 OResp=0.011/0.005/0.006/0.002/0.000/0.003/0.006/0.004/0.005/0.003/0.004/0.003/0.003 CSD=0.004/0.003/0.000/0.000/0.000/0.000/0.000/0.000/0.050/0.000/0.003/0.000/0.002 OSD=0.038/0.033/0.036/0.029/0.000/0.031/0.057/0.029/0.033/0.027/0.028/0.030/0.028 C90%Resp=0.020/0.010/0.010/0.010/0.010/0.010/0.010/0.010/0.010/0.010/0.010/0.010/0.010 O90%Resp=0.020/0.010/0.010/0.010/0.010/0.010/0.010/0.010/0.010/0.010/0.010/0.010/0.010");

        JsonBuilder b = new JsonBuilder();
        test.apply(input,b,null);

        System.out.println(b.getRoot().toString(2));
        System.out.println("Remainder=||"+input.toString()+"||");

    }


    @Test
    public void pushTarget_named(){
        ExpOld push = new ExpOld("push","(?<a>a)").set(Rule.PushTarget,"a");

        JsonBuilder b = new JsonBuilder();
        push.apply(factory.apply("a"),b,null);

        assertEquals("target Name","a",b.getContextString(JsonBuilder.NAME_KEY,false));
    }
    @Test
    public void prePopTarget_named(){
        ExpOld a = new ExpOld("a","(?<a>a)").set(Rule.PrePopTarget,"popMe");

        JsonBuilder b = new JsonBuilder();
        b.getTarget().set("Name",0);
        b.pushTarget(new Json(),"popMe");
        b.getTarget().set("Name",1);
        b.pushTarget(new Json());
        b.getTarget().set("Name",2);
        a.apply(factory.apply("a"),b,null);

        assertFalse("no named target",b.hasContext(JsonBuilder.NAME_KEY,true));
    }

    @Test
    public void rootTarget(){
        ExpOld group = new ExpOld("group","(?<a>a)").group("one").group("two");
        ExpOld root = new ExpOld("root","(?<b>b)").set(Rule.TargetRoot);
        ExpOld close = new ExpOld("close","c").set(Merge.PreClose);
        ParserOld p = new ParserOld();
        p.add(group);
        p.add(root);
        p.add(close);

        Json json = p.onLine("abc");
        assertTrue("json.b",json.has("b"));
    }
    @Test
    public void rootTarget_group(){
        ExpOld group = new ExpOld("group","(?<a>a)").group("one").group("two");
        ExpOld root = new ExpOld("root","(?<b>b)").group("uno").group("dos").set(Rule.TargetRoot);
        ExpOld close = new ExpOld("close","c").set(Merge.PreClose);
        ParserOld p = new ParserOld();
        p.add(group);
        p.add(root);
        p.add(close);

        Json json = p.onLine("abc");
        assertFalse("json.b",json.has("b"));
        assertTrue("json.uno",json.has("uno"));

    }


    @Test
    public void keySplit(){
        ExpOld exp = new ExpOld("keySplit","(?<foo.bar>.*)");

        Json result = exp.apply("biz");

        assertTrue(result.has("foo"));
        assertTrue(result.getJson("foo").has("bar"));
        assertEquals("biz",result.getJson("foo").getString("bar"));
    }

    @Test
    public void valueFrom(){
        assertEquals("Default value should be Field", Value.Key, Value.from("fooooooo"));
    }
    @Test
    public void eatFrom(){
        assertEquals("default Eat should be Width", Eat.Width, Eat.from(1));
        assertEquals("default Eat should be Width", Eat.Width, Eat.from(10));
        assertEquals("Failed to identify Eat.None", Eat.None, Eat.from(Eat.None.getId()));
    }
    @Test
    public void parseKMG(){

        assertEquals("wrong value for 1", Math.pow(1024.0,0), ExpOld.parseKMG("1"), 0.000);
        assertEquals("wrong value for 8b (expected 1 byte)",Math.pow(1024.0,0), ExpOld.parseKMG("8b"),0.000);
        assertEquals("wrong value for 1k",Math.pow(1024.0,1), ExpOld.parseKMG("1k"),0.000);
        assertEquals("wrong value for 1m",Math.pow(1024.0,2), ExpOld.parseKMG("1m"),0.000);
        assertEquals("wrong value for 1g",Math.pow(1024.0,3), ExpOld.parseKMG("1g"),0.000);
        assertEquals("wrong value for 1t",Math.pow(1024.0,4), ExpOld.parseKMG("1t"),0.000);

    }

    @Test
    public void groupOrder(){
        JsonBuilder b = new JsonBuilder();
        ExpOld p = new ExpOld("kv","(?<key>\\w+)=(?<value>\\w+)").group("first").group("second");
        p.apply(factory.apply("foo=bar"),b,null);
        assertTrue("Groupings should be FiFo starting at root", b.getRoot().has("first"));
        assertTrue("Groupings should be FiFo starting at root", b.getRoot().getJson("first").has("second"));

    }
    @Test
    public void valueInPattern(){
        ExpOld p = new ExpOld("valueInpattern","(?<id:targetId>\\w+) (?<nest:nestLength>\\w+) (?<key:key>\\w+)(?<size:kmg>\\w+) ");
        assertEquals("id should use targetId value",Value.TargetId,p.get("id"));
        assertEquals("nest should use key value",Value.NestLength,p.get("nest"));
        assertEquals("key should use key value",Value.Key,p.get("key"));

        assertEquals("size should use kmg value",Value.KMG,p.get("size"));
    }

    @Test
    public void key(){
        JsonBuilder b = new JsonBuilder();
        ExpOld p = new ExpOld("kv","(?<key>\\w+)=(?<value>\\w+)")
                .key("key");
        p.apply(factory.apply("foo=bar"),b,null);
        assertTrue("Should be grouped by value of key field (foo)", b.getRoot().has("foo"));

    }
    @Test
    public void extend(){
        JsonBuilder b = new JsonBuilder();
        Json status = new Json(false);
        b.getRoot().set("status",status);

        ExpOld p = new ExpOld("kv","(?<key>\\w+)=(?<value>\\w+)").extend("status");
        p.apply(factory.apply("foo=bar"),b,null);
        p.apply(factory.apply("fizz=fuzz"),b,null);

        assertTrue("Status should have value and keyas child objects but is "+b.getRoot(), status.has("value") && status.has("key"));
    }


    @Test
    public void extend_group(){
        JsonBuilder b = new JsonBuilder();
        Json status = new Json(false);
        b.getRoot().set("status",status);

        ExpOld p = new ExpOld("kv","(?<key>\\w+)=(?<value>\\w+)").extend("status").group("lock").set(Merge.Entry);
        p.apply(factory.apply("foo=bar"),b,null);
        p.apply(factory.apply("fizz=fuzz"),b,null);

        assertTrue("Status should have <lock> as child object but is "+b.getRoot(), status.has("lock"));
    }

    @Test
    public void nest_entry(){

        ExpOld norm = new ExpOld("kv","\\s*(?<key>\\S+)\\s*:\\s*(?<value>.*)")
            .set(Merge.Entry)
            .eat(Eat.Match);
        ExpOld nest = new ExpOld("nest","(?<child:nestLength>[\\s-]*-\\s*)")
            .eat(Eat.Match)
            .add(norm);

        ParserOld p = new ParserOld();
        p.add(nest);
        p.add(norm);

        p.onLine("foo:bar");
        p.onLine("  - a : Alpha");
        p.onLine("    b : Bravo");
        p.onLine("  - y : Yankee");
        p.onLine("    z : Zulu");

        Json root = p.getBuilder().getRoot();

        Json expected = Json.fromString("{\"key\":\"foo\",\"value\":\"bar\",\"child\":[[{\"key\":\"a\",\"value\":\"Alpha\"},{\"key\":\"b\",\"value\":\"Bravo\"}],[{\"key\":\"y\",\"value\":\"Yankee\"},{\"key\":\"z\",\"value\":\"Zulu\"}]]}\n");
        assertEquals("root should have 2 children each with 2 entries",expected,root);
    }

    @Test
    public void value_nestLength(){

        JsonBuilder b = new JsonBuilder();
        ExpOld p = new ExpOld("tree","(?<nest>\\s*)(?<Name>\\w+)");
        p.set("nest", Value.NestLength);
        //b.getRoot().add("Name","root");

        p.apply(factory.apply("a"),b,null);
        p.apply(factory.apply(" aa"),b,null);
        p.apply(factory.apply("  aaa"),b,null);
        p.apply(factory.apply("b"),b,null);

        assertTrue("tree should use nest as the child key:\n"+b.getRoot().toString(2),b.getRoot().has("nest") && b.getRoot().getJson("nest").isArray());
        assertTrue("expect only one key on root json:\n"+b.getRoot().toString(2),b.getRoot().size()==1);
        assertTrue("expect nest to have 2 children:\n"+b.getRoot().getJson("nest").toString(2),b.getRoot().getJson("nest").size()==2);
        assertTrue("expect a to have one child",b.getRoot().getJson("nest").getJson(0).getJson("nest").isArray() && b.getRoot().getJson("nest").getJson(0).getJson("nest").size()==1);
        assertTrue("expect a.aa to have one child",b.getRoot().getJson("nest").getJson(0).getJson("nest").getJson(0).getJson("nest").isArray() && b.getRoot().getJson("nest").getJson(0).getJson("nest").getJson(0).getJson("nest").size()==1);

    }
    @Test
    public void value_nestLength_multi(){

        JsonBuilder b = new JsonBuilder();
        ExpOld p = new ExpOld("tree","(?<nest>\\s*)(?<Name>\\w+)");
        p.set("nest", Value.NestLength);
        //b.getRoot().add("Name","root");

        p.apply(factory.apply("a"),b,null);
        p.apply(factory.apply("  aa"),b,null);
        p.apply(factory.apply("    aaa"),b,null);
        p.apply(factory.apply("    aab"),b,null);
        p.apply(factory.apply("      aaba"),b,null);
        p.apply(factory.apply("    aac"),b,null);
        p.apply(factory.apply("b"),b,null);

        Json root = b.getRoot();
        assertTrue("root has nest:",root.has("nest") && root.get("nest") instanceof Json);
        Json nest = root.getJson("nest");
        assertEquals("nest has 2 children",2,nest.size());
        assertEquals("nest[0].Name","a",nest.getJson(0).getString("Name"));
        assertEquals("nest[1].Name","b",nest.getJson(1).getString("Name"));
    }

    @Test
    public void valueTargetId(){
        JsonBuilder b = new JsonBuilder();
        ExpOld p = new ExpOld("tid","(?<id>\\d+) (?<Name>\\S+)");
        p.set("id",Value.TargetId);
        p.apply(factory.apply("1 foo"),b,null);
        p.apply(factory.apply("1 bar"),b,null);

        assertTrue("root should have a Name entry but was: "+b.getRoot(),b.getRoot().has("Name"));
        assertTrue("root should have two Name values but was: "+b.getRoot(),b.getRoot().getJson("Name").size()==2);
        p.apply(factory.apply("2 biz"),b,null);
        p.apply(factory.apply("2 fiz"),b,null);
        assertTrue("root should have a Name entry but was: "+b.getRoot(),b.getRoot().has("Name"));
        assertTrue("root should have two Name values but was: "+b.getRoot(),b.getRoot().getJson("Name").size()==2);

    }
    @Test
    public void autoNumber(){
        JsonBuilder b = new JsonBuilder();
        ExpOld p = new ExpOld("kv","(?<key>\\w+)=(?<value>\\w+)");
        p.apply(factory.apply("age=23"),b,null);

        assertEquals(23,b.getRoot().getLong("value"));
    }
    @Test
    public void valueKMG(){
        JsonBuilder b = new JsonBuilder();
        ExpOld p = new ExpOld("kv","(?<key>\\w+)=(?<value>\\w+)").set("value", Value.KMG);
        p.apply(factory.apply("age=1G"),b,null);
        assertEquals(Math.pow(1024.0,3),b.getRoot().getLong("value"),0.000);
    }

    @Test
    public void valueCount(){
        JsonBuilder b = new JsonBuilder();
        ExpOld p = new ExpOld("kv","(?<key>\\w+)=(?<value>\\w+)").set("value", Value.Count);
        p.apply(factory.apply("age=old"),b,null);
        assertEquals(1,b.getRoot().getLong("old"));
        p.apply(factory.apply("age=old"),b,null);
        assertEquals(2,b.getRoot().getLong("old"));
    }
    @Test
    public void valueSum(){
        JsonBuilder b = new JsonBuilder();
        ExpOld p = new ExpOld("kv","(?<key>\\w+)=(?<value>\\w+)").set("value", Value.Sum);
        p.apply(factory.apply("age=23"),b,null);
        assertEquals(23,b.getRoot().getLong("value"));
        p.apply(factory.apply("age=23"),b,null);
        assertEquals(46,b.getRoot().getLong("value"));
    }
    @Test
    public void valueKey(){
        JsonBuilder b = new JsonBuilder();
        ExpOld p = new ExpOld("kv","(?<key>\\w+)=(?<value>\\w+)").set("key", "value");
        p.apply(factory.apply("age=23"),b,null);

        assertTrue("Should turn value of <key> to the key for <value>", b.getRoot().has("age"));
        assertEquals("expected {\"age\":\"23\"}","23",b.getRoot().getString("age"));
    }
    @Test
    public void valueBooleanKey(){
        JsonBuilder b = new JsonBuilder();
        ExpOld p = new ExpOld("kv","(?<key>\\w+)=(?<value>\\w+)").set("key", Value.BooleanKey);
        p.apply(factory.apply("age=23"),b,null);
        assertEquals("value should be a boolean",true,b.getRoot().getBoolean("key"));
    }
    @Test
    public void valueBooleanValue(){
        JsonBuilder b = new JsonBuilder();
        ExpOld p = new ExpOld("kv","(?<key>\\w+)=(?<value>\\w+)").set("key", Value.BooleanValue);
        p.apply(factory.apply("age=23"),b,null);
        assertEquals("value should be a boolean",true,b.getRoot().getBoolean("age"));
    }
    @Test
    public void valuePosition(){
        JsonBuilder b = new JsonBuilder();
        ExpOld p = new ExpOld("kv","(?<key>\\w+)=(?<value>\\w+)").set("key", Value.Position);
        p.apply(factory.apply("012345 age=23"),b,null);
        assertEquals("should equal the offset from start of line", 7, b.getRoot().getLong("key"));
    }
    @Test
    public void valueString(){
        JsonBuilder b = new JsonBuilder();
        ExpOld p = new ExpOld("kv","(?<key>\\w+)=(?<value>\\w+)").set("value", Value.String);
        p.apply(factory.apply("age=23"),b,null);
        p.apply(factory.apply("age=23"),b,null);

        assertEquals("<value> should use string concat rather than list append", "2323", b.getRoot().getString("value"));
    }
    @Test
    public void valueList(){
        JsonBuilder b = new JsonBuilder();
        ExpOld p = new ExpOld("kv","(?<key>\\w+)=(?<value>\\w+)").set("value", Value.List);
        p.apply(factory.apply("age=23"),b,null);
        p.apply(factory.apply("age=23"),b,null);

        assertTrue("<key> should be treated as a list by default",b.getRoot().get("key") instanceof Json);
        assertTrue("<value> should be treated as a list",b.getRoot().get("value") instanceof Json);
    }

    @Test
    public void eatMatch(){
        DropString line = factory.apply("age=23, age=24, age=26");
        JsonBuilder b = new JsonBuilder();
        ExpOld p = new ExpOld("kv","(?<key>\\w+)=(?<value>\\w+)").eat(Eat.Match);
        p.apply(line, b, null);
        assertEquals("should remove the matched string", ", age=24, age=26", line.toString());
    }
    @Test
    public void eatToMatch(){
        DropString line = factory.apply("foo=1 bar=1 foo=2");
        JsonBuilder b = new JsonBuilder();
        ExpOld p = new ExpOld("bar","bar=(?<bar>\\S+)").eat(Eat.ToMatch);
        p.apply(line,b,null);
        assertEquals("should remove first foo"," foo=2",line.toString());
    }


    @Test
    public void eatWidth(){
        DropString line = factory.apply("age=23, age=24, age=26");
        JsonBuilder b = new JsonBuilder();
        ExpOld p = new ExpOld("kv","(?<key>\\w+)=(?<value>\\w+)").eat(8);
        p.apply(line,b,null);
        assertEquals("should remove the matched string","age=24, age=26",line.toString());
    }
    @Test
    public void eatLine(){
        DropString line = factory.apply("age=23, age=24, age=26");
        JsonBuilder b = new JsonBuilder();
        ExpOld p = new ExpOld("kv","(?<key>\\w+)=(?<value>\\w+)").eat(Eat.Line);
        p.apply(line,b,null);
        assertEquals("should remove the matched string","",line.toString());
    }

    @Test
    public void lineStart(){
        JsonBuilder b = new JsonBuilder();
        ExpOld p = new ExpOld("kv", "(?<key>\\w+)=(?<value>\\w+)")
                .add( new ExpOld("-","(?<sk>\\w+)\\-(?<sv>\\w+)").set(Rule.LineStart) );

        p.apply(factory.apply("a-b key=value foo-bar"),b,null);
        assertEquals("should match first <sk>-<sv> not the last","a",b.getRoot().getString("sk"));
    }

    @Test @Ignore
    public void nestmap(){
        JsonBuilder b = new JsonBuilder();
        ExpOld open = new ExpOld("start","^\\s*\\{")
            .group("child").set(Rule.PushTarget).set(Merge.Entry).eat(Eat.Match);
        ExpOld comma = new ExpOld("comma","^\\s*,\\s*").eat(Eat.Match);
        ExpOld kvSeparator = new ExpOld("kvSeparator","^\\s*:\\s*").eat(Eat.Match);
        ExpOld key = new ExpOld("key","^\\s*(?<key>[^:\\s,\\]]+)\\s*").eat(Eat.Match).set(Merge.Entry);;
        ExpOld value = new ExpOld("value","^\\s*(?<value>[^,\\}\\{]*[^\\s,\\}\\{])").eat(Eat.Match);
        ExpOld close = new ExpOld("close","^\\s*\\}")
                .eat(Eat.Match)
                .set(Rule.PostPopTarget)
                .set(Rule.PostPopTarget)
                ;
        ParserOld p = new ParserOld();
        p.add(open.clone().set(Rule.RepeatChildren)
                .add(comma)
                .add(open.clone().set(Rule.Repeat))
                .add(close.set(Rule.Repeat))
                .add(key
                    .add(kvSeparator
                        .add(value)
                    )
                )
        );


        //p.onLine("{ a : Alpha, b : Bravo, c : { c.a : Able, c.b : Ben }, d : Dan}");

        p.onLine("{ a : Alpha, b : Bravo {b.a: Able, b.b: Ben }, c : Charlie }");
        Json root = p.getBuilder().getRoot();



    }

    @Test @Ignore
    public void nestLists(){

        //works with a double pop
        JsonBuilder b = new JsonBuilder();
        ExpOld open = new ExpOld("start","^\\s*\\[")
                .group("child").set(Rule.PushTarget).set(Merge.Entry).eat(Eat.Match);
        ExpOld comma = new ExpOld("comma","^\\s*,\\s*").eat(Eat.Match);
        ExpOld entry = new ExpOld("entry","^\\s*(?<key>[^:\\s,\\]]+)\\s*").eat(Eat.Match).set(Merge.Entry);
        ExpOld close = new ExpOld("close","^\\s*]")
                .eat(Eat.Match)
                .set(Rule.PostPopTarget)
                .set(Rule.PostPopTarget);

        ParserOld p = new ParserOld();
        p.add(open.clone().set(Rule.RepeatChildren)
                .add(comma)
                .add(open.clone())
                .add(close)
                .add(entry)
        );

        p.onLine("[ alpha, bravo, [b.b b.c], charlie, [ yankee, zulu ] delta ]");

        Json root = p.getBuilder().getRoot();


    }

    @Test
    public void repeat(){
        JsonBuilder b = new JsonBuilder();
        ExpOld p = new ExpOld("num","(?<num>\\d+)").set(Rule.Repeat);
        p.apply(factory.apply("1 2 3 4"),b,null);
        assertEquals("num should be an array with 4 elements",4,b.getRoot().getJson("num").size());
    }
    @Test
    public void pushTarget(){
        JsonBuilder b = new JsonBuilder();
        ExpOld p = new ExpOld("num","(?<num>\\d+)").group("pushed").set(Rule.PushTarget).set(Merge.Entry);
        p.apply(factory.apply(" 1 "), b, null);
        p.apply(factory.apply(" 2 "), b, null);
        p.apply(factory.apply(" 3 "), b, null);

        assertTrue("context should not equal root",b.getRoot()!=b.getTarget());
    }
    @Test
    public void postPopTarget(){
        JsonBuilder b = new JsonBuilder();
        Json lost = new Json();
        lost.set("lost","lost");
        b.pushTarget(lost);
        ExpOld p = new ExpOld("num","(?<num>\\d+)").group("pushed").set(Rule.PostPopTarget);
        p.apply(factory.apply(" 1 "),b,null);
        assertTrue("context should not equal starting context", lost != b.getTarget());
        assertTrue("fields should be applied to context before pop",lost.has("pushed"));
    }
    @Test
    public void prePopTarget(){
        JsonBuilder b = new JsonBuilder();
        Json lost = new Json();
        lost.set("lost","lost");
        b.pushTarget(lost);
        ExpOld p = new ExpOld("num","(?<num>\\d+)").group("pushed").set(Rule.PrePopTarget);
        p.apply(factory.apply(" 1 "), b, null);
        assertTrue("context should not equal starting context",lost!=b.getTarget());
        assertTrue("fields should be applied to context before pop", b.getTarget().has("pushed"));
    }
    @Test
    public void postClearTarget(){
        JsonBuilder b = new JsonBuilder();
        Json first = new Json();
        first.set("ctx", "lost");
        Json second = new Json();
        second.set("ctx","second");
        b.pushTarget(first);
        b.pushTarget(second);
        ExpOld p = new ExpOld("num","(?<num>\\d+)").group("pushed").set(Rule.PostClearTarget);
        p.apply(factory.apply(" 1 "),b,null);
        assertTrue("context should not equal starting context",b.getRoot()==b.getTarget());
        assertTrue("fields should be applied to context before pop", second.has("pushed"));
    }

    @Test
    public void newStart(){
        JsonBuilder b = new JsonBuilder();
        ExpOld p = new ExpOld("num","(?<num>\\d+)").set(Merge.PreClose);
        p.apply(factory.apply(" 1 "),b,null);
        p.apply(factory.apply(" 2 "), b, null);
        assertEquals("matches should not be combined", 2, b.getRoot().getLong("num"));
        assertTrue("previous match should be moved to a closed root",b.wasClosed());
    }

    // I don't see a difference between extend and group
    @Test
    public void extend_entry(){
        JsonBuilder b = new JsonBuilder();
        ExpOld p = new ExpOld("num","(?<num>\\d+)").extend("nums").set(Merge.Entry);
        p.apply(factory.apply(" 1 "),b,null);
        p.apply(factory.apply(" 2 "), b, null);
        assertEquals("nums should have 2 entries",2,b.getRoot().getJson("nums").size());
    }
    @Test
    public void group_entry(){
        JsonBuilder b = new JsonBuilder();
        ExpOld p = new ExpOld("num","(?<num>\\d+)").group("nums").set(Merge.Entry);
        p.apply(factory.apply(" 1 "),b,null);
        p.apply(factory.apply(" 2 "), b, null);
        assertEquals("nums should have 2 entries",2,b.getRoot().getJson("nums").size());

    }
    @Test
    public void group_extend(){
        JsonBuilder b = new JsonBuilder();
        ExpOld p = new ExpOld("kv", "(?<key>\\w+)=(?<value>\\w+)").set("key","value").group("kv").set(Merge.Extend);
        p.apply(factory.apply(" age=23 "),b,null);
        p.apply(factory.apply(" size=small "),b,null);
        assertEquals("<kv> should be and array of length 1",1,b.getRoot().getJson("kv").size());
    }
    @Test
    public void collection(){
        JsonBuilder b = new JsonBuilder();
        ExpOld p = new ExpOld("kv", "(?<key>\\w+)=(?<value>\\w+)").set("key","value").group("kv").set(Merge.Collection);
        p.apply(factory.apply(" age=23 "),b,null);
        p.apply(factory.apply(" size=small "),b,null);

        assertTrue("<kv> should have kv.size",b.getRoot().getJson("kv").has("size"));
        assertTrue("<kv> should have kv.age",b.getRoot().getJson("kv").has("age"));
    }


    @Test
    public void childOrder(){
        JsonBuilder b = new JsonBuilder();
        ExpOld p = new ExpOld("start","\\[").eat(Eat.Match)
                .add(new ExpOld("quoted","^\\s*,?\\s*\"(?<value>[^\"]+)\"")
                    .group("child").set(Merge.Entry).eat(Eat.Match)
                )
                .add(new ExpOld("normal","^\\s*,?\\s*(?<value>[^,\\]]*[^\\s,\\]])")
                    .group("child").set(Merge.Entry).eat(Eat.Match)
                )
                .set(Rule.RepeatChildren);

        p.apply(factory.apply("[ aa, \"bb,bb]bb\" ,cc]"),b,null);

        Json json = b.getRoot();
        assertTrue("match should contain a child entry",json.has("child"));
        assertEquals("child should contain 3 entires",3,json.getJson("child").size());

    }

}
