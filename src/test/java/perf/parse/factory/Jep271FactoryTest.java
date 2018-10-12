package perf.parse.factory;

import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import perf.parse.Exp;
import perf.parse.Parser;
import perf.yaup.Sets;
import perf.yaup.json.Json;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class Jep271FactoryTest {
    private static Jep271Factory f;

    @BeforeClass
    public static void staticInit(){
        f = new Jep271Factory();
    }

    @Test
    public void newPaarser_serial_tags(){
        Parser p = f.newParser();
        p.onLine("[2018-04-18T09:07:19.417-0500][0.186s][info][gc,cpu] GC(0) User=0.01s Sys=0.01s Real=0.01s");
        Json root = p.getBuilder().getRoot();
        System.out.println(root.toString(2));
    }

    @Test
    public void newParser_printGc_parallel(){
        Parser p = f.newParser();
        p.onLine("1.479: [GC (Allocation Failure)  3932160K->38671K(11927552K), 0.0364259 secs]");
        Json root = p.getBuilder().getRoot();
        assertTrue("root should be empty",root.isEmpty());

    }

    @Test
    public void newParser_parallel_missing_uptime_due_to_exp_order(){
        Parser p = f.newParser();
        p.onLine("[1089.845s][info][gc] GC(676) Pause Young (Allocation Failure) 10461M->5607M(12152M) 57.141ms");
        p.onLine("[1093.185s][info][gc] GC(677) Pause Young (Allocation Failure) 10456M->6076M(11810M) 384.097ms");
        Json root = p.getBuilder().getRoot();
        System.out.println(root.toString(2));
        assertTrue("has uptime",root.has("uptime"));
        assertEquals("uptime = 1093.185",1093.185,root.getDouble("uptime"),0.00000001);
    }

    @Test
    public void newParser_prefixes(){//tests that prefixes are parsed in correct order and hostname doesn't interfere with tags
        Parser p = f.newParser();
        p.onLine("[2018-04-18T09:07:15.744-0500][2018-04-18T14:07:15.744+0000][0.007s][1524060435744ms][7ms][741352031658ns][7051559ns][hostname][12994][12995][info][gc] Using Serial");
        Json root = p.getBuilder().getRoot();
        assertEquals("uptimeMillis",7,root.getLong("uptimeMillis"));
        assertEquals("level","info",root.getString("level"));
        assertEquals("utcTime","2018-04-18T14:07:15.744+0000",root.getString("utcTime"));
        assertEquals("uptimeNanos",741352031658L,root.getLong("uptimeNanos"));
        assertEquals("time","2018-04-18T09:07:15.744-0500",root.getString("time"));
        assertEquals("gc","Serial",root.getString("gc"));
        assertEquals("timeMillis",1524060435744L,root.getLong("timeMillis"));
        assertEquals("uptime",0.007,root.getDouble("uptime"),0.00000001);
        Json tags = new Json();
        tags.add("gc");
        assertEquals("tags",tags,root.getJson("tags"));
    }
    @Test
    public void newParser_gcAge_fullTable(){
        Parser p = f.newParser();
        p.onLine("[2018-04-18T09:07:22.988-0500][0.279s][debug][gc,age] GC(4) Desired survivor size 4358144 bytes, new threshold 15 (max threshold 15)");
        p.onLine("[2018-04-18T09:07:22.988-0500][0.279s][trace][gc,age] GC(4) Age table with threshold 15 (max threshold 15)");
        p.onLine("[2018-04-18T09:07:22.988-0500][0.279s][trace][gc,age] GC(4) - age   1:         56 bytes,         56 total");
        p.onLine("[2018-04-18T09:07:22.988-0500][0.279s][trace][gc,age] GC(4) - age   2:         24 bytes,         80 total");

        Json root = p.getBuilder().getRoot();
        assertEquals("level","trace",root.getString("level"));
        assertEquals("gcId",4,root.getLong("gcId"));
        assertEquals("uptime",0.279,root.getDouble("uptime"),0.00000001);
        assertEquals("time","2018-04-18T09:07:22.988-0500",root.getString("time"));

        assertTrue("has tags",root.has("tags") && root.get("tags") instanceof Json);
        assertTrue("tags=[gc,age]",root.getJson("tags").keys().size()==2 && root.getJson("tags").values().containsAll(Sets.of("gc","age")));

        assertEquals("survivorSize",4358144,root.getLong("survivorSize"));
        assertEquals("threshold",15,root.getLong("threshold"));
        assertEquals("maxThreshold",15,root.getLong("maxThreshold"));

        //table
        assertEquals("tableThreshold",15,root.getLong("tableThreshold"));
        assertEquals("tableMaxThreshold",15,root.getLong("tableMaxThreshold"));

        assertTrue("has table",root.has("table") && root.get("table") instanceof Json);
        Json table = root.getJson("table");
        assertEquals("table.1.age",1,table.getJson("1").getLong("age"));
        assertEquals("table.1.size",56,table.getJson("1").getLong("size"));
        assertEquals("table.1.total",56,table.getJson("1").getLong("total"));
        assertEquals("table.2.age",2,table.getJson("2").getLong("age"));
        assertEquals("table.2.size",24,table.getJson("2").getLong("size"));
        assertEquals("table.2.total",80,table.getJson("2").getLong("total"));

    }
    @Test
    public void newParser_g1_gc_safepoint(){//safepoint treated as sum, phases list, and safepoint doesn't break up gcId's targetId
        Parser p = f.newParser();
        p.setState("gc-g1",true);
        p.onLine("[0.147s][info][gc       ] GC(1) Concurrent Cycle");
        p.onLine("[0.148s][info][safepoint] Application time: 0.0007689 seconds");
        p.onLine("[0.151s][info][gc       ] GC(1) Pause Remark 40M->40M(250M) 1.125ms");
        p.onLine("[0.151s][info][safepoint] Total time for which application threads were stopped: 0.0027370 seconds, Stopping threads took: 0.0015790 seconds");
        p.onLine("[0.151s][info][safepoint] Application time: 0.0000921 seconds");
        p.onLine("[0.153s][info][gc       ] GC(1) Pause Cleanup 40M->40M(250M) 0.131ms");
        p.onLine("[0.153s][info][safepoint] Total time for which application threads were stopped: 0.0020267 seconds, Stopping threads took: 0.0018751 seconds");
        p.onLine("[0.154s][info][gc       ] GC(1) Concurrent Cycle 7.309ms");

        Json root = p.getBuilder().getRoot();

        assertEquals("level","info",root.getString("level"));
        assertEquals("gcId",1,root.getLong("gcId"));
        assertEquals("uptime",0.147,root.getDouble("uptime"),0.00000001);

        assertTrue("has tags",root.has("tags") && root.get("tags") instanceof Json);
        assertTrue("tags=[gc,safepoint]",root.getJson("tags").keys().size()==2 && root.getJson("tags").values().containsAll(Sets.of("gc","safepoint")));

        assertTrue("safepoint", root.has("safepoint") && root.get("safepoint") instanceof Json);
        Json safepoint = root.getJson("safepoint");
        assertEquals("Application time sum",(0.0007689+0.0000921),safepoint.getDouble("applicationSeconds"),0.00000001);
        assertEquals("Threads stopped",(0.0027370 + 0.0020267),safepoint.getDouble("stoppedSeconds"),0.00000001);
        assertEquals("Stopping time",(0.0015790 + 0.0018751),safepoint.getDouble("quiesceSeconds"),0.00000001);

        assertTrue("phases",root.has("phases") && root.get("phases") instanceof Json && root.getJson("phases").isArray());
        Json phases = root.getJson("phases");
        assertEquals("phases count",3,phases.size());

        assertEquals("phase[0].phase","Pause Remark",phases.getJson(0).getString("phase"));
        assertEquals("phase[0].milliseconds",1.125,phases.getJson(0).getDouble("milliseconds"),0.00000001);
        assertEquals("phase[0].before",Exp.parseKMG("40M"),phases.getJson(0).getLong("before"));
        assertEquals("phase[0].after",Exp.parseKMG("40M"),phases.getJson(0).getLong("after"));
        assertEquals("phase[0].capacity",Exp.parseKMG("250M"),phases.getJson(0).getLong("capacity"));

        assertEquals("phase[1].phase","Pause Cleanup",phases.getJson(1).getString("phase"));
        assertEquals("phase[1].milliseconds",0.131,phases.getJson(1).getDouble("milliseconds"),0.00000001);
        assertEquals("phase[1].before",Exp.parseKMG("40M"),phases.getJson(1).getLong("before"));
        assertEquals("phase[1].after",Exp.parseKMG("40M"),phases.getJson(1).getLong("after"));
        assertEquals("phase[1].capacity",Exp.parseKMG("250M"),phases.getJson(1).getLong("capacity"));

        assertEquals("phase[2].phase","Concurrent Cycle",phases.getJson(2).getString("phase"));
        assertEquals("phase[2].milliseconds",7.309,phases.getJson(2).getDouble("milliseconds"),0.00000001);
    }

    @Test @Ignore
    public void newParser_serial_gc_heap(){
        Parser p = f.newParser();
        p.onLine("[2018-04-18T09:07:26.359-0500][0.181s][debug][gc,heap] GC(0) Heap before GC invocations=0 (full 0):");
        p.onLine("[2018-04-18T09:07:26.359-0500][0.181s][debug][gc,heap] GC(0)  def new generation   total 76800K, used 63962K [0x00000006c7200000, 0x00000006cc550000, 0x000000071a150000)");
        p.onLine("[2018-04-18T09:07:26.359-0500][0.181s][debug][gc,heap] GC(0)   eden space 68288K,  93% used [0x00000006c7200000, 0x00000006cb076880, 0x00000006cb4b0000)");
        p.onLine("[2018-04-18T09:07:26.359-0500][0.181s][debug][gc,heap] GC(0)   from space 8512K,   0% used [0x00000006cb4b0000, 0x00000006cb4b0000, 0x00000006cbd00000)");
        p.onLine("[2018-04-18T09:07:26.359-0500][0.181s][debug][gc,heap] GC(0)   to   space 8512K,   0% used [0x00000006cbd00000, 0x00000006cbd00000, 0x00000006cc550000)");
        p.onLine("[2018-04-18T09:07:26.359-0500][0.181s][debug][gc,heap] GC(0)  tenured generation   total 170688K, used 0K [0x000000071a150000, 0x0000000724800000, 0x00000007c0000000)");
        p.onLine("[2018-04-18T09:07:26.359-0500][0.181s][debug][gc,heap] GC(0)    the space 170688K,   0% used [0x000000071a150000, 0x000000071a150000, 0x000000071a150200, 0x0000000724800000)");
        p.onLine("[2018-04-18T09:07:26.359-0500][0.181s][debug][gc,heap] GC(0)  Metaspace       used 4990K, capacity 5086K, committed 5376K, reserved 1056768K");
        p.onLine("[2018-04-18T09:07:26.359-0500][0.181s][debug][gc,heap] GC(0)   class space    used 428K, capacity 458K, committed 512K, reserved 1048576K");
        p.onLine("[2018-04-18T09:07:26.370-0500][0.193s][info ][gc,heap] GC(0) DefNew: 63962K->5938K(76800K)");
        p.onLine("[2018-04-18T09:07:26.370-0500][0.193s][info ][gc,heap] GC(0) Tenured: 0K->9983K(170688K)");
        p.onLine("[2018-04-18T09:07:26.370-0500][0.193s][debug][gc,heap] GC(0) Heap after GC invocations=1 (full 0):");
        p.onLine("[2018-04-18T09:07:26.370-0500][0.193s][debug][gc,heap] GC(0)  def new generation   total 76800K, used 5938K [0x00000006c7200000, 0x00000006cc550000, 0x000000071a150000)");
        p.onLine("[2018-04-18T09:07:26.370-0500][0.193s][debug][gc,heap] GC(0)   eden space 68288K,   0% used [0x00000006c7200000, 0x00000006c7200000, 0x00000006cb4b0000)");
        p.onLine("[2018-04-18T09:07:26.370-0500][0.193s][debug][gc,heap] GC(0)   from space 8512K,  69% used [0x00000006cbd00000, 0x00000006cc2ccbf0, 0x00000006cc550000)");
        p.onLine("[2018-04-18T09:07:26.370-0500][0.193s][debug][gc,heap] GC(0)   to   space 8512K,   0% used [0x00000006cb4b0000, 0x00000006cb4b0000, 0x00000006cbd00000)");
        p.onLine("[2018-04-18T09:07:26.370-0500][0.193s][debug][gc,heap] GC(0)  tenured generation   total 170688K, used 9983K [0x000000071a150000, 0x0000000724800000, 0x00000007c0000000)");
        p.onLine("[2018-04-18T09:07:26.370-0500][0.193s][debug][gc,heap] GC(0)    the space 170688K,   5% used [0x000000071a150000, 0x000000071ab0ffc8, 0x000000071ab10000, 0x0000000724800000)");
        p.onLine("[2018-04-18T09:07:26.370-0500][0.193s][debug][gc,heap] GC(0)  Metaspace       used 4990K, capacity 5086K, committed 5376K, reserved 1056768K");
        p.onLine("[2018-04-18T09:07:26.370-0500][0.193s][debug][gc,heap] GC(0)   class space    used 428K, capacity 458K, committed 512K, reserved 1048576K");

        Json root = p.getBuilder().getRoot();
        System.out.println(root.toString(2));


    }

    @Test @Ignore
    public void newParser_serial_gc_heap_oracle10_46(){
        Parser p = f.newParser();
        p.onLine("[2018-04-18T09:09:52.803-0500][1.318s][debug][gc,heap] GC(18) Heap before GC invocations=12 (full 6): def new generation   total 360064K, used 320064K [0x00000006c7200000, 0x00000006df8b0000, 0x000000071a150000)");
        p.onLine("[2018-04-18T09:09:52.803-0500][1.318s][debug][gc,heap] GC(18)   eden space 320064K, 100% used [0x00000006c7200000, 0x00000006daa90000, 0x00000006daa90000)");
        p.onLine("[2018-04-18T09:09:52.803-0500][1.318s][debug][gc,heap] GC(18)   from space 40000K,   0% used [0x00000006daa90000, 0x00000006daa90000, 0x00000006dd1a0000)");
        p.onLine("[2018-04-18T09:09:52.803-0500][1.318s][debug][gc,heap] GC(18)   to   space 40000K,   0% used [0x00000006dd1a0000, 0x00000006dd1a0000, 0x00000006df8b0000)");
        p.onLine("[2018-04-18T09:09:52.803-0500][1.318s][debug][gc,heap] GC(18)  tenured generation   total 1439048K, used 1119017K [0x000000071a150000, 0x0000000771ea2000, 0x00000007c0000000)");
        p.onLine("[2018-04-18T09:09:52.803-0500][1.318s][debug][gc,heap] GC(18)    the space 1439048K,  77% used [0x000000071a150000, 0x000000075e61a568, 0x000000075e61a600, 0x0000000771ea2000)");
        p.onLine("[2018-04-18T09:09:52.803-0500][1.318s][debug][gc,heap] GC(18)  Metaspace       used 4769K, capacity 4862K, committed 5120K, reserved 1056768K");
        p.onLine("[2018-04-18T09:09:52.803-0500][1.318s][debug][gc,heap] GC(18)   class space    used 397K, capacity 426K, committed 512K, reserved 1048576K");
        p.onLine("[2018-04-18T09:09:52.949-0500][1.464s][info ][gc,heap] GC(18) DefNew: 320064K->0K(360064K)");
        p.onLine("[2018-04-18T09:09:52.949-0500][1.464s][info ][gc,heap] GC(18) Tenured: 1119017K->1438505K(1439048K)");
        p.onLine("[2018-04-18T09:09:52.949-0500][1.464s][debug][gc,heap] GC(18) Heap after GC invocations=13 (full 6): def new generation   total 360064K, used 0K [0x00000006c7200000, 0x00000006df8b0000, 0x000000071a150000)");
        p.onLine("[2018-04-18T09:09:52.949-0500][1.464s][debug][gc,heap] GC(18)   eden space 320064K,   0% used [0x00000006c7200000, 0x00000006c7200000, 0x00000006daa90000)");
        p.onLine("[2018-04-18T09:09:52.949-0500][1.464s][debug][gc,heap] GC(18)   from space 40000K,   0% used [0x00000006dd1a0000, 0x00000006dd1a0050, 0x00000006df8b0000)");
        p.onLine("[2018-04-18T09:09:52.949-0500][1.464s][debug][gc,heap] GC(18)   to   space 40000K,   0% used [0x00000006daa90000, 0x00000006daa90000, 0x00000006dd1a0000)");
        p.onLine("[2018-04-18T09:09:52.949-0500][1.464s][debug][gc,heap] GC(18)  tenured generation   total 1439048K, used 1438505K [0x000000071a150000, 0x0000000771ea2000, 0x00000007c0000000)");
        p.onLine("[2018-04-18T09:09:52.949-0500][1.464s][debug][gc,heap] GC(18)    the space 1439048K,  99% used [0x000000071a150000, 0x0000000771e1a558, 0x0000000771e1a600, 0x0000000771ea2000)");
        p.onLine("[2018-04-18T09:09:52.949-0500][1.464s][debug][gc,heap] GC(18)  Metaspace       used 4769K, capacity 4862K, committed 5120K, reserved 1056768K");
        p.onLine("[2018-04-18T09:09:52.949-0500][1.464s][debug][gc,heap] GC(18)   class space    used 397K, capacity 426K, committed 512K, reserved 1048576K");
        Json root = p.getBuilder().getRoot();
        System.out.println(root.toString(2));
    }

    @Test
    public void usingCms(){
        Json root;
        root = f.usingCms().apply("[0.008s][info ][gc] Using Concurrent Mark Sweep");
        assertEquals("gc","Concurrent Mark Sweep",root.getString("gc"));
    }
    @Test
    public void usingParallel(){
        Json root;
        root = f.usingParallel().apply("[0.007s][info][gc] Using Parallel");
        assertEquals("gc","Parallel",root.getString("gc"));
    }
    @Test
    public void usingG1(){
        Json root;
        root = f.usingG1().apply("[0.010s][info ][gc] Using G1");
        assertEquals("gc","G1",root.getString("gc"));
    }

    @Test
    public void usingSerial(){
        Json root;
        root = f.usingSerial().apply("[0.008s][info][gc] Using Serial");
        assertEquals("gc","Serial",root.getString("gc"));
    }

    @Test
    public void gcPause(){
        Json root;
        root = f.gcPause().apply("Pause Young (Allocation Failure) 62M->15M(241M) 9.238ms");
        assertEquals("reason","Allocation Failure",root.getString("reason"));
        assertEquals("usedBefore",Exp.parseKMG("62M"),root.getLong("usedBefore"));
        assertEquals("usedAfter", Exp.parseKMG("15M"),root.getLong("usedAfter"));
        assertEquals("capacity",Exp.parseKMG("241M"),root.getLong("capacity"));
        assertEquals("milliseconds",9.238,root.getDouble("milliseconds"),0.00000001);
    }
    @Test
    public void gcTags(){
        Json root;
        root = f.gcTags().apply("[gc]");
        assertTrue("tags is array",root.has("tags") && root.getJson("tags").isArray());
        assertEquals("tags[0]","gc",root.getJson("tags").getString(0));

        root = f.gcTags().apply("[gc,cpu      ]");
        assertTrue("tags is array",root.has("tags") && root.getJson("tags").isArray());
        assertEquals("2 tags",2,root.getJson("tags").size());
        assertEquals("tags[0]","gc",root.getJson("tags").getString(0));
        assertEquals("tags[1]","cpu",root.getJson("tags").getString(1));
    }

    @Test
    public void gcResize(){
        Json root;
        root = f.gcResize().apply("61852K->15323K(247488K)");

        assertEquals("usedBefore",Exp.parseKMG("61852K"),root.getLong("usedBefore"));
        assertEquals("usedAfter",Exp.parseKMG("15323K"),root.getLong("usedAfter"));
        assertEquals("capacity",Exp.parseKMG("247488K"),root.getLong("capacity"));
    }
    @Test
    public void gcLevel(){
        Json root;
        root = f.gcLevel().apply("[info ]");
        assertEquals("info","info",root.getString("level"));

        root = f.gcLevel().apply("[trace]");
        assertEquals("trace","trace",root.getString("level"));
    }

    @Test @Ignore
    public void gcKeyValue(){

    }

    @Test
    public void parallelSizeChanged(){
        Json root;
        root = f.parallelSizeChanged().apply("PSYoung generation size changed: 1358848K->1356800K");
        assertTrue("resize",root.has("resize") && !root.getJson("resize").isArray());
        root = root.getJson("resize");
        assertEquals("region","PSYoung",root.getString("region"));
        assertEquals("before",Exp.parseKMG("1358848K"),root.getLong("before"));
        assertEquals("after",Exp.parseKMG("1356800K"),root.getLong("after"));
    }

    @Test
    public void g1MarkStack(){
        Json root;
        root = f.g1MarkStack().apply("MarkStackSize: 4096k  MarkStackSizeMax: 524288k");
        assertTrue("markStack",root.has("markStack") && root.get("markStack") instanceof Json);
        root = root.getJson("markStack");
        assertEquals("size\n"+root.toString(2),Exp.parseKMG("4096k"),root.getLong("size"));
        assertEquals("max\n"+root.toString(2),Exp.parseKMG("524288k"),root.getLong("max"));
    }

    @Test
    public void g1ResizePhase(){
        Json root;
        root = f.g1ResizePhase().apply("Pause Remark 40M->40M(250M) 1.611ms");

        assertTrue("phase\n"+root.toString(2),root.has("phases") && root.get("phases") instanceof Json);
        root = root.getJson("phases");
        assertEquals("phase","Pause Remark",root.getString("phase"));
        assertEquals("milliseconds",1.611,root.getDouble("milliseconds"),0.00000001);
        assertEquals("before",Exp.parseKMG("40M"),root.getLong("before"));
        assertEquals("after",Exp.parseKMG("40M"),root.getLong("after"));
        assertEquals("capacity",Exp.parseKMG("250M"),root.getLong("capacity"));

    }
    @Test
    public void g1TimedPhase(){
        Json root;
        root = f.g1TimedPhase().apply("Finalize Live Data 0.000ms");
        assertTrue("phases\n"+root.toString(2),root.has("phases") && root.get("phases") instanceof Json);
        root = root.getJson("phases");
        assertEquals("phase","Finalize Live Data",root.getString("phase"));
        assertEquals("milliseconds",0,root.getDouble("milliseconds"),0.00000001);
    }

    @Test
    public void gcCpu(){
        Json root;
        root = f.gcCpu().apply("User=0.02s Sys=0.01s Real=0.02s");
        assertTrue("cpu\n"+root.toString(2),root.has("cpu") && root.get("cpu") instanceof Json);
        root = root.getJson("cpu");
        assertEquals("user",0.02,root.getDouble("user"),0.00000001);
        assertEquals("sys",0.01,root.getDouble("sys"),0.00000001);
        assertEquals("real",0.02,root.getDouble("real"),0.00000001);

    }
    @Test
    public void gcHeapSize(){
        Json root;

        root = f.gcHeapSize().apply("Maximum heap size 4173353984");
        assertTrue("heap",root.has("heap") && root.get("heap") instanceof Json);
        assertEquals("heap.Maximum",4173353984l,root.getJson("heap").getLong("Maximum"));

        root = f.gcHeapSize().apply("Initial heap size 260834624");
        assertTrue("heap",root.has("heap") && root.get("heap") instanceof Json);
        assertEquals("heap.Initial",260834624,root.getJson("heap").getLong("Initial"));

        root = f.gcHeapSize().apply("Minimum heap size 6815736");
        assertTrue("heap",root.has("heap") && root.get("heap") instanceof Json);
        assertEquals("heap.Minimum",6815736,root.getJson("heap").getLong("Minimum"));
    }

    @Test
    public void gcHeapRange(){
        Json root;

        root = f.gcHeapRange().apply("Minimum heap 8388608  Initial heap 262144000  Maximum heap 4175429632");
        assertTrue("heap",root.has("heap") && root.get("heap") instanceof Json);
        assertEquals("heap.min",8388608,root.getJson("heap").getLong("min"));
        assertEquals("heap.initial",262144000,root.getJson("heap").getLong("initial"));
        assertEquals("heap.max",4175429632l,root.getJson("heap").getLong("max"));
    }

    @Test
    public void gcHeapYoungRange(){
        Json root;
        root = f.gcHeapYoungRange().apply("1: Minimum young 196608  Initial young 87359488  Maximum young 1391788032");

        assertTrue("heap",root.has("heap") && root.get("heap") instanceof Json);
        root = root.getJson("heap");
        assertTrue("young",root.has("young") && root.get("young") instanceof Json);
        assertEquals("young.min",196608,root.getJson("young").getLong("min"));
        assertEquals("young.initial",87359488,root.getJson("young").getLong("initial"));
        assertEquals("young.max",1391788032,root.getJson("young").getLong("max"));

    }

    @Test
    public void gcHeapOldRange(){
        Json root;
        root = f.gcHeapOldRange().apply("Minimum old 65536  Initial old 174784512  Maximum old 2783641600");

        assertTrue("heap",root.has("heap") && root.get("heap") instanceof Json);
        root = root.getJson("heap");
        assertTrue("old",root.has("old") && root.get("old") instanceof Json);
        assertEquals("old.min",65536,root.getJson("old").getLong("min"));
        assertEquals("old.initial",174784512,root.getJson("old").getLong("initial"));
        assertEquals("old.max",2783641600l,root.getJson("old").getLong("max"));

    }

    @Test
    public void gcHeapHeader(){
        Json root;

        root = f.gcHeapHeader().apply("Heap before GC invocations=0 (full 0): ");
        assertTrue("heap",root.has("heap") && root.get("heap") instanceof Json);
        root = root.getJson("heap");
        assertTrue("before",root.has("before") && root.get("before") instanceof Json);
        root = root.getJson("before");
        assertEquals("phase","before",root.getString("phase"));
        assertEquals("invocations",0,root.getLong("invocations"));
        assertEquals("full",0,root.getLong("full"));

        root = f.gcHeapHeader().apply("Heap after GC invocations=1 (full 0): ");
        assertTrue("heap",root.has("heap") && root.get("heap") instanceof Json);
        root = root.getJson("heap");
        assertTrue("after",root.has("after") && root.get("after") instanceof Json);
        root = root.getJson("after");
        assertEquals("phase","after",root.getString("phase"));
        assertEquals("invocations",1,root.getLong("invocations"));
        assertEquals("full",0,root.getLong("full"));
    }

    @Test
    public void gcHeapRegion(){
        Json root;
        root = f.gcHeapRegion().apply(" def new generation   total 76800K, used 63648K [0x00000006c7200000, 0x00000006cc550000, 0x000000071a150000)");
        assertTrue("region\n"+root.toString(2),root.has("region") && root.get("region") instanceof Json);
        root = root.getJson("region");

        assertTrue("region.isArray > 0\n"+root.toString(2),root.isArray() && root.size()>0);
        root = root.getJson(0);

        assertEquals("name","def new generation",root.getString("name"));
        assertEquals("total",Exp.parseKMG("76800K"),root.getLong("total"));
        assertEquals("used",Exp.parseKMG("63648K"),root.getLong("used"));
        assertEquals("start","0x00000006c7200000",root.getString("start"));
        assertEquals("current","0x00000006cc550000",root.getString("current"));
        assertEquals("end","0x000000071a150000",root.getString("end"));

        root = f.gcHeapRegion().apply("garbage-first heap   total 256000K, used 110592K [0x00000006c7200000, 0x00000006c73007d0, 0x00000007c0000000)");
        //TODO verify
    }

    @Test @Ignore
    public void gcHeapRegionG1(){
        Json root;

        root = f.gcHeapRegionG1().apply("");

        System.out.println(root.toString(2));
    }

    @Test
    public void gcHeapMetaRegion(){
        Json root;

        root = f.gcHeapMetaRegion().apply(" Metaspace       used 4769K, capacity 4862K, committed 5120K, reserved 1056768K");
        assertTrue("region\n"+root.toString(2),root.has("region") && root.get("region") instanceof Json);
        root = root.getJson("region");

        assertTrue("region.isArray > 0\n"+root.toString(2),root.isArray() && root.size()>0);
        root = root.getJson(0);
        assertEquals("region","Metaspace",root.getString("name"));
        assertEquals("committed",Exp.parseKMG("5120K"),root.getLong("committed"));
        assertEquals("reserved",Exp.parseKMG("1056768K"),root.getLong("reserved"));
        assertEquals("used",Exp.parseKMG("4769K"),root.getLong("used"));
        assertEquals("capacity",Exp.parseKMG("4862K"),root.getLong("capacity"));

    }

    @Test
    public void gcHeapRegionResize(){
        Json root;
        root = f.gcHeapRegionResize().apply("ParOldGen: 145286K->185222K(210944K)");

        assertTrue("ParOldGen",root.has("ParOldGen") && root.get("ParOldGen") instanceof Json);
        root = root.getJson("ParOldGen");
        assertEquals("region","ParOldGen",root.getString("region"));
        assertEquals("size",Exp.parseKMG("210944K"),root.getLong("size"));
        assertEquals("before",Exp.parseKMG("145286K"),root.getLong("before"));
        assertEquals("after",Exp.parseKMG("185222K"),root.getLong("after"));

    }
    @Test
    public void gcHeapRegionResizeG1(){
        Json root;
        root = f.gcHeapRegionResizeG1().apply("Eden regions: 4->0(149)");
        assertTrue("Eden",root.has("Eden") && root.get("Eden") instanceof Json);
        root = root.getJson("Eden");
        assertEquals("region","Eden",root.getString("region"));
        assertEquals("before",4,root.getLong("before"));
        assertEquals("after",0,root.getLong("after"));
        assertEquals("total",149,root.getLong("total"));
    }

    @Test
    public void gcHeapRegionResizeG1UsedWaste(){
        Json root;
        root = f.gcHeapRegionResizeG1UsedWaste().apply(" Used: 20480K, Waste: 0K");

        assertEquals("used",Exp.parseKMG("20480K"),root.getLong("used"));
        assertEquals("waste",Exp.parseKMG("0K"),root.getLong("waste"));
    }

    @Test
    public void gcHeapMetaSpace(){
        Json root;
        root = f.gcHeapMetaSpace().apply("  class space    used 388K, capacity 390K, committed 512K, reserved 1048576K");
        assertEquals("space","class",root.getString("space"));
        assertEquals("committed",Exp.parseKMG("512K"),root.getLong("committed"));
        assertEquals("reserved",Exp.parseKMG("1048576K"),root.getLong("reserved"));
        assertEquals("used",Exp.parseKMG("388K"),root.getLong("used"));
        assertEquals("capcaity",Exp.parseKMG("390K"),root.getLong("capacity"));
    }

    @Test
    public void gcHeapSpace(){
        Json root;
        root = f.gcHeapSpace().apply("   eden space 68288K,  93% used [0x00000006c7200000, 0x00000006cb076880, 0x00000006cb4b0000)");

        assertEquals("space","eden",root.getString("space"));
        assertEquals("size",Exp.parseKMG("68288K"),root.getLong("size"));
        assertEquals("used",93,root.getLong("used"));
        assertEquals("start","0x00000006c7200000",root.getString("start"));
        assertEquals("end","0x00000006cb4b0000",root.getString("end"));
        assertEquals("current","0x00000006cb076880",root.getString("current"));

    }

    @Test
    public void gcHeapSpaceG1(){
        Json root;
        root = f.gcHeapSpaceG1().apply("   region size 1024K, 5 young (5120K), 0 survivors (0K)");

        assertEquals("regionSize",Exp.parseKMG("1024K"),root.getLong("regionSize"));
        assertEquals("youngCount",5,root.getLong("youngCount"));
        assertEquals("youngSize",Exp.parseKMG("5120K"),root.getLong("youngSize"));
        assertEquals("survivorCount",0,root.getLong("survivorCount"));
        assertEquals("survivorSize",Exp.parseKMG("0K"),root.getLong("survivorSize"));
    }

    @Test
    public void safepointStopTime(){
        Json root;
        root = f.safepointStopTime().apply("Total time for which application threads were stopped: 0.0019746 seconds, Stopping threads took: 0.0000102 seconds");
        assertTrue("safepoint\n"+root.toString(2),root.has("safepoint") && root.get("safepoint") instanceof Json);
        root = root.getJson("safepoint");
        assertEquals("stoppedSeconds",0.0019746,root.getDouble("stoppedSeconds"),0.00000001);
        assertEquals("quiesceSeconds",0.0000102,root.getDouble("quiesceSeconds"),0.00000001);
    }

    @Test
    public void safepointAppTime(){
        Json root;
        root = f.safepointAppTime().apply("Application time: 0.0009972 seconds");
        assertTrue("safepoint\n"+root.toString(2),root.has("safepoint") && root.get("safepoint") instanceof Json);
        root = root.getJson("safepoint");
        assertEquals("applicationSeconds",0.0009972,root.getDouble("applicationSeconds"),0.00000001);

    }
    @Test
    public void gcClassHistoStart(){
        Json root;
        root = f.gcClassHistoStart().apply("Class Histogram (before full gc)");
        assertEquals("phase","before",root.getString("phase"));

        root = f.gcClassHistoStart().apply("Class Histogram (after full gc)");
        assertEquals("phase","after",root.getString("phase"));
    }
    @Test
    public void gcClassHistoEntry(){
        Json root;
        root = f.gcClassHistoEntry().apply("    1:          2709     1963112296  [B (java.base@10)");
        assertTrue("histo\n"+root.toString(2),root.has("histo") && root.get("histo") instanceof Json);
        root = root.getJson("histo");
        assertTrue("isArray",root.isArray());
        root = root.getJson(0);
        assertEquals("num",1,root.getLong("num"));
        assertEquals("count",2709,root.getLong("count"));
        assertEquals("bytes",1963112296,root.getLong("bytes"));
        assertEquals("name","[B (java.base@10)",root.getString("name"));
    }

    @Test
    public void gcClassHistoTotal(){
        Json root;
        root = f.gcClassHistoTotal().apply("Total         14175     1963663064");
        assertTrue("total\n"+root.toString(2),root.has("total") && root.get("total") instanceof Json);
        root = root.getJson("total");
        assertEquals("count",14175,root.getLong("count"));
        assertEquals("bytes",1963663064,root.getLong("bytes"));
    }
    @Test
    public void gcClassHistoEnd(){
        Json root;
        root = f.gcClassHistoEnd().apply("Class Histogram (before full gc) 18.000ms");
        assertEquals("phase","before",root.getString("phase"));
        assertEquals("milliseconds",18.000,root.getDouble("milliseconds"),0.00000001);
    }
    @Test
    public void gcId(){
        Json root;
        root = f.gcId().apply("GC(27)");
        assertEquals("gcId\n"+root.toString(2),27,root.getLong("gcId"));
    }
    @Test
    public void time(){
        Json root = f.time().apply("[2018-04-12T09:24:30.397-0500]");
        assertEquals("time","2018-04-12T09:24:30.397-0500",root.getString("time"));
    }
    @Test
    public void utcTime(){
        Json root = f.utcTime().apply("[2018-04-12T14:24:30.397+0000]");
        assertEquals("utcTime\n"+root.toString(2),"2018-04-12T14:24:30.397+0000",root.getString("utcTime"));
    }
    @Test
    public void uptime(){
        Json root = f.uptime().apply("[0.179s]");
        assertEquals("uptime",0.179,root.getDouble("uptime"),0.00000001);
    }
    @Test
    public void timeMillis(){
        Json root = f.timeMillis().apply("[1523543070397ms]");
        assertEquals("timeMillis",1523543070397L,root.getLong("timeMillis"));
    }
    @Test
    public void uptimeMillis(){
        Json root = f.uptimeMillis().apply("[15ms]");
        assertEquals("uptimeMillis",15,root.getLong("uptimeMillis"));
    }
    @Test
    public void timeNanos(){
        Json root = f.timeNanos().apply("[6267442276019ns]");
        assertEquals("timeNanos",6267442276019L,root.getLong("timeNanos"));
    }
    @Test
    public void uptimeNanos(){
        Json root = f.uptimeNanos().apply("[10192976ns]");
        assertEquals("uptimeNanos\n"+root.toString(2),10192976,root.getLong("uptimeNanos"));
    }

    @Test
    public void gcExpanding(){
        Json root = f.gcExpanding().apply("Expanding tenured generation from 170688K by 39936K to 210624K");
        assertEquals("region","tenured generation",root.getString("region"));
        assertEquals("from",Exp.parseKMG("170688K"),root.getLong("from"));
        assertEquals("by",Exp.parseKMG("39936K"),root.getLong("by"));
        assertEquals("to",Exp.parseKMG("210624K"),root.getLong("to"));
    }
    @Test
    public void gcShrinking(){
        Json root = f.gcShrinking().apply("Shrinking tenured generation from 880164K to 720420K");
        assertEquals("region","tenured generation",root.getString("region"));
        assertEquals("from",Exp.parseKMG("880164K"),root.getLong("from"));
        assertEquals("to",Exp.parseKMG("720420K"),root.getLong("to"));

        root = f.gcShrinking().apply("Shrinking ParOldGen from 319488K by 56832K to 262656K");
        assertEquals("region","ParOldGen",root.getString("region"));
        assertEquals("from",Exp.parseKMG("319488K"),root.getLong("from"));
        assertEquals("by",Exp.parseKMG("56832K"),root.getLong("by"));
        assertEquals("to",Exp.parseKMG("262656K"),root.getLong("to"));

    }
    @Test
    public void gcAge(){
        Json root = f.gcAge().apply("Desired survivor size 4358144 bytes, new threshold 1 (max threshold 6)");
        assertEquals("survivorSize",Exp.parseKMG("4358144"),root.getLong("survivorSize"));
        assertEquals("threshold",1,root.getLong("threshold"));
        assertEquals("maxThreshold",6,root.getLong("maxThreshold"));
    }

    @Test
    public void gcAgeTableHeader(){
        Json root = f.gcAgeTableHeader().apply("Age table with threshold 1 (max threshold 6)");
        assertEquals("tableThreshold\n"+root.toString(2),1,root.getLong("tableThreshold"));
        assertEquals("tableMaxThreshold",6,root.getLong("tableMaxThreshold"));
    }

    @Test
    public void gcAgeTableEntry(){
        Json root = f.gcAgeTableEntry().apply("- age   1:    6081448 bytes,    6081448 total");

        assertTrue("table\n"+root.toString(2),root.has("table") && root.get("table") instanceof Json);
        root = root.getJson("table");
        assertTrue("1\n"+root.toString(2),root.has("1") && root.get("1") instanceof Json);
        root = root.getJson("1");
        assertEquals("age",1,root.getLong("age"));
        assertEquals("size",6081448,root.getLong("size"));
        assertEquals("total",6081448,root.getLong("total"));

    }

}
