package perf.parse.factory;

import perf.parse.Eat;
import perf.parse.Exp;
import perf.parse.JsonConsumer;
import perf.parse.Merge;
import perf.parse.Parser;
import perf.parse.Rule;
import perf.parse.Value;
import perf.parse.reader.TextLineReader;
import perf.yaup.AsciiArt;
import perf.yaup.json.Json;

import java.util.LinkedList;

/**
 * Created by wreicher
 */
public class OpenJdkGcFactory {

    public Exp jdk9Tags(){
        return new Exp("tags","\\[(?<tags:List>[^\\s,\\]]+)")

                .add(new Exp("otherTags",",(?<tags:List>[^\\s,\\]]+)")
                        .set(Rule.Repeat)
                )
                .add(new Exp("tagsEnd","\\s*\\]")
                );
    }
    public Exp jdk9LogStopHeapFormat(){
        return new Exp("jdk9StopHeapFormat","(?<usedBefore>\\d+)M->(?<usedAfter>\\d+)M\\((?<capacity>\\d+)M\\)");
    }
    public Exp jdk9Level(){
        return new Exp("level","\\[(?<level>error|warning|info|debug|trace|develop)\\s*\\]");
    }

    public Exp jdk9gcCpu(){
        return new Exp("gcCpu","User=(?<user:Number>\\d+\\.\\d{3})s Sys=(?<sys:Number>\\d+\\.\\d{3})s Real=(?<real:Number>\\d+\\.\\d{3})s").group("gcCpu").eat(Eat.Line);
    }
    public Exp jdk9gcClassHistoEnd(){
        return new Exp("gcClassHistoEnd","Class Histogram \\((?<phase>\\S+) full gc\\) (?<ms:Number>\\d+\\.\\d{3})ms")
                .set(Merge.NewStart);

    }
    public Exp jdk9safepointStopTime(){
        return new Exp("safepointStop","\\[safepoint\\s*\\] Total time for which application threads were stopped: (?<totalSeconds:Number>\\d+\\.\\d+) seconds, Stopping threads took: (?<threadSeconds:Number>\\d+\\.\\d+) seconds")
                .group("safepoint")
                .group("stop");
    }
    public Exp jdk9safepointAppTime(){
        return new Exp("safepointApplication","\\[safepoint\\s*\\] Application time: (?<seconds:Number>\\d+\\.\\d+) seconds")
                .group("safepoint")
                .group("application");
    }

    public Exp jdk9gcClassHistoStart(){
        return new Exp("gcClassHistoStart","Class Histogram \\((?<phase>\\S+) full gc\\)")
                .set(Merge.NewStart);
    }
    public Exp jdk9gcClassHistoEntry(){
        return new Exp("gcClassHistoEntry","(?<num:Number>\\d+):\\s+(?<count:Number>\\d+):\\s+(?<bytes:Number>\\d+)\\s+(?<name>.*)").group("histo").set(Merge.Entry);
    }
    public Exp jdk9gcClassHistoTotal(){
        return new Exp("gcClassHistoTotal","Total\\s+(?<count:Number>\\d+)\\s+(?<bytes:Number>").group("total");
    }

    public Exp jdk9GcTarget(){

        return new Exp("gcTarget","GC\\((?<gcId>\\d+)\\)").set("gcId",Value.TargetId);
    }
    public Exp jdk9Timestamp(){
        return new Exp("timestamp","\\[(?<timestamp>\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3}-\\d{4}\\]");
    }
    public Exp jdk9UsingGC(){
        return new Exp("using","Using (?<gc>\\S+)");
    }
    public Exp jdk9Expanding(){
        return new Exp("expand","Expanding (?<gen>\\S+) from (?<from>\\d+[kKmMgG]?) by (?<by>\\d+[kKmMgG]?) to (?<to>\\d+[kKmMgG]?)")
            .set("from",Value.KMG).set("by",Value.KMG).set("to",Value.KMG);
    }
    public Exp jdk9Shrinking(){
        return new Exp("shrink","Shrinking (?<gen>\\S+) from (?<from>\\d+[kKmMgG]?) by (?<by>\\d+[kKmMgG]?) to (?<to>\\d+[kKmMgG]?)")
            .set("from",Value.KMG).set("by",Value.KMG).set("to",Value.KMG);
    }
    public Exp newJavaHotspotPattern(){
        return new Exp("javaHotspot","Java HotSpot\\(TM\\) (?<hotspot>.+?) for (?<platform>\\S+) JRE \\((?<jvmVersion>[^\\)]+)\\), built on (?<buildDate>.+?) by (?<builder>.*)")
                .eat(Eat.Line)
                .set(Merge.NewStart);
    }
    public Exp newOpenjdkPattern(){
        return new Exp("openJdk","OpenJDK (?<hotspot>.+?) for (?<platform>\\S+) JRE \\((?<jvmVersion>[^\\)]+)\\), built on (?<buildDate>.+?) by (?<builder>.*)")
                .eat(Eat.Line)
                .set(Merge.NewStart);
    }
    public Exp newMemoryPattern(){
        return new Exp("memory","Memory: (?<pageSize>\\d+[kKmMgG]?) page, physical (?<physicalTotal>\\d+[kKmMgG]?)\\((?<physicalFree>\\d+[kKmMgG]?) free\\), swap (?<swapTotal>\\d+[kKmMgG]?)\\((?<swapFree>\\d+[kKmMgG]?) free\\)")
                .eat(Eat.Line)
                .set("pageSize",Value.KMG)
                .set("physicalTotal",Value.KMG)
                .set("physicalFree",Value.KMG)
                .set("swapTotal",Value.KMG)
                .set("swapFree",Value.KMG)
                ;
    }
    public Exp commandLinePattern(){
        return new Exp("commandLine","CommandLine flags:")
                .add(new Exp("flag"," (?<flag>[^ ]+)")
                .set(Rule.Repeat)
                .eat(Eat.Match));
    }
    public Exp newHeapPattern(){
        return new Exp("heap","Heap")
                .set(Merge.NewStart)
                .enables("heapSummary")
                .eat(Eat.Line);
    }
    public Exp newHeapGenPattern(){
        return new Exp("heapGen"," (?<name>\\S+)\\s+total (?<total>\\d+[KMG]), used (?<used>\\d+[KMG]) \\[(?<startAddress>0x[0-9a-f]+), (?<writeAddress>0x[0-9a-f]+), (?<endAddress>0x[0-9a-f]+)\\)")
                .set(Merge.Entry)
                .requires("heapSummary")
                .group("section")
                .set(Rule.AvoidTarget)
                .set(Rule.PushTarget)
                .set("total",Value.KMG)
                .set("used",Value.KMG)
                ;
    }
    public Exp newHeapGenSpacePattern(){
        return new Exp("heapGenSpace","\\s+(?<name>\\S+) space (?<size>\\d+[KMG]), (?<usedPercentage>\\d+)% used \\[(?<startAddress>0x[0-9a-f]+),(?<writeAddress>0x[0-9a-f]+),(?<endAddress>0x[0-9a-f]+)\\)")
                .set(Merge.Entry)
                .requires("heapSummary")
                .group("space")
                .set("size",Value.KMG)
                .set("usedPercentage",Value.Number)
                ;
    }
    public Exp newHeapMetaspacePattern(){
        return new Exp("metaspace","^ (?<name>\\S+)\\s+used (?<used>\\d+[KMG]), capacity (?<capacity>\\d+[KMG]), committed (?<committed>\\d+[KMG]), reserved (?<reserved>\\d+[KMG])")
                .set(Merge.Entry)
                .requires("heapSummary")
                .enables("heapMetaspaceSummary")
                .group("section")
                .set(Rule.AvoidTarget)
                .set(Rule.PushTarget)
                .set("used",Value.KMG)
                .set("capacity",Value.KMG)
                .set("committed",Value.KMG)
                .set("reserved",Value.KMG)
                ;
    }
    public Exp newHeapMetaspaceRegionPattern(){
        return new Exp("metaspaceRegion","  (?<regionName>.{4,}\\S)\\s{2,}used (?<used>\\d+[KMG]), capacity (?<capacity>\\d+[KMG]), committed (?<committed>\\d+[KMG]), reserved (?<reserved>\\d+[KMG])")
                .set(Merge.Entry)
                .requires("heapMetaspaceSummary")
                .requires("heapSummary")
                .group("space")
                .set("used",Value.KMG)
                .set("capacity",Value.KMG)
                .set("committed",Value.KMG)
                .set("reserved",Value.KMG)
                ;
    }
    public Exp newTimestampPattern(){
        return new Exp("timestamp","^(?<timestamp>\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3}-\\d{4}): ").set(Merge.NewStart).eat(Eat.Match);
    }
    public Exp newElapsedPattern(){
        return new Exp("elapsed","^(?<elapsed>\\d+\\.\\d{3}): ")
            .set("elapsed", Value.Number)
            .set(Merge.NewStart)
            .eat(Eat.Match);
    }
    public Exp newStopTimePattern(){
        return new Exp("stopTime","Total time for which application threads were stopped: (?<threadpause>\\d+\\.\\d+) seconds")
                .eat(Eat.Line)
                .add(new Exp("stoppingThread","Stopping threads took: (?<theadStopping>\\d+\\.\\d+) seconds"));
    }
    public Exp newUserSysRealPattern(){
        return new Exp("usersysreal","\\[Times: user=(?<user>\\d+\\.\\d{2}) sys=(?<sys>\\d+\\.\\d{2}), real=(?<real>\\d+\\.\\d{2}) secs\\]").group("times").eat(Eat.Match);
    }
    public Exp newRegionPattern(){
        return new Exp("region","\\[(?<name>\\w+): (?<pregc>\\d+[KMG]?)->(?<postgc>\\d+[KMG]?)\\((?<size>\\d+[KMG]?)\\)\\][ ,]")
            .group("region")
            .key("name") // key name means we do not need to Merge as new Entry
            //.set(Exp.Merge.Entry)
            .set(Rule.Repeat)
            .eat(Eat.Match)
            .set("pregc", Value.KMG)
            .set("postgc", Value.KMG)
            .set("size", Value.KMG);
    }
    public Exp newSurvivorThresholdPattern(){
        return new Exp("survivorthreshold","Desired survivor size (?<survivorsize>\\d+) bytes, new threshold (?<threshold>\\d+) \\(max (?<maxThreshold>\\d+)\\)");
    }
    public Exp newGCReasonPattern(){
        return new Exp("gcreason","\\((?<gcreason>.+?)\\) ")
            .eat(Eat.Match);
    }
    public Exp newGCTimePattern(){
        return new Exp("gctime",", (?<gctime>\\d+\\.\\d+) secs\\] ")
                .set("gctime",Value.Number);
    }
    public Exp newHeapSizePattern(){
        return new Exp("heapsize","(?<pregc>\\d+[KMG]?)->(?<postgc>\\d+[KMG]?)\\((?<size>\\d+[KMG]?)\\)")
            .group("heap")
            .eat(Eat.Match)
            .set("pregc", Value.KMG)
            .set("postgc", Value.KMG)
            .set("size", Value.KMG);
    }
    public Exp newFullGCPattern(){
        return new Exp("FullGC","^\\[(?<gctype>Full GC) ").set(Merge.NewStart).eat(Eat.Match);
    }
    public Exp newGCPattern(){
        return new Exp("GC","^\\[(?<gctype>GC) ").set(Merge.NewStart).eat(Eat.Match);
    }
    public Exp newPolicyPattern(){
        return new Exp("policy","^(?<key>\\w+)::(?<value>[^:]+):").set("key","value").group("policy").set(Merge.Entry).eat(Eat.Match)
            .add(new Exp("K,Vnumber", "  (?<key>\\w+): (?<value>\\d+\\.?\\d*)").set("key", "value").set(Rule.Repeat));
    }

    public Exp newTlabThreadPattern(){
        return new Exp("TlabThread","TLAB: (?<threadName>[^:]+):" +
            " (?<address>0x[0-9a-f]+)" +
            " \\[id: (?<id>\\d+)\\]" +
            " desired_size: (?<desiredSize>\\d+[KMG]?B)" +
            " slow allocs: (?<slowAllocs>\\d+)" +
            "\\s+refill waste: (?<wasteSize>\\d+[KMG]?B)" +
            " alloc: (?<alloc>\\d+\\.\\d{5})" +
            "\\s+(?<allocSize>\\d+[KMG]?B)" +
            " refills: (?<refills>\\d+)" +
            " waste (?<wastePercentage>\\d+\\.\\d+)%" +
            " gc: (?<gcSize>\\d+[KMG]?B)" +
            " slow: (?<slowSize>\\d+[KMG]?B)" +
            " fast: (?<fastSize>\\d+[KMG]?B)"
        )
            .set("id",Value.Number)
            .set("desiredSize",Value.KMG)
            .set("slowAllocs",Value.Number)
            .set("wasteSize",Value.KMG)
            .set("alloc",Value.Number)
            .set("allocSize",Value.KMG)
            .set("refills",Value.Number)
            .set("wastePercentage",Value.Number)
            .set("gcSize",Value.KMG)
            .set("slowSize",Value.KMG)
            .set("fastSize",Value.KMG)
            .eat(Eat.Line)
            .set(Merge.NewStart);
    }
    public Exp newTlabTotalPattern(){
        return new Exp("TlabTotal","TLAB totals: thrds: (?<threads>\\d+)" +
                "\\s+refills: (?<refills>\\d+) max: (?<maxRefills>\\d+)" +
                " slow allocs: (?<slowAllocs>\\d+) max (?<maxSlowAllocs>\\d+)" +
                " waste: (?<wastePercentage>\\d+\\.\\d+)%" +
                " gc: (?<gcSize>\\d+[KMG]?B) max: (?<maxGcSize>\\d+[KMG]?B)" +
                " slow: (?<slowSize>\\d+[KMG]?B) max: (?<maxSlowSize>\\d+[KMG]?B)" +
                " fast: (?<fastSize>\\d+[KMG]?B) max: (?<maxFastSize>\\d+[KMG]?B)")
                .set("threads",Value.Number)
                .set("refills",Value.Number)
                .set("maxRefills",Value.Number)
                .set("slowAllocs",Value.Number)
                .set("maxSlowAllocs",Value.Number)
                .set("wastePercentage",Value.Number)
                .set("gcSize",Value.KMG)
                .set("maxGcSize",Value.KMG)
                .set("gcSize",Value.KMG)
                .set("maxGcSize",Value.KMG)
                .set("slowSize",Value.KMG)
                .set("maxSlowSize",Value.KMG)
                .set("fastSize",Value.KMG)
                .set("maxFastSize",Value.KMG)
                .eat(Eat.Line)
                .set(Merge.NewStart)
            ;
    }
    public Exp newApplicationConcurrentTime() {
        return new Exp("applicationConcurrent","Application time: (?<applicationConcurrentTime>\\d+\\.\\d+) seconds")
                .set("applicationConcurrentTime",Value.Number)
                .set(Merge.NewStart);
    }
    public Parser newGcParser(){
        Parser p = new Parser();
        p.add(newHeapPattern());
        p.add(newHeapGenPattern());
        p.add(newHeapGenSpacePattern());
        p.add(newHeapMetaspacePattern());
        p.add(newHeapMetaspaceRegionPattern());
        p.add(newJavaHotspotPattern());
        p.add(newOpenjdkPattern());
        p.add(newMemoryPattern());
        p.add(commandLinePattern());
        p.add(newTimestampPattern());
        p.add(newElapsedPattern());
        p.add(newGCPattern());
        p.add(newFullGCPattern());
        p.add(newStopTimePattern());
        p.add(newUserSysRealPattern());
        p.add(newRegionPattern());
        p.add(newSurvivorThresholdPattern());
        p.add(newGCReasonPattern());
        p.add(newGCTimePattern());
        p.add(newHeapSizePattern());
        p.add(newPolicyPattern());
        p.add(newTlabThreadPattern());
        p.add(newTlabTotalPattern());
        p.add(newApplicationConcurrentTime());
        return p;
    }

    public static void main(String[] args) {


        String filePath="";
        //String filePath = "/home/wreicher/specWork/reentrant/reentrant-aio-196/client1.specjms.verbose-gc-sm.gclog";
        //String filePath = "/home/wreicher/specWork/reentrant/reentrant-aio-196/server_20160114_221048.gclog";
        //String filePath = "/tmp/server_2lc_setup.gclog";
        //String filePath = "/home/wreicher/specWork/server_20140902_122635.152K.gclog";
        filePath = "/home/wreicher/specWork/server.255Z.gclog";
        filePath = "/home/wreicher/specWork/server.256A.gclog";
        filePath = "/home/wreicher/specWork/server.256B.gclog";
        filePath = "/home/wreicher/specWork/server.256B.gclog";
        filePath = "/home/wreicher/specWork/server.256I.gclog";
        filePath = "/home/wreicher/perfWork/eap-tests-ejb/server_20170314_100949.gclog";
        filePath = "/home/wreicher/perfWork/byteBuffer/326G/server_20170303_125218.gclog";
        //filePath = "/home/wreicher/perfWork/openshift/openjdk/gc.log.0.current";
        //filePath = "/home/wreicher/perfWork/byteBuffer/326F/server_20170303_103608.gclog";
        TextLineReader r = new TextLineReader();
        OpenJdkGcFactory f = new OpenJdkGcFactory();
        Parser p = f.newGcParser();

        //p.add((json)->System.out.println(json.toString(2)));
        r.addParser(p);
        r.read("/home/wreicher/perfWork/amq/jdbc/00259/client1/specjms.verbose-gc-hq.gclog");
        //System.exit(0);

        long gargage = 0;

        //fullGC
        StringBuilder fullGc = new StringBuilder();
        LinkedList<Json> pokemon = new LinkedList<>();
        p.add((json)->{
            pokemon.add(json);
            if(json.has("times")){
                double user = json.getJson("times").getDouble("user");
                double sys = json.getJson("times").getDouble("sys");
                double real = json.getJson("times").getDouble("real");
                if(real > user + sys){
                    System.out.println(AsciiArt.ANSI_RED+user+" > "+(sys+real)+" @ "+json.getString("timestamp")+AsciiArt.ANSI_RESET);
                }
            }
        });
        p.add(new JsonConsumer(){

            @Override
            public void consume(Json object) {
                if(object.has("gctype")){
                    if(object.getString("gctype").contains("Full GC")){
                        fullGc.append("■");
                    }else{
                        fullGc.append(" ");
                    }
                }
            }
        });

        //gcOverhead
        StringBuilder gcOverhead = new StringBuilder();
        p.add(new JsonConsumer() {
            private double elapsed = 0.0;
            @Override
            public void consume(Json object) {
                if(object.has("elapsed") && object.has("gctime")){
                    double newElapsed = object.getDouble("elapsed");
                    double gcTime = object.getDouble("gctime");
                    double overHead = gcTime/(newElapsed-elapsed);

                    gcOverhead.append( AsciiArt.horiz( overHead , 1 ) );
                    elapsed = newElapsed;
                }
            }
        });
        //gcbars
        long totalSize=12l*1024l*1024l*1024l; // 1GB
        System.out.println("toalSize="+totalSize);
        int maxWidth = 20;
        double ratio = 1.0*totalSize/maxWidth;
        p.add(new JsonConsumer() {
            @Override
            public void consume(Json object) {
                //System.out.println(object.toString(2));
                if(object.has("region")){
                    Json region = object.getJson("region");
                    String youngGen="";
                    if(region.has("PSYoungGen")){
                        long size = region.getJson("PSYoungGen").getLong("size");
                        long pregc = region.getJson("PSYoungGen").getLong("pregc");
                        long postgc = region.getJson("PSYoungGen").getLong("postgc");
                        youngGen = AsciiArt.vert(postgc,totalSize,maxWidth,true);

                    }
                    String oldGen = " ";
                    if(region.has("ParOldGen")){

                        long size = region.getJson("ParOldGen").getLong("size");
                        long pregc = region.getJson("ParOldGen").getLong("pregc");
                        long postgc = region.getJson("ParOldGen").getLong("postgc");
                        oldGen = AsciiArt.vert(postgc,totalSize,maxWidth,true);
                    }
                    String heap = " ";
                    if(object.has("heap")){
                        long size = object.getJson("heap").getLong("size");
                        long pregc = object.getJson("heap").getLong("pregc");
                        long postgc = object.getJson("heap").getLong("postgc");

                        heap = AsciiArt.vert(postgc,totalSize,maxWidth,true);
                    }

                    //System.out.println("maxWidth="+maxWidth+" youngGen.length="+youngGen.length());

                    System.out.printf("|%"+maxWidth+"s|%"+maxWidth+"s|%"+maxWidth+"s|\n", youngGen, oldGen, heap);

                }
            }
        });

        //postgc heap usage v size
        StringBuilder pgc = new StringBuilder();

        p.add(new JsonConsumer() {
            @Override
            public void start() {}

            @Override
            public void consume(Json object) {
                if(object.has("heap") && object.getJson("heap").has("postgc")){
                    long postgc = object.getJson("heap").getLong("postgc");
                    long size = object.getJson("heap").getLong("size");
                    pgc.append(AsciiArt.horiz(postgc,size));
                }
            }

            @Override
            public void close() {}
        });
        r.addParser(p);

        //p.onLine(new CheatChars("2015-11-13T11:16:55.950-0600: 25.617: [GC (Allocation Failure) [PSYoungGen: 269312K->11928K(309248K)] 316281K->58905K(658944K), 0.0059238 secs] [Times: user=0.02 sys=0.00, real=0.00 secs]\n"));
        //p.close();
        //p.onLine(new CheatChars("2015-11-13T11:16:37.515-0600: 7.182: [Full GC (Metadata GC Threshold) [PSYoungGen: 30352K->0K(306688K)] [ParOldGen: 17229K->46969K(349696K)] 47582K->46969K(656384K), [Metaspace: 21093K->21093K(1069056K)], 0.2619827 secs] [Times: user=0.89 sys=0.02, real=0.27 secs]\n"));
        //p.close();
        //p.onLine(new CheatChars("2015-11-13T11:16:55.950-0600: 25.617: [GC (Allocation Failure) [PSYoungGen: 269312K->11928K(309248K)] 316281K->58905K(658944K), 0.0059238 secs] [Times: user=0.02 sys=0.00, real=0.00 secs]\n"));

        r.read(filePath);
        System.out.println("heap.postgc │"+pgc.toString());
        System.out.println("gcOverhead  │"+gcOverhead.toString());
        System.out.println("full GCs    │"+fullGc.toString());


        System.out.println("pokemon = "+pokemon.size() + pokemon.getFirst().toString());
    }
}
