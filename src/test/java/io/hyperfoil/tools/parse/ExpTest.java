package io.hyperfoil.tools.parse;

import io.hyperfoil.tools.parse.internal.CheatChars;
import io.hyperfoil.tools.parse.internal.DropString;
import io.hyperfoil.tools.parse.json.JsonBuilder;
import io.hyperfoil.tools.yaup.json.Json;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static org.junit.Assert.*;

public class ExpTest {

   Function<String, DropString> factory = CheatChars::new;

   @Test
   public void parsePattern_valueType_duration(){
      Map<String,Exp.ValueInfo> parsed = Exp.parsePattern("(?<foo:duration>");
      assertNotNull(parsed);
      assertTrue(parsed.containsKey("foo"));
      assertEquals(ValueType.Duration,parsed.get("foo").getType());
   }

   @Test @Ignore
   public void removePatternValues_negative_lookbehind(){
      String input = "((?<![\\\\])['\"])(?<name>(?:.(?!(?<![\\\\])\\1))*.?)\\1";
      String safe = Exp.removePatternValues(input);
   }

   @Test
   public void parent_disables_child_enables(){
      Parser p = new Parser();
      Exp parent = new Exp("foo","foo").disables("state");
      parent.add(new Exp("bar","bar").enables("state"));
      p.add(parent);
      p.onLine("foo bar");

      assertTrue("child should enable after parent disables",p.getState("state"));
   }


   @Test
   public void nest_push_without_capture(){
      Exp exp = new Exp("foo").nest("foo").addRule(ExpRule.PushTarget).addRule(ExpRule.PreClearTarget);
      Json json = exp.apply("foo");
      assertTrue("json should have a foo key:"+json,json.has("foo"));
      assertTrue("json.foo should be json:"+json,json.get("foo") instanceof Json);
      Json foo = json.getJson("foo");
      assertTrue("foo should be empty:"+foo,foo.isEmpty());
   }

   @Test
   public void fromJson_enables(){
      Json json = Json.fromString("    {\n" +
              "        \"merge\": \"AsEntry\",\n" +
              "        \"name\": \"subnet\",\n" +
              "        \"pattern\":\"subnet\",\n" +
              "        \"rules\": [\n" +
              "            \"PushTarget\"\n" +
              "        ],\n" +
              "        \"nest\": \"subnet\",\n" +
              "        \"enables\": \"subnet\"\n" +
              "    }",null,true);
      assertNotNull("json should not be null",json);
      Exp exp = Exp.fromJson(json);
      assertNotNull("exp should not be null",exp);
      assertTrue("should enable subnet: "+exp.getEnables(),exp.getEnables().contains("subnet"));
   }

   @Test
   public void buildPattern_nothing_parsed(){
      Exp test = new Exp("(?<first>\\w+)")
         .setType("first", ValueType.KMG)
         .setMerge("first",ValueMerge.First);
      String pattern = test.buildPattern();
      Assert.assertEquals("should add :type:merge after name","(?<first:kmg:first>\\w+)",pattern);
   }
   @Test
   public void buildPattern_field_name_matches_merge_name(){
      Exp test = new Exp("(?<foo>\\w+)(?<first>\\w+)")
         .setMerge("foo",ValueMerge.First)
         .setMerge("first",ValueMerge.First);

      String pattern = test.buildPattern();
      Assert.assertEquals("should not replace first in foo description","(?<foo:first>\\w+)(?<first:first>\\w+)",pattern);
   }
   @Test
   public void buildPattern_replace_field_parse(){
      Exp test = new Exp("(?<foo:kmg:first>\\w+)")
         .setType("foo",ValueType.Auto)
         .setMerge("foo",ValueMerge.Auto);
      String pattern = test.buildPattern();
      Assert.assertEquals("existing pattern :type:merge should be removed for auto","(?<foo>\\w+)",pattern);
   }
   @Test
   public void buildPattern_merge_key(){
      Exp test = new Exp("(?<foo>\\w+)(?<bar>\\w+)").setKeyValue("foo","bar");
      String pattern = test.buildPattern();
      Assert.assertEquals("expect key=bar for foo","(?<foo:key=bar>\\w+)(?<bar>\\w+)",pattern);
   }
   @Test
   public void getNest_allOptions(){
      Exp test = new Exp("")
         .key("key").group("group").extend("extend");

      String nest = test.getNest();
      Assert.assertEquals("use key and extend notation",Exp.NEST_KEY_PREFIX+"key"+Exp.NEST_KEY_SUFFIX+".group."+ Exp.NEST_EXTEND_PREFIX+"extend"+Exp.NEST_EXTEND_SUFFIX,nest);
   }
   @Test
   public void getNest_dot_in_group(){
      Exp test = new Exp("").group("key.value");
      String nest = test.getNest();

      Assert.assertEquals(". should be escaped","key\\.value",nest);
   }

   @Test
   public void nest_dot_in_group(){
      Exp test = new Exp("").nest("key\\.value");
      List<String> keys = new ArrayList<>();
      test.eachNest((key,type)->{
         keys.add(key);
      });
      Assert.assertEquals("expect 1 key",1,keys.size());
      Assert.assertEquals("key should remove \\","key.value",keys.get(0));
   }

   @Test
   public void repeat(){
      DropString input = factory.apply("a=apple b=ball c=cat");
      Exp exp = new Exp("foo","(?<key>\\w+)=(?<value>\\w+)")
         .eat(Eat.Match)
         .addRule(ExpRule.Repeat);

      JsonBuilder builder = new JsonBuilder();
      exp.apply(input,builder,null);

      Json json = builder.getRoot();
      Assert.assertEquals(true,json.get("key") instanceof Json);
      Assert.assertEquals(3,json.getJson("key").size());
   }

   @Test
   public void child_beforeParent_eat_toMatch(){
      DropString input = factory.apply("a=apple b=ball a=ant");
      Exp b = new Exp("b","b=(?<b>\\w+)")
         .eat(Eat.ToMatch)
         .add(new Exp("a","a=(?<a>\\w+)").setRange(MatchRange.BeforeParent));
      JsonBuilder builder = new JsonBuilder();
      DropString.Ref starIndex = input.reference(0);
      b.apply(input,builder,null,starIndex);

      Json json = builder.getRoot();
      Assert.assertEquals("ball",json.getString("b"));
      Assert.assertEquals("apple",json.getString("a"));

   }
   @Test
   public void child_beforeParent_then_entireLine(){
      DropString input = factory.apply("a=apple b=ball a=ant");
      Exp b = new Exp("b","b=(?<b>\\w+)")
         .eat(Eat.ToMatch)
         .add(new Exp("a","a=(?<a>\\w+)").setRange(MatchRange.BeforeParent))
         .add(new Exp("again","a=(?<again>\\w+)").setRange(MatchRange.EntireLine));

      JsonBuilder builder = new JsonBuilder();
      DropString.Ref starIndex = input.reference(0);
      b.apply(input,builder,null,starIndex);

      Json json = builder.getRoot();
      Assert.assertEquals("ball\n"+json.toString(2),"ball",json.getString("b"));
      Assert.assertEquals("apple\n"+json.toString(2),"apple",json.getString("a"));
      Assert.assertEquals("again\n"+json.toString(2),"ant",json.getString("again"));
   }
   @Test
   public void testInsuranceDriverStat(){
      Exp test = new Exp("InsuranceDriver","(?<seconds>\\d+\\.\\d{2})s - InsuranceDriver: ")
         .add(new Exp("stat"," (?<key>[^\\s=]+)=").addRule(ExpRule.Repeat).group("stat").setMerge(ExpMerge.AsEntry)
            .add(new Exp("value","^/?(?<value>-|\\d+\\.\\d{3})").addRule(ExpRule.Repeat)))
         .add(new Exp("group","(?<group>[^/]+)[/|$]").addRule(ExpRule.Repeat))
         ;

      DropString input = factory.apply("1800.02s - InsuranceDriver: REST - Accept Quote/REST - Add Vehicle/REST - Delete Vehicle/REST - Login/REST - Logout/REST - Register/REST - Register Invalid/REST - Unregister/REST - Update User/REST - View Insurance/REST - View Quote/REST - View User/REST - View Vehicle CThru=114.592/130.791/50.430/427.272/424.505/41.431/11.166/27.232/52.030/51.597/132.058/78.661/170.655 OThru=114.527/132.157/51.557/427.414/427.291/42.737/10.774/26.162/52.975/49.549/132.237/79.503/164.191 CErr=0.000/0.000/0.000/0.000/0.000/0.000/0.000/0.000/0.000/0.000/0.000/0.000/0.000 CResp=0.009/0.003/0.004/0.001/0.000/0.002/0.003/0.003/0.004/0.001/0.002/0.002/0.001 OResp=0.011/0.005/0.006/0.002/0.000/0.003/0.006/0.004/0.005/0.003/0.004/0.003/0.003 CSD=0.004/0.003/0.000/0.000/0.000/0.000/0.000/0.000/0.050/0.000/0.003/0.000/0.002 OSD=0.038/0.033/0.036/0.029/0.000/0.031/0.057/0.029/0.033/0.027/0.028/0.030/0.028 C90%Resp=0.020/0.010/0.010/0.010/0.010/0.010/0.010/0.010/0.010/0.010/0.010/0.010/0.010 O90%Resp=0.020/0.010/0.010/0.010/0.010/0.010/0.010/0.010/0.010/0.010/0.010/0.010/0.010");

      JsonBuilder b = new JsonBuilder();
      test.apply(input,b,null);


   }

   @Test
   public void asEntry_build_array(){
      Exp exp = new Exp("asEntry","(?<a>.)")
              .setMerge(ExpMerge.AsEntry)
              .addRule(ExpRule.PushTarget)
              .addRule(ExpRule.PrePopTarget);

      JsonBuilder b = new JsonBuilder();
      exp.apply(factory.apply("1"),b,null);
      exp.apply(factory.apply("2"),b,null);

      Json root = b.getRoot();
      assertNotNull(root);
      assertTrue("root should be an array",root.isArray());
      assertEquals("root should have 2 entries",2,root.size());
      assertTrue("root[0] should be json",root.get(0) instanceof Json);
      Json json = root.getJson(0);
      assertTrue("root[0] should have an a",json.has("a"));
      assertTrue("root[1] should be json",root.get(1) instanceof Json);
      json = root.getJson(1);
      assertTrue("root[1] should have an a",json.has("a"));

   }


   @Test
   public void pushTarget_named(){
      Exp push = new Exp("push","(?<a>a)").addRule(ExpRule.PushTarget,"a");

      JsonBuilder b = new JsonBuilder();
      push.apply(factory.apply("a"),b,null);

      Assert.assertEquals("target Name","a",b.getContextString(JsonBuilder.NAME_KEY,false));
   }
   @Test
   public void prePopTarget_named(){
      Exp a = new Exp("a","(?<a>a)").addRule(ExpRule.PrePopTarget,"popMe");

      JsonBuilder b = new JsonBuilder();
      b.getTarget().set("Name",0);
      b.pushTarget(new Json(),"popMe");
      b.getTarget().set("Name",1);
      b.pushTarget(new Json());
      b.getTarget().set("Name",2);
      a.apply(factory.apply("a"),b,null);

      Assert.assertFalse("no named target",b.hasContext(JsonBuilder.NAME_KEY,true));
   }
   @Test
   public void rootTarget(){
      Exp group = new Exp("group","(?<a>a)").group("one").group("two");
      Exp root = new Exp("root","(?<b>b)").addRule(ExpRule.TargetRoot);
      Exp close = new Exp("close","c").addRule(ExpRule.PreClose);
      Parser p = new Parser();
      p.add(group);
      p.add(root);
      p.add(close);

      Json json = p.onLine("abc");
      Assert.assertTrue("json.b",json.has("b"));
   }
   @Test
   public void rootTarget_group(){
      Exp group = new Exp("group","(?<a>a)").group("one").group("two");
      Exp root = new Exp("root","(?<b>b)").group("uno").group("dos").addRule(ExpRule.TargetRoot);
      Exp close = new Exp("close","c").addRule(ExpRule.PreClose);
      Parser p = new Parser();
      p.add(group);
      p.add(root);
      p.add(close);

      Json json = p.onLine("abc");
      Assert.assertFalse("json.b",json.has("b"));
      Assert.assertTrue("json.uno",json.has("uno"));

   }
   @Test
   public void keySplit(){
      Exp exp = new Exp("keySplit","(?<foo.bar>.*)");

      Json result = exp.apply("biz");

      Assert.assertTrue(result.has("foo"));
      Assert.assertTrue(result.getJson("foo").has("bar"));
      Assert.assertEquals("biz",result.getJson("foo").getString("bar"));
   }
   @Test
   public void groupOrder(){
      JsonBuilder b = new JsonBuilder();
      Exp p = new Exp("kv","(?<key>\\w+)=(?<value>\\w+)").group("first").group("second");
      p.apply(factory.apply("foo=bar"),b,null);
      Assert.assertTrue("Groupings should be FiFo starting at root", b.getRoot().has("first"));
      Assert.assertTrue("Groupings should be FiFo starting at root", b.getRoot().getJson("first").has("second"));

   }
   @Test
   public void valueInPattern(){
      Exp p = new Exp("valueInpattern","(?<id:targetId>\\w+) (?<nest:treeSibling>\\w+) (?<key:key>\\w+)(?<size:kmg>\\w+) ");
      Assert.assertEquals("id should use targetId value",ValueMerge.TargetId,p.getMerge("id"));
      Assert.assertEquals("nest should use key value", ValueMerge.TreeSibling,p.getMerge("nest"));
      Assert.assertEquals("key should use key value",ValueMerge.Key,p.getMerge("key"));

      Assert.assertEquals("size should use kmg value",ValueType.KMG,p.getType("size"));
   }
   @Test
   public void key(){
      JsonBuilder b = new JsonBuilder();
      Exp p = new Exp("kv","(?<key>\\w+)=(?<value>\\w+)")
         .key("key");
      p.apply(factory.apply("foo=bar"),b,null);
      Assert.assertTrue("Should be grouped by value of key field (foo)", b.getRoot().has("foo"));
   }
   @Test
   public void key_ignore(){
      JsonBuilder b = new JsonBuilder();
      Exp p = new Exp("kv","(?<key:ignore>\\w+)=(?<value>\\w+)")
              .key("key");
      p.apply(factory.apply("foo=bar"),b,null);
      Assert.assertTrue("Should be grouped by value of key field (foo)", b.getRoot().has("foo"));
      Object foo = b.getRoot().get("foo");
      assertTrue("foo should be json "+foo,foo instanceof Json);
      Json jsonFoo = (Json)foo;
      assertFalse("foo should not have a foo\n"+jsonFoo.toString(2),jsonFoo.has("foo"));
      assertTrue("foo should have value\n"+jsonFoo.toString(2),jsonFoo.has("value"));
      assertEquals("foo.value should be bar","bar",jsonFoo.get("value"));
   }
   @Test
   public void key_only_capture(){
      JsonBuilder b = new JsonBuilder();
      Exp p = new Exp("kv","(?<key>\\w+)")
              .key("key");
      p.apply(factory.apply("foo"),b,null);
      Assert.assertTrue("Should be grouped by value of key field (foo)", b.getRoot().has("foo"));
   }

   @Test
   public void extend(){
      JsonBuilder b = new JsonBuilder();
      Json status = new Json(false);
      b.getRoot().set("status",status);

      Exp p = new Exp("kv","(?<key>\\w+)=(?<value>\\w+)").extend("status");
      p.apply(factory.apply("foo=bar"),b,null);
      p.apply(factory.apply("fizz=fuzz"),b,null);

      Assert.assertTrue("Status should have value and keyas child objects but is "+b.getRoot(), status.has("value") && status.has("key"));
   }
   @Test
   public void extend_group(){
      JsonBuilder b = new JsonBuilder();
      Json status = new Json(false);
      b.getRoot().set("status",status);

      Exp p = new Exp("kv","(?<key>\\w+)=(?<value>\\w+)").extend("status").group("lock").setMerge(ExpMerge.AsEntry);
      p.apply(factory.apply("foo=bar"),b,null);
      p.apply(factory.apply("fizz=fuzz"),b,null);

      Assert.assertTrue("Status should have <lock> as child object but is "+b.getRoot(), status.has("lock"));
   }
   @Test
   public void nest_entry(){

      Exp norm = new Exp("kv","\\s*(?<key>\\S+)\\s*:\\s*(?<value>.*)")
         .setMerge(ExpMerge.AsEntry)
         .eat(Eat.Match);
      Exp nest = new Exp("nest","(?<child:treeSibling>[\\s-]*-\\s*)")

         .eat(Eat.Match)
         .add(norm);

      Parser p = new Parser();
      p.add(nest);
      p.add(norm);

      p.onLine("foo:bar");
      p.onLine("  - a : Alpha");
      p.onLine("    b : Bravo");
      p.onLine("  - y : Yankee");
      p.onLine("    z : Zulu");

      Json root = p.getBuilder().getRoot();

      assertNotNull(root);
      assertTrue("root should now be an array due to change in AsEntry",root.isArray());
      assertEquals("root shoudl have 1 entry",1,root.size());
      root = root.getJson(0);
      Json expected = Json.fromString("{\"key\":\"foo\",\"value\":\"bar\",\"child\":[[{\"key\":\"a\",\"value\":\"Alpha\"},{\"key\":\"b\",\"value\":\"Bravo\"}],[{\"key\":\"y\",\"value\":\"Yankee\"},{\"key\":\"z\",\"value\":\"Zulu\"}]]}\n");
      Assert.assertEquals("root should have 2 children each with 2 entries",expected,root);
   }


   @Test
   public void execute_modifies_match(){
      JsonBuilder b = new JsonBuilder();
      Exp p = new Exp("kv","(?<key>\\w+)=(?<value>\\w+)");
      p.execute((line, match, pattern, parser) -> {
         match.set("value",42);
         match.set("key","");
      });
      p.apply(factory.apply("age=23"),b,null);
      Assert.assertEquals(42,b.getRoot().getLong("value"));
   }
   @Test
   public void value_nestLength(){

      JsonBuilder b = new JsonBuilder();
      Exp p = new Exp("tree","(?<nest>\\s*)(?<Name>\\w+)");
      p.setMerge("nest", ValueMerge.TreeSibling);
      //b.getRoot().add("Name","root");

      p.apply(factory.apply("a"),b,null);
      p.apply(factory.apply(" aa"),b,null);
      p.apply(factory.apply("  aaa"),b,null);
      p.apply(factory.apply("b"),b,null);

      Assert.assertTrue("tree should use nest as the child key:\n"+b.getRoot().toString(2),b.getRoot().has("nest") && b.getRoot().getJson("nest").isArray());
      Assert.assertTrue("expect only one key on root json:\n"+b.getRoot().toString(2),b.getRoot().size()==1);
      Assert.assertTrue("expect nest to have 2 children:\n"+b.getRoot().getJson("nest").toString(2),b.getRoot().getJson("nest").size()==2);
      Assert.assertTrue("expect a to have one child",b.getRoot().getJson("nest").getJson(0).getJson("nest").isArray() && b.getRoot().getJson("nest").getJson(0).getJson("nest").size()==1);
      Assert.assertTrue("expect a.aa to have one child",b.getRoot().getJson("nest").getJson(0).getJson("nest").getJson(0).getJson("nest").isArray() && b.getRoot().getJson("nest").getJson(0).getJson("nest").getJson(0).getJson("nest").size()==1);

   }
   @Test
   public void value_nestLength_multi(){

      JsonBuilder b = new JsonBuilder();
      Exp p = new Exp("tree","(?<nest>\\s*)(?<Name>\\w+)");
      p.setMerge("nest", ValueMerge.TreeSibling);
      //b.getRoot().add("Name","root");

      p.apply(factory.apply("a"),b,null);
      p.apply(factory.apply("  aa"),b,null);
      p.apply(factory.apply("    aaa"),b,null);
      p.apply(factory.apply("    aab"),b,null);
      p.apply(factory.apply("      aaba"),b,null);
      p.apply(factory.apply("    aac"),b,null);
      p.apply(factory.apply("b"),b,null);

      Json root = b.getRoot();
      Assert.assertTrue("root has nest:",root.has("nest") && root.get("nest") instanceof Json);
      Json nest = root.getJson("nest");
      Assert.assertEquals("nest has 2 children",2,nest.size());
      Assert.assertEquals("nest[0].Name","a",nest.getJson(0).getString("Name"));
      Assert.assertEquals("nest[1].Name","b",nest.getJson(1).getString("Name"));
   }
   @Test
   public void valueTargetId(){
      JsonBuilder b = new JsonBuilder();
      Exp p = new Exp("tid","(?<id>\\d+) (?<Name>\\S+)");
      p.setMerge("id",ValueMerge.TargetId);
      p.apply(factory.apply("1 foo"),b,null);
      p.apply(factory.apply("1 bar"),b,null);

      Assert.assertTrue("root should have a Name entry but was: "+b.getRoot(),b.getRoot().has("Name"));
      Assert.assertTrue("root should have two Name values but was: "+b.getRoot(),b.getRoot().getJson("Name").size()==2);
      p.apply(factory.apply("2 biz"),b,null);
      p.apply(factory.apply("2 fiz"),b,null);
      Assert.assertTrue("root should have a Name entry but was: "+b.getRoot(),b.getRoot().has("Name"));
      Assert.assertTrue("root should have two Name values but was: "+b.getRoot(),b.getRoot().getJson("Name").size()==2);

   }
   @Test
   public void autoNumber(){
      JsonBuilder b = new JsonBuilder();
      Exp p = new Exp("kv","(?<key>\\w+)=(?<value>\\w+)");
      p.apply(factory.apply("age=23"),b,null);

      Assert.assertEquals(23,b.getRoot().getLong("value"));
   }
   @Test
   public void autoNumber_disabled(){
      JsonBuilder b = new JsonBuilder();
      Exp p = new Exp("kv","(?<key>\\w+)=(?<value:string>\\w+)");
      p.apply(factory.apply("age=23"),b,null);

      Assert.assertEquals("23",b.getRoot().get("value"));
   }
   @Test
   public void valueKMG(){
      JsonBuilder b = new JsonBuilder();
      Exp p = new Exp("kv","(?<key>\\w+)=(?<value>\\w+)").setType("value", ValueType.KMG);
      p.apply(factory.apply("age=1G"),b,null);
      Assert.assertEquals(Math.pow(1024.0,3),b.getRoot().getLong("value"),0.000);
   }
   @Test
   public void valueCount(){
      JsonBuilder b = new JsonBuilder();
      Exp p = new Exp("kv","(?<key>\\w+)=(?<value>\\w+)").setMerge("value", ValueMerge.Count);
      p.apply(factory.apply("age=old"),b,null);
      Assert.assertEquals(1,b.getRoot().getLong("old"));
      p.apply(factory.apply("age=old"),b,null);
      Assert.assertEquals(2,b.getRoot().getLong("old"));
   }
   @Test
   public void valueSum(){
      JsonBuilder b = new JsonBuilder();
      Exp p = new Exp("kv","(?<key>\\w+)=(?<value>\\w+)").setMerge("value", ValueMerge.Add);
      p.apply(factory.apply("age=23"),b,null);
      Assert.assertEquals(23,b.getRoot().getLong("value"));
      p.apply(factory.apply("age=23"),b,null);
      Assert.assertEquals(46,b.getRoot().getLong("value"));
   }

   @Test
   public void valueKey(){
      JsonBuilder b = new JsonBuilder();
      Exp p = new Exp("kv","(?<key>\\w+)=(?<value>\\w+)").setKeyValue("key", "value");
      p.apply(factory.apply("age=23"),b,null);
      Assert.assertTrue("Should turn value of <key> to the key for <value>", b.getRoot().has("age"));
      Assert.assertEquals("expected {\"age\":\"23\"}","23",b.getRoot().getString("age"));
      Json json = p.apply("age=23");
      assertNotNull("json should not be null",json);
      assertTrue("json.age should exist"+json,json.has("age"));
      assertEquals("json should have 1 key"+json,1,json.size());
   }
   @Test
   public void valueKey_in_pattern(){
      JsonBuilder b = new JsonBuilder();
      Exp p = new Exp("kv","(?<key:key=value>\\w+)=(?<value>\\w+)");
      Json json = p.apply("age=23");
      assertNotNull("json should not be null",json);
      assertTrue("json.age should exist"+json,json.has("age"));
      assertEquals("json should have 1 key"+json,1,json.size());

   }
   @Test
   public void valueBooleanKey(){
      JsonBuilder b = new JsonBuilder();
      Exp p = new Exp("kv","(?<key>\\w+)=(?<value>\\w+)").setMerge("key", ValueMerge.BooleanKey);
      p.apply(factory.apply("age=23"),b,null);
      Assert.assertEquals("value should be a boolean",true,b.getRoot().getBoolean("key"));
   }
   @Test
   public void valueBooleanValue(){
      JsonBuilder b = new JsonBuilder();
      Exp p = new Exp("kv","(?<key>\\w+)=(?<value>\\w+)").setMerge("key", ValueMerge.BooleanValue);
      p.apply(factory.apply("age=23"),b,null);
      Assert.assertEquals("value should be a boolean\n"+b.getRoot().toString(2),true,b.getRoot().getBoolean("age"));
   }
   @Test @Ignore
   public void valuePosition(){//TODO don't have ValueType.Position
//      JsonBuilder b = new JsonBuilder();
//      Exp p = new Exp("kv","(?<key>\\w+)=(?<value>\\w+)").setType("key", ValueType.Position);
//      p.apply(factory.apply("012345 age=23"),b,null);
//      assertEquals("should equal the offset from start of line", 7, b.getRoot().getLong("key"));
   }
   @Test
   public void valueString(){//now have to explicitly set ValueMerge=ADD
      JsonBuilder b = new JsonBuilder();
      Exp p = new Exp("kv","(?<key>\\w+)=(?<value>\\w+)").setType("value", ValueType.String).setMerge("value",ValueMerge.Add);
      p.apply(factory.apply("age=23"),b,null);
      p.apply(factory.apply("age=23"),b,null);

      Assert.assertEquals("<value> should use string concat rather than list append", "2323", b.getRoot().getString("value"));
   }
   @Test
   public void valueList(){
      JsonBuilder b = new JsonBuilder();
      Exp p = new Exp("kv","(?<key>\\w+)=(?<value>\\w+)").setMerge("value", ValueMerge.List);
      p.apply(factory.apply("age=23"),b,null);
      p.apply(factory.apply("age=23"),b,null);

      Assert.assertTrue("<key> should be treated as a list by default",b.getRoot().get("key") instanceof Json);
      Assert.assertTrue("<value> should be treated as a list",b.getRoot().get("value") instanceof Json);
   }

   @Test
   public void eatMatch(){
      DropString line = factory.apply("age=23, age=24, age=26");
      JsonBuilder b = new JsonBuilder();
      Exp p = new Exp("kv","(?<key>\\w+)=(?<value>\\w+)").eat(Eat.Match);
      p.apply(line, b, null);
      Assert.assertEquals("should remove the matched string", ", age=24, age=26", line.toString());
   }

   @Test
   public void eatToMatch(){
      DropString line = factory.apply("foo=1 bar=1 foo=2");
      JsonBuilder b = new JsonBuilder();
      Exp p = new Exp("bar","bar=(?<bar>\\S+)").eat(Eat.ToMatch);
      p.apply(line,b,null);
      Assert.assertEquals("should remove first foo"," foo=2",line.toString());
   }
   @Test
   public void eatWidth(){
      DropString line = factory.apply("age=23, age=24, age=26");
      JsonBuilder b = new JsonBuilder();
      Exp p = new Exp("kv","(?<key>\\w+)=(?<value>\\w+)").eat(8);
      p.apply(line,b,null);
      Assert.assertEquals("should remove the matched string","age=24, age=26",line.toString());
   }
   @Test
   public void eatLine(){
      DropString line = factory.apply("age=23, age=24, age=26");
      JsonBuilder b = new JsonBuilder();
      Exp p = new Exp("kv","(?<key>\\w+)=(?<value>\\w+)").eat(Eat.Line);
      p.apply(line,b,null);
      Assert.assertEquals("should remove the matched string","",line.toString());
   }
   @Test
   public void lineStart(){
      JsonBuilder b = new JsonBuilder();
      Exp p = new Exp("kv", "(?<key>\\w+)=(?<value>\\w+)")
         .add( new Exp("-","(?<sk>\\w+)\\-(?<sv>\\w+)").setRange(MatchRange.EntireLine) );

      p.apply(factory.apply("a-b key=value foo-bar"),b,null);
      Assert.assertEquals("should match first <sk>-<sv> not the last","a",b.getRoot().getString("sk"));
   }
   @Test
   public void repeat_orig(){
      JsonBuilder b = new JsonBuilder();
      Exp p = new Exp("num","(?<num>\\d+)").addRule(ExpRule.Repeat);
      p.apply(factory.apply("1 2 3 4"),b,null);
      Assert.assertEquals("num should be an array with 4 elements",4,b.getRoot().getJson("num").size());
   }

   @Test
   public void pushTarget(){
      JsonBuilder b = new JsonBuilder();
      Exp p = new Exp("num","(?<num>\\d+)").group("pushed").addRule(ExpRule.PushTarget).setMerge(ExpMerge.AsEntry);
      p.apply(factory.apply(" 1 "), b, null);
      p.apply(factory.apply(" 2 "), b, null);
      p.apply(factory.apply(" 3 "), b, null);

      Assert.assertTrue("context should not equal root",b.getRoot()!=b.getTarget());
   }

   @Test
   public void postPopTarget(){
      JsonBuilder b = new JsonBuilder();
      Json lost = new Json();
      lost.set("lost","lost");
      b.pushTarget(lost);
      Exp p = new Exp("num","(?<num>\\d+)").group("pushed").addRule(ExpRule.PostPopTarget);
      p.apply(factory.apply(" 1 "),b,null);
      Assert.assertTrue("context should not equal starting context", lost != b.getTarget());
      Assert.assertTrue("fields should be applied to context before pop",lost.has("pushed"));
   }
   @Test
   public void prePopTarget(){
      JsonBuilder b = new JsonBuilder();
      Json lost = new Json();
      lost.set("lost","lost");
      b.pushTarget(lost);
      Exp p = new Exp("num","(?<num>\\d+)").group("pushed").addRule(ExpRule.PrePopTarget);
      p.apply(factory.apply(" 1 "), b, null);
      Assert.assertTrue("context should not equal starting context",lost!=b.getTarget());
      Assert.assertTrue("fields should be applied to context before pop", b.getTarget().has("pushed"));
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
      Exp p = new Exp("num","(?<num>\\d+)").group("pushed").addRule(ExpRule.PostClearTarget);
      p.apply(factory.apply(" 1 "),b,null);
      Assert.assertTrue("context should not equal starting context",b.getRoot()==b.getTarget());
      Assert.assertTrue("fields should be applied to context before pop", second.has("pushed"));
   }
   @Test
   public void newStart(){
      JsonBuilder b = new JsonBuilder();
      Exp p = new Exp("num","(?<num>\\d+)").addRule(ExpRule.PreClose);
      p.apply(factory.apply(" 1 "),b,null);
      p.apply(factory.apply(" 2 "), b, null);
      Assert.assertEquals("matches should not be combined", 2, b.getRoot().getLong("num"));
      Assert.assertTrue("previous match should be moved to a closed root",b.wasClosed());
   }
   @Test
   public void extend_entry(){
      JsonBuilder b = new JsonBuilder();
      Exp p = new Exp("num","(?<num>\\d+)").extend("nums").setMerge(ExpMerge.AsEntry);
      p.apply(factory.apply(" 1 "),b,null);
      p.apply(factory.apply(" 2 "), b, null);
      Assert.assertEquals("nums should have 2 entries",2,b.getRoot().getJson("nums").size());
   }

   @Test
   public void group_entry(){
      JsonBuilder b = new JsonBuilder();
      Exp p = new Exp("num","(?<num>\\d+)").group("nums").setMerge(ExpMerge.AsEntry);
      p.apply(factory.apply(" 1 "),b,null);
      p.apply(factory.apply(" 2 "), b, null);

      Assert.assertEquals("nums should have 2 entries",2,b.getRoot().getJson("nums").size());

   }
   @Test
   public void group_extend(){
      JsonBuilder b = new JsonBuilder();
      Exp p = new Exp("kv", "(?<key>\\w+)=(?<value>\\w+)").setKeyValue("key","value").group("kv").setMerge(ExpMerge.Extend);
      p.apply(factory.apply(" age=23 "),b,null);
      p.apply(factory.apply(" size=small "),b,null);

      Assert.assertEquals("<kv> should be and array of length 1",1,b.getRoot().getJson("kv").size());

   }

   @Test
   public void collection(){
      JsonBuilder b = new JsonBuilder();
      Exp p = new Exp("kv", "(?<key>\\w+)=(?<value>\\w+)").setKeyValue("key","value").group("kv").setMerge(ExpMerge.ByKey);
      p.apply(factory.apply(" age=23 "),b,null);
      p.apply(factory.apply(" size=small "),b,null);

      Assert.assertTrue("<kv> should have kv.size\n"+b.getRoot().toString(2),b.getRoot().getJson("kv").has("size"));
      Assert.assertTrue("<kv> should have kv.age\n"+b.getRoot().toString(2),b.getRoot().getJson("kv").has("age"));
   }

   @Test
   public void childOrder(){
      JsonBuilder b = new JsonBuilder();
      Exp p = new Exp("start","\\[").eat(Eat.Match)
         .add(new Exp("quoted","^\\s*,?\\s*\"(?<value>[^\"]+)\"")
            .group("child").setMerge(ExpMerge.AsEntry).eat(Eat.Match)
         )
         .add(new Exp("normal","^\\s*,?\\s*(?<value>[^,\\]]*[^\\s,\\]])")
            .group("child").setMerge(ExpMerge.AsEntry).eat(Eat.Match)
         )
         .addRule(ExpRule.RepeatChildren);

      p.apply(factory.apply("[ aa, \"bb,bb]bb\" ,cc]"),b,null);

      Json json = b.getRoot();

      Assert.assertTrue("match should contain a child entry",json.has("child"));
      Assert.assertEquals("child should contain 3 entires\n"+json.toString(2),3,json.getJson("child").size());

   }
}
