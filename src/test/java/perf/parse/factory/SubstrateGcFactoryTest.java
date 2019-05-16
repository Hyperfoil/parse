package perf.parse.factory;

import org.junit.BeforeClass;
import org.junit.Test;
import perf.parse.Parser;
import perf.yaup.StringUtil;
import perf.yaup.json.Json;

import static org.junit.Assert.*;

public class SubstrateGcFactoryTest {

   private static SubstrateGcFactory f;

   @BeforeClass
   public static void staticInit(){
      f = new SubstrateGcFactory();
   }


   @Test
   public void newParser_printGc(){
      Parser p = f.newParser();
      Json json;
      json = p.onLine("[Incremental GC (CollectOnAllocation.Sometimes) 261108K->1019K, 0.0055645 secs]");
      assertEquals("first line should not emit",null,json);
      json = p.getBuilder().getRoot();
      assertEquals("cause\n"+json.toString(2),"CollectOnAllocation.Sometimes",json.getString("cause"));
      assertEquals("before\n"+json.toString(2), StringUtil.parseKMG("261108K"),json.getLong("before"));
      assertEquals("after\n"+json.toString(2),StringUtil.parseKMG("1019K"),json.getLong("after"));
      assertEquals("seconds\n"+ json.toString(2), 0.0055645,json.getDouble("seconds",0.0),0.000001);
      json = p.onLine("[Incremental GC (CollectOnAllocation.Sometimes) 261108K->1019K, 0.0062683 secs]");
      assertFalse("second line should close previous\n"+json,json==null);
      json = p.getBuilder().getRoot();
      //verifies it was closed
      assertEquals("cause\n"+json.toString(2),"CollectOnAllocation.Sometimes",json.getString("cause"));
      assertEquals("before\n"+json.toString(2), StringUtil.parseKMG("261108K"),json.getLong("before"));
      assertEquals("after\n"+json.toString(2),StringUtil.parseKMG("1019K"),json.getLong("after"));
      assertEquals("seconds\n"+ json.toString(2), 0.0062683,json.getDouble("seconds",0.0),0.000001);
   }
   @Test
   public void newParser_verboseGc(){
      Parser p = f.newParser();
      Json json;
      p.onLine("[Heap policy parameters:");
      p.onLine("  YoungGenerationSize: 268435456");
      p.onLine("  LargeArrayThreshold: 131072]");
      p.onLine("[[105503979854228 GC: before  epoch: 1  cause: CollectOnAllocation.Sometimes]");
      p.onLine(" [105503985983529 GC: after   epoch: 1  cause: CollectOnAllocation.Sometimes  policy: by space and time: 50% in incremental collections  type: incremental");
      json = p.onLine("  collection time: 6119125 nanoSeconds]]");
      assertFalse("json should emit after collection time",json==null);
      //json = p.getBuilder().getRoot();

      assertTrue("nanoSeconds\n"+json.toString(2),json.has("nanoSeconds"));
      assertTrue("parameters\n"+json.toString(2),json.has("parameters"));
      assertEquals("parameters count",2,json.getJson("parameters",new Json()).size());
      assertTrue("phase\n"+json.toString(2),json.has("phase"));
      assertEquals("phase count",2,json.getJson("phase",new Json()).size());

      p.onLine("[[105511888991426 GC: before  epoch: 2  cause: CollectOnAllocation.Sometimes]");
      p.onLine(" [105511895880370 GC: after   epoch: 2  cause: CollectOnAllocation.Sometimes  policy: by space and time: 50% in incremental collections  type: incremental");
      json = p.onLine("  collection time: 6871815 nanoSeconds]]");

   }

   @Test
   public void newParser_verboseGc_printGc(){
      Parser p = f.newParser();
      Json json;
      p.onLine("[Heap policy parameters:");
      p.onLine("  YoungGenerationSize: 268435456");
      p.onLine("  LargeArrayThreshold: 131072]");
      p.onLine("[[105820269449889 GC: before  epoch: 1  cause: CollectOnAllocation.Sometimes]");
      p.onLine("[Incremental GC (CollectOnAllocation.Sometimes) 261108K->1019K, 0.0062683 secs]");
      p.onLine(" [105820275730890 GC: after   epoch: 1  cause: CollectOnAllocation.Sometimes  policy: by space and time: 50% in incremental collections  type: incremental");
      json = p.onLine("  collection time: 6268316 nanoSeconds]]");
      assertNotNull("json should not be null",json);

      //verboseGc
      assertTrue("nanoSeconds\n"+json.toString(2),json.has("nanoSeconds"));
      assertTrue("parameters\n"+json.toString(2),json.has("parameters"));
      assertEquals("parameters count",2,json.getJson("parameters",new Json()).size());
      assertTrue("phase\n"+json.toString(2),json.has("phase"));
      assertEquals("phase count",2,json.getJson("phase",new Json()).size());
      //printGc
      assertTrue("cause\n"+json.toString(2),json.has("cause"));
      assertTrue("before\n"+json.toString(2),json.has("before"));
      assertTrue("after\n"+json.toString(2),json.has("after"));
      assertTrue("seconds\n"+json.toString(2),json.has("seconds"));

      p.onLine("[[105828483633311 GC: before  epoch: 2  cause: CollectOnAllocation.Sometimes]");
      p.onLine("[Incremental GC (CollectOnAllocation.Sometimes) 262127K->1019K, 0.0044387 secs]");
      p.onLine(" [105828488119905 GC: after   epoch: 2  cause: CollectOnAllocation.Sometimes  policy: by space and time: 50% in incremental collections  type: incremental");
      json = p.onLine("  collection time: 4438734 nanoSeconds]]");

      assertNotNull("json should not be null for 2nd event",json);
      assertTrue("nanoSeconds\n"+json.toString(2),json.has("nanoSeconds"));
      assertTrue("phase\n"+json.toString(2),json.has("phase"));
      assertEquals("phase count",2,json.getJson("phase",new Json()).size());
      //printGc
      assertTrue("cause\n"+json.toString(2),json.has("cause"));
      assertTrue("before\n"+json.toString(2),json.has("before"));
      assertTrue("after\n"+json.toString(2),json.has("after"));
      assertTrue("seconds\n"+json.toString(2),json.has("seconds"));

   }

   @Test
   public void incrementalGc(){
      Json json = f.gc().apply("[Incremental GC (CollectOnAllocation.Sometimes) 261108K->1019K, 0.0055645 secs]");
      assertEquals("cause\n"+json.toString(2),"CollectOnAllocation.Sometimes",json.getString("cause"));
      assertEquals("before\n"+json.toString(2), StringUtil.parseKMG("261108K"),json.getLong("before"));
      assertEquals("after\n"+json.toString(2),StringUtil.parseKMG("1019K"),json.getLong("after"));
      assertEquals("seconds\n"+ json.toString(2), 0.0055645,json.getDouble("seconds",0.0),0.000001);
   }

   @Test
   public void policyParametersHeader(){
      assertEquals("match header",true,f.policyParametersHeader().test("[Heap policy parameters:"));
   }

   @Test
   public void policyParameter_size(){
      Json json = f.policyParameter().apply("      MaximumHeapSize: 13353028800");
      assertEquals("policy\n"+json.toString(2),"MaximumHeapSize",json.getString("policy"));
      assertEquals("value\n"+json.toString(2),13353028800l,json.getLong("value"));
   }
   @Test
   public void policyParameter_threshold(){
      Json json = f.policyParameter().apply("  LargeArrayThreshold: 131072]");
      assertEquals("policy\n"+json.toString(2),"LargeArrayThreshold",json.getString("policy"));
      assertEquals("value\n"+json.toString(2),131072,json.getLong("value"));
   }

   @Test
   public void gcPhase_before(){
      Json json = f.gcPhase().apply("[[105503979854228 GC: before  epoch: 1  cause: CollectOnAllocation.Sometimes]");
      assertEquals("timestamp\n"+json.toString(2),105503979854228l,json.getLong("timestamp"));
      assertEquals("phase\n"+json.toString(2),"before",json.getString("phase"));
      assertEquals("epoch\n"+json.toString(2),1,json.getLong("epoch"));
      assertEquals("cause\n"+json.toString(2),"CollectOnAllocation.Sometimes",json.getString("cause"));
   }
   @Test
   public void gcPhase_after(){
      Json json = f.gcPhase().apply(" [105503985983529 GC: after   epoch: 1  cause: CollectOnAllocation.Sometimes  policy: by space and time: 50% in incremental collections  type: incremental");
      assertEquals("timestamp\n"+json.toString(2),105503985983529l,json.getLong("timestamp"));
      assertEquals("phase\n"+json.toString(2),"after",json.getString("phase"));
      assertEquals("epoch\n"+json.toString(2),1,json.getLong("epoch"));
      assertEquals("cause\n"+json.toString(2),"CollectOnAllocation.Sometimes",json.getString("cause"));
      assertEquals("policy\n"+json.toString(2),"by space and time: 50% in incremental collections",json.getString("policy"));
      assertEquals("type\n"+json.toString(2),"incremental",json.getString("type"));
   }

}
