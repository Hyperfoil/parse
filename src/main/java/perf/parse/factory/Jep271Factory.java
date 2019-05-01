package perf.parse.factory;

import perf.parse.*;

public class Jep271Factory implements ParseFactory{
    public Parser newParser() {
        Parser p = new Parser();
        addToParser(p);
        return p;
    }
    public void addToParser(Parser p) {
        addToParser(p,false);
    }
    public void addToParser(Parser p,boolean strict){
        //thankfully level is always the 2nd to last decorator
        p.add(gcId()
                .addRule(ExpRule.TargetRoot)//so that gcId is always on the root
                //Expanding does not occur under GC(#) but that might be a bug. In 11.0.1 it is now under gcId
                .add(gcExpanding().group("resize").setMerge(ExpMerge.AsEntry))
                .add(gcShrinking().group("resize").setMerge(ExpMerge.AsEntry))

                //shenandoah before gcPause because gcPause can match Pause stage

                .add(shenandoahPhase().requires("gc-shenandoah").group("phases"))


                .add(gcPause())//TODO put behind a requires so it doesn't match shenandoah pause phase?

                .add(parallelSizeChanged().group("resize").setMerge(ExpMerge.AsEntry))//ParallelGC

                .add(g1ResizePhase().requires("gc-g1").setMerge(ExpMerge.AsEntry))//G1
                .add(g1TimedPhase().requires("gc-g1").setMerge(ExpMerge.AsEntry))//G1

                //gc+cpu
                .add(gcCpu()
                    .group("cpu")
                    //.eat(Eat.Line) was preventing gcLevel...
                )

                //gc+age
                .add(gcAge())
                .add(gcAgeTableHeader().addRule(ExpRule.PushTarget))//G1
                .add(gcAgeTableEntry()
                    .group("table")
                    .key("age")
                )//G1


                //gc+heap
                .add(gcHeapHeader()
                    .group("heap")
                    .setMerge(ExpMerge.AsEntry)
                    .addRule(ExpRule.PreClearTarget)

                        .add(gcHeapRegion()
                            .group("region")
                              .setMerge(ExpMerge.AsEntry)

                            .addRule(ExpRule.PreClearTarget)//gcId targets root
                            //.set(ExpRule.PushTarget,"region")
                        )//oracle-10 puts it on the same line as "Heap (before|after)..."
                        .add(gcHeapRegionG1().requires("gc-g1").addRule(ExpRule.PushTarget))
                )
                .add(gcHeapRegion()
                    .group("region")
                    .setMerge(ExpMerge.AsEntry)
                    .addRule(ExpRule.PreClearTarget)//gcId targets root
                )
                .add(gcHeapSpace()
                    .extend("region")
                    .group("space")
                    .setMerge(ExpMerge.AsEntry)
                    .addRule(ExpRule.PrePopTarget)//gcId targets root
                )
                .add(gcHeapSpaceG1().requires("gc-g1"))
                .add(gcHeapMetaRegion()
                    .group("region")
                    .setMerge(ExpMerge.AsEntry)
                    .addRule(ExpRule.PreClearTarget)//gcId target and previous region
                )
                .add(gcHeapMetaSpace()
                    .extend("region")
                    .group("space")
                    .setMerge(ExpMerge.AsEntry)
                    .addRule(ExpRule.PrePopTarget)//gcId targets root
                )
                .add(gcHeapRegionResize()
                    .group("resize")
                    .setMerge(ExpMerge.AsEntry)
                    .addRule(ExpRule.PreClearTarget)
                )
                .add(gcHeapRegionResizeG1()
                    .requires("gc-g1")
                    .group("resize")
                    .setMerge(ExpMerge.AsEntry)
                    .addRule(ExpRule.PreClearTarget)
                    .addRule(ExpRule.PushTarget)
                )
                .add(gcHeapRegionResizeG1UsedWaste().requires("gc-g1"))

                .add(gcClassHistoStart())
                .add(gcClassHistoEntry()
                    .group("histo")
                    .setMerge(ExpMerge.AsEntry)
                )
                .add(gcClassHistoTotal().group("total"))
                .add(gcClassHistoEnd())
        );

        //before level so it can PreClose the previous trigger
        p.add(shenandoahTrigger().group("trigger").requires("gc-shenandoah").addRule(ExpRule.PreClose));

        p.add(gcLevel().enables("jep271-decorator")
                .add(time().setRange(MatchRange.BeforeParent))
                .add(utcTime().setRange(MatchRange.BeforeParent))
                .add(uptime().setRange(MatchRange.BeforeParent))
                .add(timeMillis().setRange(MatchRange.BeforeParent))
                .add(uptimeMillis().setRange(MatchRange.BeforeParent))
                .add(timeNanos().setRange(MatchRange.BeforeParent))
                .add(uptimeNanos().setRange(MatchRange.BeforeParent))
        );

        //moved to after gcId to avoid appending to previous event
        //
        p.add(
            //need requires so it won't match [GC ... from printGc logs
            gcTags().requires("jep271-decorator")
        );//always the last decorator

        //p.add(gcKeyValue());
        p.add(g1MarkStack().requires("gc-g1"));

        p.add(usingSerial().addRule(ExpRule.PostClose));
        p.add(usingParallel().addRule(ExpRule.PostClose));
        p.add(usingCms().addRule(ExpRule.PostClose));
        p.add(usingG1().addRule(ExpRule.PostClose));
        p.add(usingShenandoah().addRule(ExpRule.PostClose));

        p.add(gcExpanding());//included here to match output from openjdk10+46



        p.add(safepointStopTime().group("safepoint"));//safepoint
        p.add(safepointAppTime().group("safepoint"));//safepoint

        //gc+heap
        p.add(gcHeapSize().group("heap"));
        p.add(gcHeapRange().group("heap"));
        p.add(gcHeapYoungRange()
            .group("heap")
            .group("young")
        );
        p.add(gcHeapOldRange()
            .group("heap")
            .group("old")
        );
        //
        //end moved to after gcId
    }

    //enables different gc collectors
    //
    public Exp usingCms(){ //Using Concurrent Mark Sweep
        return new Exp("using","Using (?<gc>Concurrent Mark Sweep)")
                .enables("gc-cms");
    }
    public Exp usingSerial(){ //Using Serial
        return new Exp("using","Using (?<gc>Serial)")
                .enables("gc-serial");
    }
    public Exp usingParallel(){ //Using Parallel
        return new Exp("using","Using (?<gc>Parallel)")
                .enables("gc-parallel");
    }
    public Exp usingG1(){ //Using G1
        return new Exp("using","Using (?<gc>G1)")
                .enables("gc-g1");
    }
    public Exp usingShenandoah(){//
        return new Exp("using","Using (?<gc>Shenandoah)")
                .enables("gc-shenandoah");
    }




    //gc
    //can
    public Exp gcPause(){//"Pause Young (Allocation Failure) 62M->15M(241M) 9.238ms"
        return new Exp("pause","Pause (?<region>.+\\S)\\s+\\((?<reason>[^\\)]+)\\)")
                .add(gcResize())
                .add(new Exp("time","(?<milliseconds>\\d+\\.\\d{3})ms"));
    }

    public Exp gcTags(){
        return new Exp("tags","\\[(?<tags:set>[^\\s,\\]]+)")
                .addRule(ExpRule.TargetRoot)
                .add(new Exp("otherTags","^,(?<tags:set>[^\\s,\\]]+)")
                    .addRule(ExpRule.Repeat)
                    .addRule(ExpRule.TargetRoot)
                )
                .add(new Exp("tagsEnd","\\s*\\]")
                )

                ;
    }
    public Exp gcResize(){//61852K->15323K(247488K)
        return new Exp("gcResize","(?<usedBefore:KMG>\\d+[bBkKmMgG])->(?<usedAfter:KMG>\\d+[bBkKmMgG])\\((?<capacity:KMG>\\d+[bBkKmMgG])\\)");
    }
    public Exp gcLevel(){//"[info ]"
        return new Exp("level","\\[(?<level:last>error|warning|info|debug|trace|develop)\\s*\\]")
                .addRule(ExpRule.TargetRoot)
                .eat(Eat.ToMatch);
    }

    public Exp gcKeyValue(){//TODO when is this used
        return new Exp("gcKeyValue","(?<key>\\S+): (?<value>\\d+)")
                .group("stat")
                .setMerge(ExpMerge.AsEntry);
    }

    //Parallel
    //
    public Exp parallelSizeChanged(){//"PSYoung generation size changed: 1358848K->1356800K"
        return new Exp("parallelSizeChange","(?<region>\\w+) generation size changed: (?<before:KMG>\\d+[bBkKmMgG])->(?<after:KMG>\\d+[bBkKmMgG])")
                ;
    }

    //Shenandoah
    //
    // Concurrent reset 50M->50M(512M) 0.381ms
    // Pause Init Mark (process weakrefs) 1.939ms")
    // Concurrent marking (process weakrefs) 50M->51M(512M) 6.146ms
    public Exp shenandoahPhase(){//
        return new Exp("shenandoah.phase","(?<lock>Pause|Concurrent) (?<phase>[a-zA-Z]+(?:\\s[a-zA-Z]+)*) ")
                //.group("phases")
                .setMerge(ExpMerge.AsEntry)
                .add(new Exp("task","\\((?<task>[a-zA-Z]+(?:\\s[a-zA-Z]+)+)\\) "))
                .add(gcResize())
                .add(new Exp("milliseconds","(?<milliseconds>\\d+\\.\\d{3})ms"));
    }

    //Trigger: Allocated since last cycle (51M) is larger than allocation threshold (51M)
    //Trigger: Time since last GC (30004 ms) is larger than guaranteed interval (30000 ms)
    //Trigger: Handle Allocation Failure
    //Trigger: Free (40M) is below minimum threshold (51M)
    //Trigger: Learning 1 of 5. Free (357M) is below initial threshold (358M)
    //Trigger: Average GC time (845.14 ms) is above the time for allocation rate (7.57 MB/s) to deplete free headroom (0M)
    public Exp shenandoahTrigger(){
        return new Exp("shenandoah.trigger","Trigger: ")
            .add(
                new Exp("learning","Learning (?<learningStep>\\d+) of (?<totalSteps>\\d+)\\. ")
            )
            .add(
                new Exp("allocationFailure","Handle Allocation Failure")
                .with("cause","allocation failure")
            )
            .add(
                new Exp("freeThreshold","Free \\((?<free:KMG>\\d+[bBkKmMgG])\\) is below (?:minimum|initial) threshold \\((?<threshold:KMG>\\d+[bBkKmMgG])\\)")
                .with("cause","free threshold")
            )
            .add(
                new Exp("allocationThreshold","Allocated since last cycle \\((?<allocated:KMG>\\d+[bBkKmMgG])\\) is larger than allocation threshold \\((?<threshold:KMG>\\d+[bBkKmMgG])\\)")
                .with("cause","allocation threshold")
            )
            .add(
                new Exp("interval","Time since last GC \\((?<elapsed>\\d+) ms\\) is larger than guaranteed interval \\((?<guarantee>\\d+) ms\\)")
                .with("cause","interval"))
            .add(
                new Exp("rate","Average GC time \\((?<milliseconds>\\d+\\.\\d{2}) ms\\) is above the time for allocation rate \\((?<rate>\\d+\\.\\d{2}) MB/s\\) to deplete free headroom \\((?<free:KMG>\\d+[bBkKmMgG])\\)")
                .with("cause","rate"))
            //.set(Merge.PreClose)
        ;
    }

    //G1GC
    //
    public Exp g1MarkStack(){//"MarkStackSize: 4096k  MarkStackSizeMax: 524288k"
        return new Exp("gcG1MarkStack","MarkStackSize: (?<size:KMG>\\d+[bBkKmMgG])\\s+MarkStackSizeMax: (?<max:KMG>\\d+[bBkKmMgG])")
                .group("markStack");
    }
    public Exp g1ResizePhase(){//"Pause Remark 40M->40M(250M) 1.611ms"
        return new Exp("g1ResizePhase","(?<phase>\\w+(?:\\s\\w+)*) (?<before:KMG>\\d+[bBkKmMgG])->(?<after:KMG>\\d+[bBkKmMgG])\\((?<capacity:KMG>\\d+[bBkKmMgG])\\) (?<milliseconds>\\d+\\.\\d{3})ms")
                .group("phases")
                ;
    }
    public Exp g1TimedPhase(){//"Finalize Live Data 0.000ms"
        return new Exp("g1TimedPhase","(?<phase>\\w+(?:\\s\\w+)*) (?<milliseconds>\\d+\\.\\d{3})ms")
                .group("phases")
                ;
    }

    //gc+cpu
    public Exp gcCpu(){//"User=0.02s Sys=0.01s Real=0.02s"
        return new Exp("gcCpu","User=(?<user>\\d+\\.\\d{2,3})s Sys=(?<sys>\\d+\\.\\d{2,3})s Real=(?<real>\\d+\\.\\d{2,3})s")

                //.eat(Eat.Line)
                ;
    }

    //gc+heap=trace
    public Exp gcHeapSize(){//"Maximum heap size 4173353984"
        return new Exp("gcHeapSize","(?<limit>Initial|Minimum|Maximum) heap size (?<size>\\d+)")

                .setKeyValue("limit","size")
                ;
    }
    //gc+heap=debug
    public Exp gcHeapRange(){//"Minimum heap 8388608  Initial heap 262144000  Maximum heap 4175429632"
        return new Exp("gcHeapRange","Minimum heap (?<min>\\d+)\\s+Initial heap (?<initial>\\d+)\\s+Maximum heap (?<max>\\d+)")
                ;
    }
    //gc+heap=trace
    public Exp gcHeapYoungRange(){//"1: Minimum young 196608  Initial young 87359488  Maximum young 1391788032"
        return new Exp("gcHeapYoungRange","1: Minimum young (?<min>\\d+)\\s+Initial young (?<initial>\\d+)\\s+Maximum young (?<max>\\d+)")
                ;
    }

    public Exp gcHeapOldRange(){//"Minimum old 65536  Initial old 174784512  Maximum old 2783641600"
        return new Exp("gcHeapOldRange","Minimum old (?<min>\\d+)\\s+Initial old (?<initial>\\d+)\\s+Maximum old (?<max>\\d+)")
                ;
    }

    //gc+heap=trace
    public Exp gcHeapHeader(){//"Heap before GC invocations=0 (full 0): "
        return new Exp("gcHeapHeader","Heap (?<phase>before|after) GC invocations=(?<invocations>\\d+) \\(full (?<full>\\d+)\\):\\s*")
                ;
    }
    public Exp gcHeapRegion(){//"def new generation   total 76800K, used 63648K [0x00000006c7200000, 0x00000006cc550000, 0x000000071a150000)"
        //removed (?<region:nestLength>\s+) from start, why was it nest-length?
        return new Exp("gcHeapRegion","\\s*(?<name>\\w+(?:[\\s-]\\w+)*)\\s+total (?<total:KMG>\\d+[bBkKmMgG]), used (?<used:KMG>\\d+[bBkKmMgG])" +
                "\\s+\\[(?<start>[^,]+), (?<current>[^,]+), (?<end>[^(]+)\\)")

                ;
    }
    //TODO NOT in gc.json
    public Exp gcHeapRegionG1(){
        //removed (?<region:nestLength>\s+) from beginning to match changes to gcHeapRegion
        return new Exp("gcHeapRegion_g1","\\s+(?<name>\\w+(?:\\s\\w+)*)\\s+total (?<total:KMG>\\d+[bBkKmMgG]), used (?<used:KMG>\\d+[bBkKmMgG])"+
                "\\s+\\[(?<start>[^,]+), (?<end>[^(]+)\\)")
                ;
    }
    public Exp gcHeapMetaRegion(){//"Metaspace       used 4769K, capacity 4862K, committed 5120K, reserved 1056768K"
        //removed (?<region:nestLength>\s+) from beginning to match changes to gcHeapRegion
        return new Exp("gcHeapMetaRegion","\\s+(?<name>Metaspace)" +
                "\\s+used (?<used:KMG>\\d+[KMG]), capacity (?<capacity:KMG>\\d+[bBkKmMgG]), committed (?<committed:KMG>\\d+[bBkKmMgG]), reserved (?<reserved:KMG>\\d+[bBkKmMgG])")
                ;
    }

    public Exp gcHeapRegionResize(){//"ParOldGen: 145286K->185222K(210944K)"
        return new Exp("gcHeapRegionResize","\\s*(?<region>\\w+): (?<before:KMG>\\d+[bBkKmMgG])->(?<after:KMG>\\d+[bBkKmMgG])\\((?<size:KMG>\\d+[bBkKmMgG])\\)")
                ;
    }
    public Exp gcHeapRegionResizeG1(){//"Eden regions: 4->0(149)"
        return new Exp("gcHeapRegionResizeG1","(?<region>\\S+) regions: (?<before>\\d+)->(?<after>\\d+)")
                .add(new Exp("gcHeapRegionRegizeG1_total","\\((?<total>\\d+)\\)"))
                ;
    }
    public Exp gcHeapRegionResizeG1UsedWaste(){//" Used: 20480K, Waste: 0K"
        return new Exp("gcHeapRegionG1UsedWaste","Used: (?<used:KMG>\\d+[bBkKmMgG]), Waste: (?<waste:KMG>\\d+[bBkKmMgG])")
                ;
    }
    public Exp gcHeapMetaSpace(){//"  class space    used 388K, capacity 390K, committed 512K, reserved 1048576K"
        return new Exp("gcHeapMetaSpace","\\s*(?<space>\\S+) space"+
                "\\s+used (?<used:KMG>\\d+[bBkKmMgG]), capacity (?<capacity:KMG>\\d+[bBkKmMgG]), committed (?<committed:KMG>\\d+[bBkKmMgG]), reserved (?<reserved:KMG>\\d+[bBkKmMgG])")
                ;
    }
    public Exp gcHeapSpace(){//"   eden space 68288K,  93% used [0x00000006c7200000, 0x00000006cb076880, 0x00000006cb4b0000)"
        return new Exp("gcHeapSpace","\\s+(?<space>\\S+) space (?<size:KMG>\\d+[bBkKmMgG]),\\s+(?<used>\\d+)% used"+
                "\\s+\\[(?<start>[^,]+),\\s?(?<current>[^,]+),\\s?(?<end>[^(]+)\\)")
                ;
    }
    public Exp gcHeapSpaceG1(){//"   region size 1024K, 5 young (5120K), 0 survivors (0K)"
        return new Exp("gcHeapSpaceG1","region size (?<regionSize:KMG>\\d+[bBkKmMgG]), (?<youngCount>\\d+) young \\((?<youngSize:KMG>\\d+[bBkKmMgG])\\), (?<survivorCount>\\d+) survivors \\((?<survivorSize:KMG>\\d+[bBkKmMgG])\\)")
                ;
    }


    //TODO gc,safepoint with -XX:+UseG1GC injects safepoint between GC(#) lines
    //this means it would have to be part of the gc event (sum?) or a separate parser
    //using Add for the time being

    //safepoint=info
    public Exp safepointStopTime(){//"Total time for which application threads were stopped: 0.0019746 seconds, Stopping threads took: 0.0000102 seconds"
        return new Exp("safepointStop","Total time for which application threads were stopped: (?<stoppedSeconds:add>\\d+\\.\\d+) seconds, Stopping threads took: (?<quiesceSeconds:add>\\d+\\.\\d+) seconds")

                ;
    }
    //safepoint=info
    public Exp safepointAppTime(){//"Application time: 0.0009972 seconds"
        return new Exp("safepointApplication","Application time: (?<applicationSeconds:add>\\d+\\.\\d+) seconds")

                ;
    }
    public Exp gcClassHistoStart(){//"Class Histogram (before full gc)"
        return new Exp("gcClassHistoStart","Class Histogram \\((?<phase>\\S+) full gc\\)")
                ;
    }
    public Exp gcClassHistoEntry(){//"    1:          2709     1963112296  [B (java.base@10)"
        return new Exp("gcClassHistoEntry","(?<num>\\d+):\\s+(?<count>\\d+):?\\s+(?<bytes>\\d+)\\s+(?<name>.*)")
                ;
    }
    public Exp gcClassHistoTotal(){//"Total         14175     1963663064"
        return new Exp("gcClassHistoTotal","Total\\s+(?<count>\\d+)\\s+(?<bytes>\\d+)")

                ;
    }
    public Exp gcClassHistoEnd(){//"Class Histogram (before full gc) 18.000ms"
        return new Exp("gcClassHistoEnd","Class Histogram \\((?<phase>\\S+) full gc\\) (?<milliseconds>\\d+\\.\\d{3})ms")
                ;

    }
    public Exp gcId(){//"GC(27)"
        return new Exp("gcId","GC\\((?<gcId:TargetId>\\d+)\\)")
                ;
    }

    //Decorators
    //
    public Exp time(){ //[2018-04-12T09:24:30.397-0500]
        return new Exp("time","\\[(?<time:first>\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3}-\\d{4})\\]")
                .addRule(ExpRule.TargetRoot)
                ;
    }
    public Exp utcTime(){ //[2018-04-12T14:24:30.397+0000]
        return new Exp("utcTime","\\[(?<utcTime:first>\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3}\\+\\d{4})\\]")
                .addRule(ExpRule.TargetRoot)
                ;
    }
    public Exp uptime(){ //[0.179s]
        return new Exp("uptime","\\[(?<uptime:first>\\d+\\.\\d{3})s\\]")
                .addRule(ExpRule.TargetRoot)
                ;
    }
    public Exp timeMillis(){ //[1523543070397ms]
        return new Exp("timeMillis","\\[(?<timeMillis:first>\\d{13})ms\\]")
                .addRule(ExpRule.TargetRoot)
                ;
    }
    public Exp uptimeMillis(){ //[15ms]
        return new Exp("uptimeMillis","\\[(?<uptimeMillis:first>\\d{1,12})ms\\]")
                .addRule(ExpRule.TargetRoot)
                ;
    }
    public Exp timeNanos(){ //[6267442276019ns]
        return new Exp("timeNanos","\\[(?<timeNanos:first>\\d{13,})ns\\]")
                .addRule(ExpRule.TargetRoot)
                ;
    }
    public Exp uptimeNanos(){ //[10192976ns]
        return new Exp("uptimeNanos","\\[(?<uptimeNanos:first>\\d{1,12})ns\\]")
                .addRule(ExpRule.TargetRoot)
                ;
    }
    //TODO hostname,pid,tid,

    public Exp gcExpanding(){//"Expanding tenured generation from 170688K by 39936K to 210624K"
        return new Exp("expand","Expanding (?<region>\\S.+\\S) from (?<from:KMG>\\d+[kKmMgG]?)")
                .add(new Exp("by"," by (?<by:KMG>\\d+[kKmMgG]?)"))
                .add(new Exp("to"," to (?<to:KMG>\\d+[kKmMgG]?)"))
                .with("change","expanding")
                ;
    }
    public Exp gcShrinking(){//"Shrinking ParOldGen from 171008K by 11264K to 159744K"
        return new Exp("shrink","Shrinking (?<region>\\S.+\\S) from (?<from:KMG>\\d+[kKmMgG]?)")
                .add(new Exp("by"," by (?<by:KMG>\\d+[kKmMgG]?)"))
                .add(new Exp("to"," to (?<to:KMG>\\d+[kKmMgG]?)"))
                .with("change","shrinking")
                ;
    }

    //gc+age
    //
    public Exp gcAge(){//"Desired survivor size 4358144 bytes, new threshold 1 (max threshold 6)"
        return new Exp("gcAge","Desired survivor size (?<survivorSize>\\d+) bytes, new threshold (?<threshold>\\d+) \\(max threshold (?<maxThreshold>\\d+)\\)")
                ;
    }
    public Exp gcAgeTableHeader(){//"Age table with threshold 1 (max threshold 6)"
        return new Exp("gcAgeTableHeader","Age table with threshold (?<tableThreshold>\\d+) \\(max threshold (?<tableMaxThreshold>\\d+)\\)")
                ;
    }
    public Exp gcAgeTableEntry(){//"- age   1:    6081448 bytes,    6081448 total"
        return new Exp("gcAgeTableEntry","- age\\s+(?<age>\\d+):\\s+(?<size>\\d+) bytes,\\s+(?<total>\\d+) total")
;
    }



}
