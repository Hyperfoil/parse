package perf.parse.factory;

import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import perf.parse.MatchRule;
import perf.parse.Parser;
import perf.parse.reader.TextLineReader;
import perf.yaup.Sets;
import perf.yaup.StringUtil;
import perf.yaup.json.Json;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PrintGcFactoryTest {

    private static PrintGcFactory f;

    @BeforeClass
    public static void staticInit(){
        f = new PrintGcFactory();
    }

    @Test
    public void newParser_cms_gcId_sharedLine(){
        //TODO should two gcId on one line be split into multiple events
        //if gcId isn't present would we split on timestamp / datestamp?
        Parser p = f.newParser();
        p.onLine("2018-05-10T16:05:40.790+0000: 40458.534: #1532: [GC (Allocation Failure) 2018-05-10T16:05:40.790+0000: 40458.534: #1532: [ParNew (promotion failed): 7549760K->7345445K(7549760K), 2.4428745 secs]2018-05-10T16:05:43.233+0000: 40460.977: #1533: [CMS: 21790658K->14261353K(25165824K), 43.3354939 secs] 28741107K->14261353K(32715584K), [Metaspace: 140350K->140350K(165888K)], 45.7791953 secs] [Times: user=56.91 sys=1.21, real=45.78 secs]");

    }



    @Test
    public void newParser_cms_g1TagBug(){
        Parser p = f.newParser();
        p.onLine("2018-05-10T04:51:30.496+0000: 8.240: #1: [GC (CMS Final Remark) [YG occupancy: 5888335 K (7549760 K)]2018-05-10T04:51:30.504+0000: 8.248: #1: [Rescan (parallel) , 0.1135815 secs]2018-05-10T04:51:30.618+0000: 8.361: #1: [weak refs processing, 0.0000518 secs]2018-05-10T04:51:30.618+0000: 8.361: #1: [class unloading, 0.0114563 secs]2018-05-10T04:51:30.629+0000: 8.373: #1: [scrub symbol table, 0.0104777 secs]2018-05-10T04:51:30.640+0000: 8.383: #1: [scrub string table, 0.0011170 secs][1 CMS-remark: 0K(25165824K)] 5888335K(32715584K), 0.1463148 secs] [Times: user=3.00 sys=0.08, real=0.14 secs]");
        Json root = p.getBuilder().getRoot();

        assertFalse("root.g1Tag",root.has("g1Tag"));
        System.out.println(root.toString(2));
    }

    @Test
    public void newParser_cms_ParNew_tenuringDistribution_4(){
        Parser p = f.newParser();
        p.onLine("2018-05-10T19:38:41.521+0000: 53239.265: #1762: [GC (Allocation Failure) 2018-05-10T19:38:41.521+0000: 53239.265: #1762: [ParNew");
        p.onLine("Desired survivor size 429490176 bytes, new threshold 6 (max 6)");
        p.onLine("- age   1:   58663952 bytes,   58663952 total");
        p.onLine("- age   2:   66262912 bytes,  124926864 total");
        p.onLine("- age   3:  102218008 bytes,  227144872 total");
        p.onLine("- age   4:   85139528 bytes,  312284400 total");
        p.onLine("- age   5:   79381384 bytes,  391665784 total");
        p.onLine(": 7425651K->528962K(7549760K), 0.2153945 secs] 25414567K->18609465K(32715584K), 0.2157151 secs] [Times: user=3.83 sys=0.10, real=0.22 secs]");
        Json root = p.getBuilder().getRoot();
        System.out.println(root.toString(2));
    }

    @Test
    public void newParser_cms_ParNew_tenuringDistribution(){
        TextLineReader reader = new TextLineReader();
        Parser p = new Parser();

        p.add((json)->{
            System.out.println(json.toString(2));
        });

        f.addToParser(p);
        reader.addParser(p);
        reader.onLine("2018-05-10T04:51:23.342+0000: 1.085: #0: [GC (Allocation Failure) 2018-05-10T04:51:23.342+0000: 1.086: #0: [ParNew");
        reader.onLine("Desired survivor size 429490176 bytes, new threshold 6 (max 6)");
        reader.onLine("- age   1:   14050056 bytes,   14050056 total");
        reader.onLine(": 6710912K->13897K(7549760K), 0.0369830 secs] 6710912K->13897K(32715584K), 0.0371120 secs] [Times: user=0.51 sys=0.02, real=0.04 secs]");
        Json root = p.getBuilder().getRoot();
        System.out.println(root.toString(2));

        assertTrue("root.times",root.has("times") && root.get("times") instanceof Json);

        assertEquals("root.reason","Allocation Failure",root.getString("reason"));
        assertEquals("root.datestamp","2018-05-10T04:51:23.342+0000",root.getString("datestamp"));
        assertEquals("root.timestamp",1.085,root.getDouble("timestamp"),0.00000001);
        assertEquals("root.gcId",0,root.getLong("gcId"));
        assertEquals("root.before\n"+root.toString(2), StringUtil.parseKMG("6710912K"),root.getLong("before"));
        assertEquals("root.after", StringUtil.parseKMG("13897K"),root.getLong("after"));
        assertEquals("root.capacity", StringUtil.parseKMG("32715584K"),root.getLong("capacity"));
        assertTrue("root has region",root.has("region") && root.get("region") instanceof Json);
        Json region = root.getJson("region");
        assertEquals("region.size",1,region.size());

        Json region0 = region.getJson(0);
        assertEquals("region[0].region","ParNew",region0.getString("region"));
        assertEquals("region[0].before", StringUtil.parseKMG("6710912K"),region0.getLong("before"));
        assertEquals("region[0].after", StringUtil.parseKMG("13897K"),region0.getLong("after"));
        assertEquals("region[0].capacity", StringUtil.parseKMG("7549760K"),region0.getLong("capacity"));

        assertTrue("root.tenuring",root.has("tenuring") && root.get("tenuring") instanceof Json);

        Json tenuring = root.getJson("tenuring");



    }

    @Test
    public void newParser_cms_promotionFailed(){
        Parser p = f.newParser();
        p.onLine("2018-05-09T17:15:43.215+0000: 81035.353: #6409: [GC (Allocation Failure) 2018-05-09T17:15:43.215+0000: 81035.353: #6409: [ParNew (promotion failed): 1980203K->2146944K(2146944K), 1.4796333 secs]2018-05-09T17:15:44.694+0000: 81036.833: #6410: [CMS: 20835733K->8291039K(22780352K), 32.9419816 secs] 22178277K->8291039K(24927296K), [Metaspace: 146810K->146810K(1212416K)], 34.4220682 secs] [Times: user=49.09 sys=0.17, real=34.43 secs]");
        Json root = p.getBuilder().getRoot();

        assertTrue("has region",root.has("region") && root.get("region") instanceof Json);

        Json region = root.getJson("region");
        assertEquals("3 regions:\n"+region.toString(2),3,region.size());

        Set<String> names = region.values().stream().map(s->((Json)s).getString("region")).collect(Collectors.toSet());
        assertTrue("names: "+names,names.containsAll(Sets.of("ParNew","CMS","Metaspace")));

        Json parNew = region.values().stream()
            .map(s->(Json)s)
            .filter(s->"ParNew".equals(s.getString("region")))
            .findFirst()
            .orElse(new Json());
        assertEquals("parNew warning","promotion failed",parNew.getString("warning"));
    }

    @Test
    public void newParser_shenandoah_details_live(){
        Parser p = f.newParser();
        p.onLine("2.372: #5: [Pause Final MarkTotal Garbage: 1M");
        p.onLine("Immediate Garbage: 0M, 0 regions (0% of total)");
        p.onLine("Garbage to be collected: 1M (100% of total), 2 regions");
        p.onLine("Live objects to be evacuated: 0M");
        p.onLine("Live/garbage ratio in collected regions: 18%");
        p.onLine(" 2497M->2499M(3981M), 0.800 ms]");
        p.onLine("Cancelling concurrent GC: Allocation Failure");
        p.onLine("Concurrent marking triggered. Free: 1481M, Free Threshold: 1791M; Allocated: 1481M, Alloc Threshold: 0M");
        p.onLine("Adjusting free threshold to: 65% (2587M)");
        Json root = p.getBuilder().getRoot();
        System.out.println(root.toString(2));
    }

    @Test
    public void newParser_shenandoah_details_soloResize(){
        Parser p = f.newParser();
        p.onLine("1.405: #0: [Pause Final MarkTotal Garbage: 6M");
        p.onLine("Immediate Garbage: 1723M, 1725 regions (99% of total)");
        p.onLine(" 2809M->1094M(3981M), 0.445 ms]");
        Json root = p.getBuilder().getRoot();
        System.out.println(root.toString(2));
    }

    @Test
    public void newParser_shenandoah_gcStatistics(){
        Parser p = f.newParser();
        p.add((json)->{
            System.out.println(json.toString(2));
        });
        Json root;
        p.onLine("Total Pauses (G)            =     0.08 s (a =     3338 us) (n =    24) (lvls, us =      568,      932,     1426,     3086,    19809)");
        p.onLine("Total Pauses (N)            =     0.07 s (a =     2734 us) (n =    24) (lvls, us =       99,      133,      963,     2285,    19332)");
        p.onLine("Pause Init Mark (G)         =     0.03 s (a =     4302 us) (n =     6) (lvls, us =     2266,     2266,     3789,     4102,     7850)");
        p.onLine("Pause Init Mark (N)         =     0.02 s (a =     3434 us) (n =     6) (lvls, us =     1465,     1465,     2832,     3008,     7384)");
        p.onLine("  Accumulate Stats          =     0.00 s (a =       48 us) (n =     6) (lvls, us =       45,       45,       47,       48,       53)");
        p.onLine("  Make Parsable             =     0.00 s (a =       31 us) (n =     6) (lvls, us =       29,       29,       30,       30,       35)");
        p.onLine("  Clear Liveness            =     0.00 s (a =       80 us) (n =     6) (lvls, us =       64,       64,       83,       84,       86)");
        p.onLine("  Scan Roots                =     0.02 s (a =     3208 us) (n =     6) (lvls, us =     1250,     1250,     2598,     2793,     7172)");
        p.onLine("    S: Thread Roots         =     0.00 s (a =      799 us) (n =     6) (lvls, us =      486,      486,      576,      693,     1682)");
//        p.onLine("    S: String Table Roots   =     0.00 s (a =      803 us) (n =     6) (lvls, us =        0,        0,      652,      730,     2045)");
//        p.onLine("    S: Universe Roots       =     0.00 s (a =        2 us) (n =     6) (lvls, us =        1,        1,        2,        2,        3)");
//        p.onLine("    S: JNI Roots            =     0.00 s (a =        6 us) (n =     6) (lvls, us =        4,        4,        5,        5,       12)");
//        p.onLine("    S: JNI Weak Roots       =     0.00 s (a =       13 us) (n =     6) (lvls, us =        0,        0,       12,       13,       31)");
//        p.onLine("    S: Synchronizer Roots   =     0.00 s (a =        6 us) (n =     6) (lvls, us =        2,        2,        6,        6,       11)");
//        p.onLine("    S: Flat Profiler Roots  =     0.00 s (a =        7 us) (n =     6) (lvls, us =        5,        5,        7,        7,       12)");
//        p.onLine("    S: Management Roots     =     0.00 s (a =        2 us) (n =     6) (lvls, us =        2,        2,        2,        2,        4)");
//        p.onLine("    S: System Dict Roots    =     0.00 s (a =       91 us) (n =     6) (lvls, us =       16,       16,       18,       26,      437)");
//        p.onLine("    S: CLDG Roots           =     0.00 s (a =      514 us) (n =     6) (lvls, us =       31,       31,      631,      721,      861)");
//        p.onLine("    S: JVMTI Roots          =     0.00 s (a =        2 us) (n =     6) (lvls, us =        1,        1,        1,        1,        3)");
        p.onLine("  Resize TLABs              =     0.00 s (a =       54 us) (n =     6) (lvls, us =       39,       39,       55,       56,       60)");
        p.onLine("Pause Final Mark (G)        =     0.04 s (a =     7001 us) (n =     6) (lvls, us =     2676,     2676,     2734,     2930,    19804)");

        root = p.getBuilder().getRoot();

        System.out.println(root.toString(2));
    }
    @Test
    public void newParser_shenandoah_gcId(){
        Parser p = f.newParser();
        p.add((json)->{
            System.out.println(json.toString(2));
        });
        Json root;
        p.onLine("2017-11-14T14:38:15.525-0500: 463.254: #0: [Pause Init Mark, 7.438 ms]");
        p.onLine("2017-11-14T14:38:15.532-0500: 463.261: #0: [Concurrent marking 12G->13G(18G), 42.990 ms]");
        p.onLine("2017-11-14T14:38:15.576-0500: 463.305: #0: [Pause Final Mark 13G->9784M(18G), 2.288 ms]");
        p.onLine("2017-11-14T14:38:15.578-0500: 463.307: #0: [Concurrent evacuation 9800M->10G(18G), 14.682 ms]");
        p.onLine("2017-11-14T14:38:15.593-0500: 463.322: #0: [Pause Init Update Refs, 0.125 ms]");
        p.onLine("2017-11-14T14:38:15.593-0500: 463.322: #0: [Concurrent update references  10G->10G(18G), 17.012 ms]");
        p.onLine("2017-11-14T14:38:15.611-0500: 463.340: #0: [Pause Final Update Refs 10G->2416M(18G), 0.991 ms]");
        p.onLine("2017-11-14T14:38:15.612-0500: 463.341: #0: [Concurrent reset bitmaps 2416M->2440M(18G), 0.347 ms]");
        root = p.getBuilder().getRoot();
        System.out.println(root.toString(2));
    }

    @Test
    public void newParser_shenandoah_heap(){//1.8.0_144-b01
        Parser p = f.newParser();
        Json root;
        p.onLine("Heap");
        p.onLine("Shenandoah Heap");
        p.onLine(" 18874368K total, 7313022K used");
        p.onLine(" 8192K regions, 2304 active, 2304 total");
        p.onLine("Status: idle cancelled");
        p.onLine("Reserved region:");
        p.onLine(" - [0x0000000340000000, 0x00000007c0000000)");
        p.onLine("Virtual space: (pinned in memory)");
        p.onLine(" - committed: 19327352832");
        p.onLine(" - reserved:  19327352832");
        p.onLine(" - [low, high]:     [0x0000000340000000, 0x00000007c0000000]");
        p.onLine(" - [low_b, high_b]: [0x0000000340000000, 0x00000007c0000000]");
        p.onLine("");
        root = p.getBuilder().getRoot();
        System.out.println(root.toString(2));
    }

    @Test
    public void newParser_shenandoah_heap_regions(){
        Parser p = f.newParser();
        p.onLine("Heap");
        p.onLine("Shenandoah Heap");
        p.onLine(" 4076544K total, 4076544K committed, 1918975K used");
        p.onLine(" 3981 x 1024K regions");
        p.onLine("Status: idle cancelled");
        p.onLine("Reserved region:");
        p.onLine(" - [0x00000006c7300000, 0x00000007c0000000)");
        Json root = p.getBuilder().getRoot();
        System.out.println(root.toString(2));

    }

    @Test
    public void newParser_g1_gcDetails(){
        Parser p = f.newParser();
        Json root;
        p.onLine("1.138: [GC remark 1.138: [Finalize Marking, 0.0011024 secs] 1.139: [GC ref-proc, 0.0002180 secs] 1.139: [Unloading, 0.0024588 secs], 0.0046151 secs]");
        root = p.getBuilder().getRoot();
        System.out.println(root.toString(2));
    }

    @Test
    public void newParser_serial_gc_prefixed(){
        Parser p = f.newParser();
        Json root;
        p.onLine("2018-04-17T10:42:28.870-0500: 0.199: #5: [Full GC (Allocation Failure)  180052K->50260K(287424K), 0.0084822 secs]");
        root = p.getBuilder().getRoot();
        Set<Object> expectedKeys = Sets.of("reason","datestamp","seconds","capacity","before","gcId","after","type","timestamp");
        assertTrue("expected keys missing:"+Sets.unique(expectedKeys,root.keys())+":\n"+root.toString(2),
            root.keys().containsAll(expectedKeys)
        );
        assertEquals("before 180052K",180052*1024,root.getLong("before"));
        assertEquals("after 50260K",50260*1024,root.getLong("after"));
        assertEquals("capacity 287424K",287424*1024,root.getLong("capacity"));

    }
    @Test
    public void newParser_serial_gcDetails_prefixed(){
        Parser p = f.newParser();
        Json root;
        p.onLine("0.184: [GC (Allocation Failure) 0.184: [DefNew: 41286K->0K(76800K), 0.0232887 secs]0.208: [Tenured: 180052K->180052K(210624K), 0.0050705 secs] 181403K->50260K(287424K), [Metaspace: 2934K->2934K(1056768K)], 0.0287714 secs] [Times: user=0.01 sys=0.01, real=0.03 secs] ");
        root = p.getBuilder().getRoot();
        Set<Object> expectedKeys = Sets.of(
                "reason","times","capacity","before","after","type","timestamp"
        );
        assertTrue("expected keys:"+Sets.unique(expectedKeys,root.keys())+":\n"+root.toString(2),
                root.keys().containsAll(expectedKeys))
        ;

        assertEquals("root before 181403K",181403*1024,root.getLong("before"));
        assertEquals("root after 50260K",50260*1024,root.getLong("after"));
        assertEquals("root capacity 287424K",287424*1024,root.getLong("capacity"));
        assertEquals("root seconds 0.0287714",0.0287714,root.getDouble("seconds"),0.0000001);
    }



    @Test
    public void newParser_serial_gcDetails_heap(){
        Parser p = f.newParser();
        Json root;

        p.onLine("Heap");
        p.onLine(" def new generation   total 1223296K, used 43469K [0x00000006c7200000, 0x000000071a150000, 0x000000071a150000)");
        p.onLine("  eden space 1087424K,   3% used [0x00000006c7200000, 0x00000006c9c73588, 0x00000007097f0000)");
        p.onLine("  from space 135872K,   0% used [0x0000000711ca0000, 0x0000000711ca0000, 0x000000071a150000)");
        p.onLine("  to   space 135872K,   0% used [0x00000007097f0000, 0x00000007097f0000, 0x0000000711ca0000)");
        p.onLine(" tenured generation   total 2718400K, used 1917249K [0x000000071a150000, 0x00000007c0000000, 0x00000007c0000000)");
        p.onLine("   the space 2718400K,  70% used [0x000000071a150000, 0x000000078f1a0538, 0x000000078f1a0600, 0x00000007c0000000)");
        p.onLine(" Metaspace       used 2968K, capacity 4486K, committed 4864K, reserved 1056768K");
        p.onLine("  class space    used 321K, capacity 386K, committed 512K, reserved 1048576K");

        root = p.getBuilder().getRoot();

        assertTrue("has keys "+root.keys(),root.keys().containsAll(Sets.of(
                "tenured generation","def new generation","Metaspace"
        )));
        assertEquals("3 keys "+root.keys(),3,root.keys().size());

    }
    @Test
    public void newParser_serial_heapAtGc_before(){
        Parser p = f.newParser();
        Json root;

        p.onLine("{Heap before GC invocations=0 (full 0):");
        p.onLine(" def new generation   total 76800K, used 61852K [0x00000006c7200000, 0x00000006cc550000, 0x000000071a150000)");
        p.onLine("  eden space 68288K,  90% used [0x00000006c7200000, 0x00000006cae67150, 0x00000006cb4b0000)");
        p.onLine("  from space 8512K,   0% used [0x00000006cb4b0000, 0x00000006cb4b0000, 0x00000006cbd00000)");
        p.onLine("  to   space 8512K,   0% used [0x00000006cbd00000, 0x00000006cbd00000, 0x00000006cc550000)");
        p.onLine(" tenured generation   total 170688K, used 0K [0x000000071a150000, 0x0000000724800000, 0x00000007c0000000)");
        p.onLine("   the space 170688K,   0% used [0x000000071a150000, 0x000000071a150000, 0x000000071a150200, 0x0000000724800000)");
        p.onLine(" Metaspace       used 2934K, capacity 4486K, committed 4864K, reserved 1056768K");
        p.onLine("  class space    used 317K, capacity 386K, committed 512K, reserved 1048576K");

        root = p.getBuilder().getRoot();

        assertTrue("has keys "+root.toString(2),root.keys().containsAll(Sets.of(
                "tenured generation","def new generation","Metaspace", "phase", "gcCount","fullCount"
        )));
        assertEquals("6 keys "+root.keys(),6,root.keys().size());

    }
    @Test
    public void newParser_serial_heapAtGc_after(){
        Parser p = f.newParser();
        Json root;

        p.onLine("Heap after GC invocations=1 (full 0):");
        p.onLine(" def new generation   total 76800K, used 61852K [0x00000006c7200000, 0x00000006cc550000, 0x000000071a150000)");
        p.onLine("  eden space 68288K,  90% used [0x00000006c7200000, 0x00000006cae67150, 0x00000006cb4b0000)");
        p.onLine("  from space 8512K,   0% used [0x00000006cb4b0000, 0x00000006cb4b0000, 0x00000006cbd00000)");
        p.onLine("  to   space 8512K,   0% used [0x00000006cbd00000, 0x00000006cbd00000, 0x00000006cc550000)");
        p.onLine(" tenured generation   total 170688K, used 0K [0x000000071a150000, 0x0000000724800000, 0x00000007c0000000)");
        p.onLine("   the space 170688K,   0% used [0x000000071a150000, 0x000000071a150000, 0x000000071a150200, 0x0000000724800000)");
        p.onLine(" Metaspace       used 2934K, capacity 4486K, committed 4864K, reserved 1056768K");
        p.onLine("  class space    used 317K, capacity 386K, committed 512K, reserved 1048576K");

        root = p.getBuilder().getRoot();

        assertTrue("has keys "+root.keys(),root.keys().containsAll(Sets.of(
                "tenured generation","def new generation","Metaspace", "phase", "gcCount","fullCount"
        )));
        assertEquals("6 keys "+root.keys(),6,root.keys().size());

    }
    @Test
    public void newParser_parallel_gcDetails_fullGc(){
        Parser p = f.newParser();
        Json root;

        p.onLine("3.108: [Full GC (Ergonomics) [PSYoungGen: 639007K->0K(782336K)] [ParOldGen: 2236751K->1278287K(1559040K)] 2875759K->1278287K(2341376K), [Metaspace: 2935K->2935K(1056768K)], 0.1699732 secs] [Times: user=0.53 sys=0.03, real=0.17 secs] ");

        root = p.getBuilder().getRoot();

        Set<Object> expectedKeys = Sets.of(
                "type","reason","times","before","after","capacity","seconds","timestamp"
        );
        assertTrue("missing keys:"+Sets.unique(expectedKeys,root.keys())+":\n"+root.toString(2),root.keys().containsAll(expectedKeys));

    }
    @Test
    public void newParser_parallel_gcDetails_heap(){
        Parser p = f.newParser();
        Json root;
        p.onLine("Heap");
        p.onLine(" PSYoungGen      total 1161216K, used 46295K [0x000000076d100000, 0x00000007b6c80000, 0x00000007c0000000)");
        p.onLine("  eden space 1160704K, 3% used [0x000000076d100000,0x000000076fe35d90,0x00000007b3e80000)");
        p.onLine("  from space 512K, 0% used [0x00000007b6c00000,0x00000007b6c00000,0x00000007b6c80000)");
        p.onLine("  to   space 21504K, 0% used [0x00000007b4280000,0x00000007b4280000,0x00000007b5780000)");
        p.onLine(" ParOldGen       total 2718720K, used 1917249K [0x00000006c7200000, 0x000000076d100000, 0x000000076d100000)");
        p.onLine("  object space 2718720K, 70% used [0x00000006c7200000,0x000000073c250538,0x000000076d100000)");
        p.onLine(" Metaspace       used 2967K, capacity 4486K, committed 4864K, reserved 1056768K");
        p.onLine("  class space    used 321K, capacity 386K, committed 512K, reserved 1048576K");

        root = p.getBuilder().getRoot();

    }
    @Test
    public void newParser_parallel_tenureDistribution_multiLine(){
        Parser p = f.newParser();
        Json root;
        p.onLine("2018-02-27T10:14:44.106-0500: 224.616: [GC (Allocation Failure)");
        p.onLine("Desired survivor size 125829120 bytes, new threshold 1 (max 15)");
        p.onLine("[PSYoungGen: 3948544K->122853K(4071424K)] 5091588K->1382973K(6518784K), 0.0685783 secs] [Times: user=1.48 sys=0.01, real=0.06 secs]");


        root = p.getBuilder().getRoot();

        System.out.println(root.toString(2));
    }
    @Test
    public void newParser_cms_gcDetails_initialMark(){
        Parser p = f.newParser();
        Json root;
        p.onLine("0.247: [GC (CMS Initial Mark) [1 CMS-initial-mark: 140129K(170688K)] 180069K(247488K), 0.0009869 secs] [Times: user=0.00 sys=0.00, real=0.00 secs] ");

        root = p.getBuilder().getRoot();

        System.out.println(root.toString(2));
    }
    @Test
    public void newParser_cms_gcDetails_afExtended(){
        Parser p = f.newParser();
        Json root;
        p.onLine("0.396: [GC (Allocation Failure) 0.396: [ParNew: 81558K->0K(94848K), 0.0076137 secs]0.404: [CMS: 160090K->120147K(210624K), 0.0183360 secs] 161775K->120147K(305472K), [Metaspace: 2935K->2935K(1056768K)], 0.0260186 secs] [Times: user=0.05 sys=0.00, real=0.03 secs] ");

        root = p.getBuilder().getRoot();

        System.out.println(root.toString(2));
    }
    @Test
    public void newParser_cms_gcDetails_finalRemark(){
        Parser p = f.newParser();
        Json root;
        p.onLine("1.229: [GC (CMS Final Remark) [YG occupancy: 0 K (306688 K)]1.229: [Rescan (parallel) , 0.0007956 secs]1.230: [weak refs processing, 0.0000076 secs]1.230: [class unloading, 0.0001711 secs]1.230: [scrub symbol table, 0.0002226 secs]1.231: [scrub string table, 0.0000627 secs][1 CMS-remark: 1118547K(1198652K)] 1118547K(1505340K), 0.0012901 secs] [Times: user=0.00 sys=0.00, real=0.00 secs] ");

        root = p.getBuilder().getRoot();

        System.out.println(root.toString(2));
    }

    @Test
    public void newParser_g1gc_prefix(){
        Parser p = f.newParser();
        Json root;
        p.onLine("0.118: [GC pause (G1 Humongous Allocation) (young) (initial-mark) 98M->19M(250M), 0.0033460 secs]");

        root = p.getBuilder().getRoot();

        Set<Object> expectedKeys = Sets.of(
                "reason","seconds","g1Tag","capacity","before","after","type","timestamp"
        );
        assertTrue("expect keys missing "+Sets.unique(expectedKeys,root.keys())+":\n"+root.toString(2),root.keys().containsAll(expectedKeys));

        assertEquals("reason","G1 Humongous Allocation",root.getString("reason"));
        assertEquals("seconds",0.003346,root.getDouble("seconds"),0.0000001);
        assertEquals("before",98*1024*1024,root.getLong("before"));
        assertEquals("after",19*1024*1024,root.getLong("after"));
        assertEquals("capacity",250*1024*1024,root.getLong("capacity"));
        assertTrue("tags has 2 entires",root.get("g1Tag") instanceof Json && root.getJson("g1Tag").size()==2);

    }

    @Test
    public void newParser_g1gc_phase_ConcMarkEnd(){
        Parser p = f.newParser();
        Json root;
        p.onLine("0.122: [GC concurrent-mark-end, 0.0003460 secs]");

        root = p.getBuilder().getRoot();

        assertEquals("phase\n"+root.toString(2),"concurrent-mark-end",root.getString("phase"));
        assertEquals("seconds\n"+root.toString(2),0.0003460,root.getDouble("seconds"),0.00000001);
        assertEquals("timestamp\n"+root.toString(2),0.122,root.getDouble("timestamp"),0.00000001);
    }
    @Test
    public void newParser_g1gc_phase_remark(){
        Parser p = f.newParser();
        Json root;
        p.onLine("[GC remark, 0.0091266 secs]");

        root = p.getBuilder().getRoot();

        assertEquals("phase","remark",root.getString("phase"));
        assertEquals("seconds",0.0091266,root.getDouble("seconds"),0.00000001);
    }

    @Test
    public void newParser_g1gc_phase_cleanup(){
        Parser p = f.newParser();
        Json root;
        p.onLine("0.140: [GC cleanup 39M->39M(250M), 0.0004707 secs]");

        root = p.getBuilder().getRoot();

        assertEquals("phase","cleanup",root.getString("phase"));
        assertEquals("before",39*1024*1024,root.getLong("before"));
        assertEquals("after",39*1024*1024,root.getLong("after"));
        assertEquals("capacity",250*1024*1024,root.getLong("capacity"));
        assertEquals("seconds",0.0004707,root.getDouble("seconds"),0.00000001);

    }
    @Test
    public void newParser_g1gc_details_finalizeMarking(){
        Parser p = f.newParser();
        Json root;
        p.onLine("2018-02-06T08:45:48.243-0500: 7.726: [GC remark 2018-02-06T08:45:48.243-0500: 7.726: [Finalize Marking, 0.0004068 secs] 2018-02-06T08:45:48.243-0500: 7.727: [GC ref-proc, 0.0030626 secs] 2018-02-06T08:45:48.247-0500: 7.730: [Unloading, 0.0180262 secs], 0.0219189 secs]\n" +
                " [Times: user=0.18 sys=0.21, real=0.02 secs] ");

        root = p.getBuilder().getRoot();

        System.out.println(root.toString(2));
    }


    @Test
    public void newParser_g1gc_details_threshold(){
        Parser p = f.newParser();
        Json root;
        p.onLine("2018-02-06T04:16:44.259-0500: 0.475: [GC pause (G1 Evacuation Pause) (young)");
        p.onLine("Desired survivor size 2097152 bytes, new threshold 15 (max 15)");
        p.onLine(", 0.0096917 secs]");
        p.onLine("   [Parallel Time: 3.2 ms, GC Workers: 23]");
        p.onLine("      [GC Worker Start (ms): Min: 474.7, Avg: 475.6, Max: 477.6, Diff: 2.9]");
        p.onLine("      [Ext Root Scanning (ms): Min: 0.0, Avg: 0.2, Max: 1.4, Diff: 1.4, Add: 4.3]");
        p.onLine("      [Update RS (ms): Min: 0.0, Avg: 0.0, Max: 0.0, Diff: 0.0, Add: 0.0]");
        p.onLine("         [Processed Buffers: Min: 0, Avg: 0.0, Max: 0, Diff: 0, Add: 0]");
        p.onLine("      [Scan RS (ms): Min: 0.0, Avg: 0.0, Max: 0.0, Diff: 0.0, Add: 0.0]");
        p.onLine("      [Code Root Scanning (ms): Min: 0.0, Avg: 0.3, Max: 2.5, Diff: 2.5, Add: 7.5]");
        p.onLine("      [Object Copy (ms): Min: 0.0, Avg: 0.9, Max: 2.0, Diff: 2.0, Add: 19.9]");
        p.onLine("      [Termination (ms): Min: 0.0, Avg: 0.6, Max: 0.9, Diff: 0.9, Add: 14.6]");
        p.onLine("         [Termination Attempts: Min: 1, Avg: 13.7, Max: 34, Diff: 33, Add: 314]");
        p.onLine("      [GC Worker Other (ms): Min: 0.0, Avg: 0.1, Max: 1.7, Diff: 1.7, Add: 2.0]");
        p.onLine("      [GC Worker Total (ms): Min: 0.2, Avg: 2.1, Max: 3.0, Diff: 2.9, Add: 48.4]");
        p.onLine("      [GC Worker End (ms): Min: 477.7, Avg: 477.7, Max: 477.8, Diff: 0.0]");
        p.onLine("   [Code Root Fixup: 0.1 ms]");
        p.onLine("   [Code Root Purge: 0.0 ms]");
        p.onLine("   [Clear CT: 0.1 ms]");
        p.onLine("   [Other: 6.3 ms]");
        p.onLine("      [Choose CSet: 0.0 ms]");
        p.onLine("      [Ref Proc: 5.8 ms]");
        p.onLine("      [Ref Enq: 0.1 ms]");
        p.onLine("      [Redirty Cards: 0.1 ms]");
        p.onLine("      [Humongous Register: 0.0 ms]");
        p.onLine("      [Humongous Reclaim: 0.0 ms]");
        p.onLine("      [Free CSet: 0.1 ms]");
        p.onLine("   [Eden: 24.0M(24.0M)->0.0B(48.0M) Survivors: 0.0B->4096.0K Heap: 24.0M(256.0M)->4202.0K(256.0M)]");
        p.onLine(" [Times: user=0.08 sys=0.02, real=0.01 secs]");

        root = p.getBuilder().getRoot();

        System.out.println(root.toString(2));
    }

    @Test
    public void newParser_g1gc_details_nest(){
        Parser p = f.newParser();
        p.add((json)->{
            System.out.println(json.toString(2));
        });
        Json root;
        p.onLine("0.105: [GC pause (G1 Humongous Allocation) (young) (initial-mark), 0.0026001 secs]");
        p.onLine("   [Parallel Time: 1.6 ms, GC Workers: 4]");
        p.onLine("      [GC Worker Start (ms): Min: 104.7, Avg: 104.8, Max: 105.0, Diff: 0.3]");
        p.onLine("      [Ext Root Scanning (ms): Min: 0.0, Avg: 0.5, Max: 1.4, Diff: 1.4, Add: 2.0]");
        p.onLine("      [Update RS (ms): Min: 0.0, Avg: 0.0, Max: 0.0, Diff: 0.0, Add: 0.0]");
        p.onLine("         [Processed Buffers: Min: 0, Avg: 0.0, Max: 0, Diff: 0, Add: 0]");
        p.onLine("      [Scan RS (ms): Min: 0.0, Avg: 0.0, Max: 0.0, Diff: 0.0, Add: 0.0]");
        p.onLine("      [Code Root Scanning (ms): Min: 0.0, Avg: 0.0, Max: 0.0, Diff: 0.0, Add: 0.0]");
        p.onLine("      [Object Copy (ms): Min: 0.0, Avg: 0.3, Max: 0.4, Diff: 0.4, Add: 1.2]");
        p.onLine("      [Termination (ms): Min: 0.0, Avg: 0.6, Max: 0.8, Diff: 0.8, Add: 2.3]");
        p.onLine("         [Termination Attempts: Min: 1, Avg: 2.5, Max: 4, Diff: 3, Add: 10]");
        p.onLine("      [GC Worker Other (ms): Min: 0.0, Avg: 0.0, Max: 0.0, Diff: 0.0, Add: 0.0]");
        p.onLine("      [GC Worker Total (ms): Min: 1.2, Avg: 1.4, Max: 1.5, Diff: 0.3, Add: 5.6]");
        p.onLine("      [GC Worker End (ms): Min: 106.1, Avg: 106.2, Max: 106.2, Diff: 0.0]");
        p.onLine("   [Code Root Fixup: 0.0 ms]");
        p.onLine("   [Code Root Purge: 0.0 ms]");
        p.onLine("   [Clear CT: 0.0 ms]");
        p.onLine("   [Other: 1.0 ms]");
        p.onLine("      [Choose CSet: 0.0 ms]");
        p.onLine("      [Ref Proc: 0.8 ms]");
        p.onLine("      [Ref Enq: 0.0 ms]");
        p.onLine("      [Redirty Cards: 0.0 ms]");
        p.onLine("      [Humongous Register: 0.0 ms]");
        p.onLine("      [Humongous Reclaim: 0.1 ms]");
        p.onLine("      [Free CSet: 0.0 ms]");
        p.onLine("   [Eden: 4096.0K(14.0M)->0.0B(16.0M) Survivors: 0.0B->1024.0K Heap: 98.7M(250.0M)->20.0M(250.0M)]");
        p.onLine(" [Times: user=0.01 sys=0.00, real=0.00 secs] ");

        root = p.getBuilder().getRoot();

        System.out.println(root.toString(2));

        assertEquals("phase","pause",root.getString("phase"));
        assertEquals("reason","G1 Humongous Allocation",root.getString("reason"));
        assertEquals("seconds",0.0026001,root.getDouble("seconds"),0.00000001);
        assertTrue("g1Tag",root.has("g1Tag") && root.getJson("g1Tag").isArray());
        assertTrue("g1Tag=[young,initial-mark]",root.getJson("g1Tag").values().containsAll(Arrays.asList("young","initial-mark")));
        assertTrue("detail",root.has("detail") && root.getJson("detail").isArray());
        assertEquals("detail count:\n"+root.getJson("detail").values().stream().map(v->{
            if(v instanceof Json){
                return ((Json)v).getString("category");
            }else{
                return v.toString();
            }
        }).collect(Collectors.joining("|,|")),7,root.getJson("detail").size());

        Json detail0 = root.getJson("detail").getJson(0);

        assertEquals("detail[0].millliseconds\n"+detail0.toString(2),1.6,detail0.getDouble("millliseconds"),0.00000001);
        assertEquals("detail[0].worker",4,detail0.getLong("workers"));
        assertTrue("detail[0].detail",detail0.has("detail") && detail0.getJson("detail").isArray());

        Json detail00 = detail0.getJson("detail").getJson(0);
        assertEquals("detail[0][0].category","GC Worker Start (ms)",detail00.getString("category"));
        assertEquals("detail[0][0].min",104.7,detail00.getDouble("min"),0.00000001);
        assertEquals("detail[0][0].avg",104.8,detail00.getDouble("avg"),0.00000001);
        assertEquals("detail[0][0].max",105,detail00.getDouble("max"),0.00000001);
        assertEquals("detail[0][0].diff",0.3,detail00.getDouble("diff"),0.00000001);

        Json detail1 = root.getJson("detail").getJson(1);

        assertEquals("detail[1].category","Code Root Fixup",detail1.getString("category"));
        assertEquals("detail[1].time",0,detail1.getDouble("time"),0.00000001);
        //assertEquals("detail count",);

    }

    @Test
    public void gcShenandoahCancelConcurrent(){
        Json root = f.gcShenandoahCancelConcurrent().apply("Cancelling concurrent GC: Allocation Failure");
        System.out.println(root.toString(2));
    }
    @Test
    public void gcShenandoahResizePhase(){
        Json root = f.gcShenandoahResizePhase().apply("[Concurrent marking 2809M->2809M(3981M), 1.196 ms]");
        System.out.println(root.toString(2));
    }
    @Test
    public void gcShenandoahStatisticsEntry(){
        Json root = f.gcShenandoahStatisticsEntry().apply("Total Pauses (G)            =     1.02 s (a =   102115 us) (n =    10) (lvls, us =    39648,    56836,    88867,    97656,   229139)");
        System.out.println(root.toString(2));
    }
    @Test
    public void gcShenandoahTimedPhase(){
        Json root = f.gcShenandoahTimedPhase().apply("[Pause Init Mark, 0.528 ms]");
        System.out.println(root.toString(2));
    }
    @Test
    public void gcShenandoahTimedPhase_notMatch_resize() {
        Json root = f.gcShenandoahTimedPhase().apply("[Concurrent reset bitmaps 1352M->1407M(3981M), 0.365 ms]");
        assertTrue("should not match",root.isEmpty());
    }
    @Test
    public void gcShenandoahDetailsInlineTotalGarbage(){
        Json root = f.gcShenandoahDetailsInlineTotalGarbage().apply("[Pause Final MarkTotal Garbage: 6M");
        System.out.println(root.toString(2));

        assertFalse("root should not be empty",root.isEmpty());

    }
    @Test
    public void gcShenandoahDetailsInlineAdaptiveCset(){
        Json root = f.gcShenandoahDetailsInlineAdaptiveCset().apply("[Pause Final MarkAdaptive CSet selection: free target = 6451M, actual free = 14664M; min cset = 0M, max cset = 10998M");
        System.out.println(root.toString(2));

        assertFalse("root should not be empty",root.isEmpty());

    }
    @Test
    public void gcShenandoahDetailsHeapTCU(){
        Json root = f.gcShenandoahDetailsHeapTCU().apply(" 18874368K total, 14835712K committed, 713727K used");
        System.out.println(root.toString(2));

        assertFalse("root should not be empty",root.isEmpty());

    }

    @Test
    public void gcShenandoahDetailsHeapVirtualRange(){
        Json root = f.gcShenandoahDetailsHeapVirtualRange()
            .apply(" - [low_b, high_b]: [0x0000000340000000, 0x00000007c0000000]");
        assertFalse("root should not be empty",root.isEmpty());
        assertEquals("root.low_b","0x0000000340000000",root.getString("low_b"));
        assertEquals("root.high_b","0x00000007c0000000",root.getString("high_b"));

    }
    @Test
    public void newParser_gcShenandoahDetailsHeapVirtualRange(){
        Parser p = f.newParser();
        p.setState("printGc-heap-shenandoah",true);
        p.onLine(" - [low_b, high_b]:[0x0000000340000000, 0x00000007c0000000]");
        p.add((j)->{
            System.out.println("Consume: "+j.toString(2));
        });
        p.addUnparsedConsumer((remainder,original,lineNmber)->{
            System.out.println("Unparsed: ||"+remainder+"||");
            System.out.println("  Original:||"+original+"||");
        });
        Json root = p.getBuilder().getRoot();
        System.out.println(root.toString(2));
    }


    @Test
    public void gcShenandoahDetailHeapRegionsActiveTotal(){
        Json root = f.gcShenandoahDetailHeapRegionsActiveTotal().apply(" 8192K regions, 2304 active, 2304 total");
        assertFalse("root should not be empty",root.isEmpty());
    }
    @Test
    public void gcAdaptiveSizePolicyOldGenCosts(){
        Json root = f.gcAdaptiveSizePolicyOldGenCosts()
            .apply("PSAdaptiveSizePolicy::compute_old_gen_free_space: costs minor_time: 0.567664 major_cost: 0.014526 mutator_cost: 0.417810 throughput_goal: 0.990000 live_space: 292574784 free_space: 10737418240 old_promo_size: 4294967296 desired_promo_size: 4294967296");
        assertFalse("root should not be empty",root.isEmpty());

    }
    @Test
    public void gcShenandoahDetailsHeapVirtualCommitted(){
        Json root = f.gcShenandoahDetailsHeapVirtualCommitted()
            .apply(" - committed: 19327352832");
        assertFalse("root should not be empty",root.isEmpty());
    }


    @Test @Ignore
    public void gcMemoryLine(){
        Json root = f.gcMemoryLine().apply("Memory: 4k page, physical 263842984k(199299872k free), swap 4194300k(4194300k free)");
        System.out.println(root.toString(2));
    }
    @Test @Ignore
    public void gcDetailsHeapSpaceG1(){
        Json root = f.gcDetailsHeapSpaceG1().apply("  region size 4096K, 30 young (122880K), 20 survivors (81920K)");
        System.out.println(root.toString(2));
    }

    @Test @Ignore
    public void gcG1TimedStep(){
        Json root = f.gcG1TimedStep().apply("[GC ref-proc, 0.0002180 secs]");
        System.out.println(root.toString(2));

    }

    @Test
    public void gcG1Phase(){//" pause "
        Json root = f.gcG1Phase().apply("pause ");
        assertEquals("phase","pause",root.getString("phase"));
    }

    @Test @Ignore
    public void gcG1DetailsNestHeapResize(){
        Json root = f.gcG1DetailsNestHeapResize().setRule(MatchRule.Repeat).apply("Eden: 1024.0K(16.0M)->0.0B(183.0M) Survivors: 1024.0K->1024.0K Heap: 3666.8M(3682.0M)->936.5M(3682.0M)]");
        System.out.println(root.toString(2));
    }

    @Test
    public void gcDateStamps_minus(){
        Json root =f.gcDateStamps().apply("2018-04-17T10:42:28.747-0500: 0.076: #0: [GC (Allocation Failure)  61852K->15323K(247488K), 0.0066592 secs]");
        assertTrue("has datestamp:"+root.toString(),root.has("datestamp"));
        assertEquals("2018-04-17T10:42:28.747-0500",root.getString("datestamp"));
    }
    @Test
    public void gcDateStamps_plus(){
        Json root =f.gcDateStamps().apply("2018-04-17T10:42:28.747+0500: 0.076: #0: [GC (Allocation Failure)  61852K->15323K(247488K), 0.0066592 secs]");
        assertTrue("has datestamp:"+root.toString(),root.has("datestamp"));
        assertEquals("2018-04-17T10:42:28.747+0500",root.getString("datestamp"));
    }


    @Test @Ignore
    public void gcTenuringAgeDetails(){
        Json root = f.gcTenuringAgeDetails().apply("- age   1:    4606400 bytes,    4606400 total");
        System.out.println(root.toString(2));
    }

    @Test
    public void gcTimeStamps(){
        Parser p = new Parser();
        Json root = f.gcTimestamp().apply("2018-04-17T10:42:28.747-0500: 0.076: #0: [GC (Allocation Failure)  61852K->15323K(247488K), 0.0066592 secs]");
        assertTrue("has timestamp:"+root.toString(),root.has("timestamp"));
        assertEquals(0.076,root.getDouble("timestamp"),0.0001);
    }
    @Test
    public void gcId(){
        Json root =f.gcId().apply("2018-04-17T10:42:28.747-0500: 0.076: #0: [GC (Allocation Failure)  61852K->15323K(247488K), 0.0066592 secs]");
        assertTrue("has gcId:"+root.toString(),root.has("gcId"));
        assertEquals(0,root.getLong("gcId"));
    }
    @Test @Ignore
    public void gcReason_Systemgc(){
        Json root = f.gcReason().apply("(System.gc())");
        System.out.println(root.toString(2));
    }
    @Test
    public void gcType_gc(){
        Json root =f.gcType().apply("2018-04-17T10:42:28.747-0500: 0.076: #0: [GC (Allocation Failure)  61852K->15323K(247488K), 0.0066592 secs]");
        assertTrue("has type:"+root.toString(),root.has("type"));
        assertEquals("GC",root.getString("type"));
    }
    @Test
    public void gcType_fullGc(){
        Parser p = new Parser();
        Json root;
        p.add(f.gcType());
        p.onLine("2018-04-17T10:42:28.870-0500: 0.199: #5: [Full GC (Allocation Failure)  180052K->50260K(287424K), 0.0084822 secs]");
        root = p.getBuilder().getRoot();
        assertTrue("has type:"+root.toString(),root.has("type"));
        assertEquals("Full GC",root.getString("type"));
    }
    @Test
    public void gcResize(){
        Parser p = new Parser();
        Json root;
        p.add(f.gcResize());
        p.onLine("2018-04-17T10:42:28.747-0500: 0.076: #0: [GC (Allocation Failure)  61852K->15323K(247488K), 0.0066592 secs]");
        root = p.getBuilder().getRoot();
        assertTrue("has before:"+root.toString(),root.has("before"));
        assertTrue("has after:"+root.toString(),root.has("after"));
        assertTrue("has capacity:"+root.toString(),root.has("capacity"));
    }
    @Test
    public void gcSecs(){
        Parser p = new Parser();
        Json root;
        p.add(f.gcSecs());
        p.onLine("2018-04-17T10:42:28.747-0500: 0.076: #0: [GC (Allocation Failure)  61852K->15323K(247488K), 0.0066592 secs]");
        root = p.getBuilder().getRoot();
        assertTrue("has seconds:"+root.toString(),root.has("seconds"));
        assertEquals(0.0066592,root.getDouble("seconds"),0.0000001);
    }
    @Test
    public void gcCmsUsed(){
        Parser p = new Parser();
        Json root;
        p.add(f.gcCmsUsed());
        p.onLine(" 559443K(899448K)");
        root = p.getBuilder().getRoot();
        assertTrue("has used:"+root.toString(),root.has("used"));
        assertTrue("has capacity:"+root.toString(),root.has("capacity"));
    }
    @Test
    public void gcCmsUsed_spaced(){
        Json root=f.gcCmsUsed().apply(" 0 K (180032 K)]");
        assertTrue("has used:"+root.toString(),root.has("used"));
        assertTrue("has capacity:"+root.toString(),root.has("capacity"));
    }
    @Test
    public void gcCmsTimed(){
        Json root;
        root = f.gcCmsTimed().apply("[Rescan (parallel) , 0.0004003 secs]");
        assertEquals("phase","Rescan (parallel)",root.getString("phase"));
        assertEquals("secs",0.0004003,root.getDouble("secs"),0.00000001);

        root = f.gcCmsTimed().apply("[weak refs processing, 0.0000055 secs]");
        assertEquals("phase","weak refs processing",root.getString("phase"));
        assertEquals("secs",0.0000055,root.getDouble("secs"),0.00000001);

        root = f.gcCmsTimed().apply("[class unloading, 0.0001464 secs]");
        assertEquals("phase","class unloading",root.getString("phase"));
        assertEquals("secs",0.0001464,root.getDouble("secs"),0.00000001);

        root = f.gcCmsTimed().apply("[scrub symbol table, 0.0001936 secs]");
        assertEquals("phase","scrub symbol table",root.getString("phase"));
        assertEquals("secs",0.0001936,root.getDouble("secs"),0.00000001);
    }

    @Test
    public void gcDetailsRegionName(){
        Json root;
        root = f.gcDetailsRegionName().apply("[1 CMS-initial-mark: 719189K(932408K)]");
        assertEquals("region","1 CMS-initial-mark",root.getString("region"));

        root = f.gcDetailsRegionName().apply("[ParNew: 0K->0K(166720K), 0.0006483 secs]");
        assertEquals("region","ParNew",root.getString("region"));
    }
    @Test
    public void gcDetailsTimes(){
        Json root=f.gcDetailsTimes().apply("0.184: [GC (Allocation Failure) 0.184: [DefNew: 41286K->0K(76800K), 0.0232887 secs]0.208: [Tenured: 180052K->50260K(210624K), 0.0050705 secs] 181403K->50260K(287424K), [Metaspace: 2934K->2934K(1056768K)], 0.0287714 secs] [Times: user=0.01 sys=0.01, real=0.03 secs] ");
        assertTrue("has user,sys,real:"+root.toString(),
            root.has("user") && root.has("sys") && root.has("real")
        );
    }
    @Test
    public void gcHeapAtGcHeader_before(){
        Json root=f.gcHeapAtGcHeader().apply("{Heap before GC invocations=0 (full 0):");
        assertTrue("has phase,gcCount,fullCount:"+root,
                root.has("phase") && root.has("gcCount") && root.has("fullCount"));
    }
    @Test
    public void gcHeapAtGcHeader_after(){
        Json root=f.gcHeapAtGcHeader().apply("Heap after GC invocations=1 (full 0):");
        assertTrue("has phase,gcCount,fullCount:"+root,
                root.has("phase") && root.has("gcCount") && root.has("fullCount"));
    }
    @Test
    public void gcDetailsHeapRegion(){
        Json root = f.gcDetailsHeapRegion().apply(" def new generation   total 76800K, used 61852K [0x00000006c7200000, 0x00000006cc550000, 0x000000071a150000)");
        assertEquals("region is def new generation","def new generation",root.getString("region"));
    }
    @Test
    public void gcDetailsHeapSpace(){
        Json root=f.gcDetailsHeapSpace().apply("  eden space 68288K,   0% used [0x00000006c7200000, 0x00000006c7200000, 0x00000006cb4b0000)");

        assertEquals("space is eden","eden",root.getString("space"));
    }
    @Test
    public void gcDetailsHeapMeta(){
        Json root=f.gcDetailsHeapMeta().apply(" Metaspace       used 2934K, capacity 4486K, committed 4864K, reserved 1056768K");
        assertEquals("region","Metaspace",root.getString("region"));
    }
    @Test
    public void gcDetailsHeapMetaSpace(){
        Json root = f.gcDetailsHeapMetaSpace().apply("  class space    used 317K, capacity 386K, committed 512K, reserved 1048576K");
        assertEquals("space is class","class",root.getString("space"));

    }
    @Test
    public void gcApplicationConcurrent(){
        Json root = f.gcApplicationConcurrent().apply(" Application time: 0.0435193 seconds");
        assertTrue("has 'applicationConcurrent':"+root,root.has("applicationConcurrent"));
    }
    @Test
    public void gcApplicationStopped(){
        Json root = f.gcApplicationStopped().apply("Total time for which application threads were stopped: 0.0084703 seconds, Stopping threads took: 0.0000127 seconds");
        assertTrue("has 'applicationStopped':"+root,root.has("applicationStopped"));
    }

}
