package perf.parse.factory;

import perf.parse.*;
import perf.yaup.HashedLists;
import perf.yaup.json.Json;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public class PrintGcFactory {

    public static void main(String[] args) {
        PrintGcFactory printGcFactory = new PrintGcFactory();
        Jep271Factory jep271Factory = new Jep271Factory();
        Parser p = new Parser();
        Set<String> unparsed = new HashSet<>();
        printGcFactory.addToParser(p);
        jep271Factory.addToParser(p);
        HashedLists<String,String> fileUnparsed = new HashedLists<>();
        File folder = new File("/home/wreicher/perfWork/jdcasey");
        Arrays.asList(folder.listFiles())
        .stream()
        .filter((f)->!f.getName().endsWith(".swp"))//don't parse vi swp files :)
        .filter((f)->f.getName().startsWith("gc.log"))
        //.filter((f)->f.getPath().equals("/home/wreicher/perfWork/gc/jenkins/specjms.amq7.2017-11-14_14-30-32.pid25332.gclog"))
        .forEach((path)->{
            System.out.println(path);
            p.clearUnparsedConsumers();
            Json list = new Json();
            p.addUnparsedConsumer((remainder,original,lineNumber)->{
                if(unparsed.add(remainder)){
                    fileUnparsed.put(path.toString(),remainder);
                }
                //System.out.println("REMAINDER::"+remainder+"::");
            });

            try {
                AtomicInteger lines = new AtomicInteger(0);
                try {
                    Files.lines(path.toPath()).forEach((line) -> {
                        lines.incrementAndGet();
                        Json emit = p.onLine(line);
                        if (emit != null) {
                            list.add(emit);
                            //System.out.println(emit.toString(2));
                        }
                        //System.out.println("||"+line+"||");
                    });
                }catch(UncheckedIOException uioe){
                    System.out.println(path+" "+lines.get());
                }
            Json emit = p.close();
            if(emit!=null){
                list.add(emit);
            }
            } catch (IOException e) {
                e.printStackTrace();
            }
            //System.out.println(Json.typeStructure(list).toString(2));
        });
        fileUnparsed.forEach((path,entries)->{
            System.out.println(path);
            entries.forEach(line->{
                System.out.println("  ||"+line+"||");
            });
            try {
                System.in.read();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

    }
    public Parser newParser() {
        Parser p = new Parser();
        addToParser(p);
        return p;
    }
    public void addToParser(Parser p) {
        p.add(gcMemoryLine()
            .set(Merge.PreClose)
        );
        p.add(gcCommandLine()
            .set(Merge.PreClose)
        );

        p.add(gcTenuringDistribution()
            .set(Rule.TargetRoot)
        );
        p.add(gcTenuringAgeDetails()
            .set(Rule.TargetRoot)
            .group("tenuring")
            .set(Merge.Entry)
            //.key("age")
        );

        p.add(gcAdaptiveSizeStart()
            .group("adaptiveSize")
        );
        p.add(gcAdaptiveSizePolicyAvg()
            .group("adaptiveSize")
        );
        p.add(gcAdaptiveSizePolicyEdenLimits()
            .group("adaptiveSize")
            .group("limits")
        );
        p.add(gcAdaptiveSizePolicyOldGenLimits()
            .group("adaptiveSize")
            .group("limits")
        );
        p.add(gcAdaptiveSizePolicySizes()
            .group("adaptiveSize")
        );
        p.add(gcAdaptiveSizePolicyEdenCosts()
            .group("adaptiveSize")
            .group("costs")
        );
        p.add(gcAdaptiveSizePolicyOldGenCosts()
            .group("adaptiveSize")
            .group("costs")
        );
        p.add(gcAdaptiveSizeStop()
            .group("adaptiveSize")
        );

        //-XX:+UseShenandoahGC
        p.add(gcShenandoahCancelConcurrent()
            .eat(Eat.Line)
        );
        p.add(gcShenandoahDetailsTriggerConcurrent()
            .group("concurrentMark")
        );
        p.add(gcShenandoahDetailsPredictedCset()
            .group("cset")
        );
        p.add(gcShenandoahTimedPhase()
            .set(Merge.PreClose)
        );
        p.add(gcShenandoahResizePhase()
            .set(Merge.PreClose)
        );

        p.add(gcShenandoahDetailsInlineTotalGarbage()
            .set(Merge.PreClose)
        );
        p.add(gcShenandoahDetailsInlineAdaptiveCset()
            .set(Merge.PreClose)
        );

        p.add(gcShenandoahDetailsPeriodicTrigger());

        p.add(gcShenandoahDetailsImmediateGarbage());
        p.add(gcShenandoahDetailsFree());
        p.add(gcShenandoahDetailsCollectGarbage());
        p.add(gcShenandoahDetailsLiveObjects());
        p.add(gcShenandoahDetailsLiveGarbageRatio());
        p.add(gcShenandoahDetailsCapacity());
        p.add(gcShenandoahDetailsUncommitted());

        p.add(gcShenandoahDetailsAdjustingFreeThreshold()
            .group("changeThreshold")
        );

        p.add(gcShenandoahDetailsSoloTotalGarbage());
        p.add(gcShenandoahDetailsSoloResizeTime());
        p.add(gcShenandoahDetailsSoloTime());

        p.add(gcShenandoahStatisticsHeader()
            .set(Merge.PreClose)
        );
        p.add(gcShenandoahStatisticsEntry());
        p.add(gcShenandoahAFStats());
        p.add(gcShenandoahSuccessDegenerated());

        //-XX:+UseG1GC
        p.add(gcG1DetailsNest()
            .eat(Eat.Line)
            .add(gcG1DetailsNestHeapResize()
                .group("heap")
                .key("category")
                .eat(Eat.Line)
                .set(Rule.RepeatChildren)
                .add(gcG1DetailsNestHeapBeforeAfter()
                    .group("heap")
                    .key("category")
                    .set(Rule.PrePopTarget)
                    .set(Rule.PushTarget)
                )
                .add(gcG1DetailsNestHeapResize()
                    .set(Rule.PrePopTarget)
                    .set(Rule.PushTarget)
                )
            )
            .add(gcG1DetailsNestCategory()
                .add(gcG1DetailsNestTime())
                .add(gcG1DetailsNestThreads())
                .add(gcG1DetailsNestMinMax()
                    .add(gcG1DetailsNestSum())
                )
                .add(gcG1DetailsNestUserSys())
                .add(gcG1DetailsNestResize())
                .add(gcG1DetailsNestClose())
            )
        );

        //heap at end of gc or PrintHeapAtGC
        //moved before GC line matching because of (full) matching a reason
        p.add(gcHeapAtGcHeader()//before gcDetailsHeap to match before "Heap" matches
            .enables("printGc-heap")
            .set(Merge.PreClose)
        );
        p.add(gcDetailsHeap()
            .enables("printGc-heap")
            .set(Merge.PreClose)
        );

        p.add(gcDetailsHeapRegion()
            .requires("printGc-heap")
            .key("region")
            .set(Rule.PrePopTarget)
            .set(Rule.PushTarget)
        );
        p.add(gcDetailsHeapSpaceExtraAddress()
            .requires("printGc-heap")
            .key("space")
        );
        p.add(gcDetailsHeapSpace()
            .requires("printGc-heap")
            .key("space")
        );
        p.add(gcDetailsHeapSpaceG1()
            .requires("printGc-heap")
            .group("region-size")
        );
        p.add(gcDetailsHeapMetaSpace()//before HeapMeta because HeapMeta can match HeapMetaSpace
            .requires("printGc-heap")
            .key("space")
        );
        p.add(gcDetailsHeapMeta()
            .requires("printGc-heap")
            .key("region")
            .set(Rule.PrePopTarget)
            .set(Rule.PushTarget)
        );
        p.add(gcHeapAtGcSuffix()
            .disables("printGc-heap")
        );

        //Shenandoah heap at end of gc
        p.add(gcShenandoahDetailsHeapHeader()
            .enables("printGc-heap-shenandoah")
            .disables("printGc-heap")
        );
        p.add(gcShenandoahDetailsHeapTCU()
            .requires("printGc-heap-shenandoah")
        );
        p.add(gcShenandoahDetailsHeapTU()
            .requires("printGc-heap-shenandoah")
        );
        p.add(gcShenandoahDetailHeapRegions()
            .group("regions")
            .requires("printGc-heap-shenandoah")
        );
        p.add(gcShenandoahDetailHeapRegionsActiveTotal()
            .group("regions")
            .requires("printGc-heap-shenandoah")
        );
        p.add(gcShenandoahDetailsHeapStatus()
            .requires("printGc-heap-shenandoah")
        );
        p.add(gcShenandoahDetailsHeapReservedHeader()
            .requires("printGc-heap-shenandoah")
        );
        p.add(gcShenandoahDetailsHeapReserved()
            .group("reserved")
            .requires("printGc-heap-shenandoah")
        );
        p.add(gcShenandoahDetailsHeapVirtualHeader()
            .requires("printGc-heap-shenandoah")
        );
        p.add(gcShenandoahDetailsHeapVirtualCommitted()
            .group("virtual")
            .requires("printGc-heap-shenandoah")
        );
        p.add(gcShenandoahDetailsHeapVirtualReserved()
            .group("virtual")
            .requires("printGc-heap-shenandoah")
        );
        p.add(gcShenandoahDetailsHeapVirtualRange()
            .requires("printGc-heap-shenandoah")
            .group("virtual")
            .group("range")
            .set(Merge.Entry)
        );
        p.add(gcType()
            .disables("printGc-heap")
            .disables("printGc-heap-shenandoah")
            .set(Merge.PreClose)
            .add(gcReason())
            .add(gcG1TimedStep()
                .group("steps")
                .set(Merge.Entry)
                .set(Rule.Repeat)
            )
            .add(gcAdaptiveSizePolicyAverages()
                .group("adaptiveSize")
            )//PrintAdaptiveSizePolicy
            .add(gcG1Phase())
            .add(gcG1Tag()
                .set(Rule.Repeat)
            )
        );


        //This would normally go under gcType to be on same line but PrintTenuringDistribution,PrintAdaptiveSizePolicy can break it to multiple lines
        //needs to be bofore gcDetailsRegionName to prevent gcDetailsRegionName from matching "[Times:'
        p.add(gcDetailsTimes()
            .group("times")
            .set(Rule.TargetRoot)//in case we are in a gcDetailsRegionName that split into multiple lines
        );
        //just gcCmsUsed for "[YG occupancy: 5888335 K (7549760 K)]"
//        p.add(new Exp("gcDetailsCmsOccupancy",
//            "\\[(?<region>\\w+(?:\\s\\w+)): (?<size:KMG>\\d+ [bBkKmMgG]?) \\((?<capacity:KMG>\\d+ [bBkKmMgG]?)\\)\\]"
//            )
//            .group("region")
//            .set(Rule.PreClearTarget,"gcDetailsRegionName")
//        );
        p.add(gcDetailsRegionName()
            .group("region")
            .set(Merge.Entry)
            .set(Rule.PreClearTarget,"gcDetailsRegionName")
            .set(Rule.PushTarget,"gcDetailsRegionName")
            .set(Rule.Repeat)
            .add(gcDetailsRegionWarning())//for (promotion failed)
            .add(gcResize()
                .add(gcDetailsRegionClose()
                    .set(Rule.PostClearTarget,"gcDetailsRegionName")
                    //hack closing GROUP_NAME, should ROOT and GROUP really have separate names?
                    //both auto-close but then other patterns cannot interact with them...
                    //closing GROUP_NAME fixes newParser_serial_gcDetails_prefixed which was putting seconds with Metadata rather than root
                    .set(Rule.PostClearTarget,"gcDetailsRegionName"+Exp.GROUPED_NAME)
                )//hack, one pop should be fine but it appears we need 2? not sure why
            )
            .add(gcCmsUsed()
                .add(gcCmsUsedCloser()
                    .set(Rule.PostClearTarget,"gcDetailsRegionName")
                    .set(Rule.PostClearTarget,"gcDetailsRegionName"+Exp.GROUPED_NAME)
                )//hack to close targets, something is double pushing targets, maybe nest isn't poping correctly?
            )
            .add(gcCmsTimed()
                .group("phases")
                .set(Merge.Entry)
                .set(Rule.Repeat)
            )
            .add( new Exp("gcSecs-2","\\s*, (?<seconds>\\d+\\.\\d{7}) secs\\]")
                    .set(Rule.PostPopTarget,"gcDetailsRegionName")//not sure if named is required here
                )
        );
        p.add(
            new Exp("grouping-resize+size","")
                .set(Rule.RepeatChildren)
                .add(gcResize())
                .add(gcCmsUsed())
                .add(gcSecs()
                    .set(Rule.PostClearTarget,"gcDetailsRegionName")//remove gcDetailsRegionName if still in it
                )

        );

        //commented out because TenuringDistribution puts resize+secs from region onto new line
        //so we need this group to repeat at least once if it matches
//        p.add(gcResize());//hacked on root target because not closing Region in parallel+gcDetails
//        p.add(gcCmsUsed());
//        p.add(gcSecs()
//            .set(Rule.PostClearTarget,"gcDetailsRegionName")//remove gcDetailsRegionName if still in it
//        );


        //end of what would normally be on same line as gcType

        //application timing, before gc line decorators to include decorators in PreClose
        p.add(gcApplicationConcurrent()
            .set(Merge.PreClose)
        );
        p.add(gcApplicationStopped()
            .set(Merge.PreClose)
        );

        //gc line start decorators (that also get strewn throughout the Serial collector gc but we only want the first one
        p.add(gcDateStamps()
            .set(Rule.TargetRoot)//in case we are in a gcDetailsRegionName that split into multiple lines
            .set(Rule.LineStart)
        );
        p.add(gcTimestamp()
            .set(Rule.LineStart)
            .set(Rule.TargetRoot)//in case we are in a gcDetailsRegionName that split into multiple lines
        );//after the other parser to avoid picking out incorrect #.###:
        p.add(gcId()
            .set(Rule.LineStart)
            .set(Rule.TargetRoot)//in case we are in a gcDetailsRegionName that split into multiple lines
        );

    }

    //-XX:+UseShenandoahGC
    //
    public Exp gcShenandoahTimedPhase(){//[Pause Init Mark, 0.590 ms]
        return new Exp("gcShenandoahTimedPhase",
            "\\[(?<phase>\\w+(?:\\s\\w+)*), (?<milliseconds>\\d+\\.\\d+) ms\\]");
    }
    public Exp gcShenandoahResizePhase(){//[Concurrent marking 2809M->2809M(3981M), 1.196 ms]
        return new Exp("gcShenandoahResizePhase",
            "\\[(?<phase>\\w+(?:\\s\\w+)*)\\s{1,2}(?<before:KMG>\\d+[bBkKmMgG])->(?<after:KMG>\\d+[bBkKmMgG])\\((?<capacity:KMG>\\d+[bBkKmMgG])\\), (?<milliseconds>\\d+\\.\\d+) ms\\]");
    }
    public Exp gcShenandoahCancelConcurrent(){//Cancelling concurrent GC: Allocation Failure
        return new Exp("gcShenandoahCancelConcurrent",
            "Cancelling concurrent GC: (?<cancelReason>.*)");
    }
    public Exp gcShenandoahStatisticsHeader(){
        return new Exp("gcShenandoahStatisticsHeader","GC STATISTICS:");
    }
    //footer
    public Exp gcShenandoahStatisticsEntry(){//TODO are they always us (microseconds)?
        //Total Pauses (G)            =     1.02 s (a =   102115 us) (n =    10) (lvls, us =    39648,    56836,    88867,    97656,   229139)
        //  Accumulate Stats          =     0.00 s (a =       10 us) (n =     5) (lvls, us =        4,        4,        4,        4,       19)
        return new Exp("gcShenandoahStatisticsEntry",
            "(?<detail:nestLength>\\s*)(?<name>\\S.+\\S)\\s+=\\s+(?<seconds>\\d+\\.\\d+) s \\(a = \\s+(?<average>\\d+) us\\) \\(n =\\s+(?<count>\\d+)\\) \\(lvls, us =\\s+(?<lvl0>\\d+),\\s+(?<lvl25>\\d+),\\s+(?<lvl50>\\d+),\\s+(?<lvl75>\\d+),\\s+(?<lvl100>\\d+)\\)");
    }
    //footer
    public Exp gcShenandoahAFStats(){//4 allocation failure and 0 user requested GCs
        return new Exp("gcShenandoahAFStats",
            "(?<allocationFailure>\\d+) allocation failure and (?<systemGc>\\d+) user requested GCs");
    }
    //footer
    public Exp gcShenandoahSuccessDegenerated(){//5 successful and 0 degenerated concurrent markings
        return new Exp("gcShenandoahSuccessDegenerated",
            "(?<successful>\\d+) successful and (?<degenerated>\\d+) degenerated (?<phase>.*)");
    }
    //Shenandoah -XX:PrintGcDetails
    //
    public Exp gcShenandoahDetailsInlineTotalGarbage(){//1.405: #0: [Pause Final MarkTotal Garbage: 6M
        return new Exp("gcShenandoahDetailsInlineTotalGarbage",
            "\\[(?<phase>\\w+(?:\\s\\w+?)*)Total Garbage: (?<totalGarbage:KMG>\\d+[bBkKmMgG])");
    }
    public Exp gcShenandoahDetailsInlineAdaptiveCset(){
        //[Pause Final MarkAdaptive CSet selection: free target = 6451M, actual free = 7432M; min cset = 0M, max cset = 5574M
        return new Exp("gcShenandoahDetailsInlineAdaptiveCset",
            "\\[(?<phase>\\w+(?:\\s\\w+?)*)Adaptive CSet selection: free target = (?<targetFree:KMG>\\d+[bBkKmMgG]), actual free = (?<actualFree:KMG>\\d+[bBkKmMgG]); min cset = (?<minCset:KMG>\\d+[bBkKmMgG]), max cset = (?<maxCset:KMG>\\d+[bBkKmMgG])")
        ;
    }
    public Exp gcShenandoahDetailsSoloTotalGarbage(){
        return new Exp("gcShenandoahDetailsSoloTotalGarbage",
            "^Total Garbage: (?<totalGarbage:KMG>\\d+[bBkKmMgG])$");
    }
    public Exp gcShenandoahDetailsImmediateGarbage(){//Immediate Garbage: 1723M, 1725 regions (99% of total)
        return new Exp("gcShenandoahDetailsImmediateGarbage",
            "Immediate Garbage: (?<immediateGarbage:KMG>\\d+[bBkKmMgG]), (?<regions>\\d+) regions \\((?<percent>\\d+)% of total\\)");
    }
    public Exp gcShenandoahDetailsFree(){//Free: 1723M, 1725 regions (99% of total)
        return new Exp("gcShenandoahDetailsFree",
            "Free: (?<free:KMG>\\d+[bBkKmMgG]), (?<regions>\\d+) regions \\((?<percent>\\d+)% of total\\)");
    }

    public Exp gcShenandoahDetailsCollectGarbage(){
        //Garbage to be collected: 1M (100% of total), 2 regions
        return new Exp("gcShenandoahDetailsCollectGarbage",
            "Garbage to be collected: (?<collectingGarbage:KMG>\\d+[bBkKmMgG]) \\((?<garbagePercent>\\d+)% of total\\), (?<regions>\\d+) regions");
    }
    public Exp gcShenandoahDetailsLiveObjects(){//Live objects to be evacuated: 0M
        return new Exp("gcShenandoahDetailsLiveObjects",
            "Live objects to be evacuated: (?<liveObjects:KMG>\\d+[bBkKmMgG])");
    }
    public Exp gcShenandoahDetailsLiveGarbageRatio(){//Live/garbage ratio in collected regions: 18%
        return new Exp("gcShenandoahDetailsLiveGarbageRatio",
            "Live/garbage ratio in collected regions: (?<liveGarbageRatio>\\d+)%");
    }
    public Exp gcShenandoahDetailsSoloResizeTime(){// 2809M->1094M(3981M), 0.445 ms]
        return new Exp("gcShenandoahDetailsSoloResizeTime",
            "^ (?<before:KMG>\\d+[bBkKmMgG])->(?<after:KMG>\\d+[bBkKmMgG])\\((?<capacity:KMG>\\d+[bBkKmMgG])\\), (?<milliseconds>\\d+\\.\\d+) ms\\]$");
    }
    public Exp gcShenandoahDetailsSoloTime(){//", 2.832 ms]"
        return new Exp("gcShenandoahDetailsSoloTime","^, (?<milliseconds>\\d+\\.\\d+) ms\\]$");

    }
    public Exp gcShenandoahDetailsAdjustingFreeThreshold(){//Adjusting free threshold to: 50% (1990M)
        return new Exp("gcShenandoahDetailsAdjustingFreeThreshold",
                "Adjusting free threshold to: (?<percent>\\d+)% \\((?<size:KMG>\\d+[bBkKmMgG])\\)")
                ;
    }
    public Exp gcShenandoahDetailsTriggerConcurrent(){
        //Concurrent marking triggered. Free: 1796M, Free Threshold: 1990M; Allocated: 1796M, Alloc Threshold: 0M
        return new Exp("gcShenandoahDetailsTriggerConcurrent",
        "Concurrent marking triggered. Free: (?<free:KMG>\\d+[bBkKmMgG]), Free Threshold: (?<freeThreshold:KMG>\\d+[bBkKmMgG]); Allocated: (?<allocated:KMG>\\d+[bBkKmMgG]), Alloc Threshold: (?<allocThreshold:KMG>\\d+[bBkKmMgG])")
                ;
    }
    public Exp gcShenandoahDetailsPeriodicTrigger(){
        //Periodic GC triggered. Time since last GC: 300004 ms, Guaranteed Interval: 300000 ms
        return new Exp("gcShenandoahDetailsPeriodicTrigger",
                "Periodic GC triggered. Time since last GC: (?<interval>\\d+) ms, Guaranteed Interval: (?<guarantee>\\d+) ms");

    }
    public Exp gcShenandoahDetailsPredictedCset(){
        //"Predicted cset threshold: 40, 12248961K CSet (48%)"
        return new Exp("gcShenandoahDetailsPredictedCset",
                "Predicted cset threshold: (?<threshold>\\d+), (?<size:KMG>\\d+[bBkKmMgG]) CSet \\((?<percentage>\\d+)%\\)")
                ;
    }
    public Exp gcShenandoahDetailsCapacity(){
        //"Capacity: 18432M, Peak Occupancy: 5373M, Lowest Free: 13059M, Free Threshold: 552M"
        return new Exp("gcShenandoahDetailsCapacity",
        "Capacity: (?<capacity:KMG>\\d+[bBkKmMgG]), Peak Occupancy: (?<peakOccupancy:KMG>\\d+[bBkKmMgG]), Lowest Free: (?<lowestFree:KMG>\\d+[bBkKmMgG]), Free Threshold: (?<freeThreshold:KMG>\\d+[bBkKmMgG])")
        ;
    }
    public Exp gcShenandoahDetailsUncommitted(){
        //"Uncommitted 12936M. Heap: 18432M reserved, 5496M committed, 165M used"
        return new Exp("gcShenandoahDetailsUncommitted",
            "Uncommitted (?<uncommitted:KMG>\\d+[bBkKmMgG]). Heap: (?<reserved:KMG>\\d+[bBkKmMgG]) reserved, (?<committed:KMG>\\d+[bBkKmMgG]) committed, (?<used:KMG>\\d+[bBkKmMgG]) used");
    }
    //Shenadoah heap
    public Exp gcShenandoahDetailsHeapHeader(){
        return new Exp("gcShenandoahDetailsHeapHeader","^Shenandoah Heap$");
    }
    public Exp gcShenandoahDetailsHeapTU(){
        //18874368K total, 7313022K used
        return new Exp("gcShenandoahDetailsHeapTU",
            "(?<total:KMG>\\d+[bBkKmMgG]) total, (?<used:KMG>\\d+[bBkKmMgG]) used");
    }
    public Exp gcShenandoahDetailsHeapTCU(){
        return new Exp("gcShenandoahDetailsHeapTCU",
            "\\s*(?<total:KMG>\\d+[bBkKmMgG]) total, (?<committed:KMG>\\d+[bBkKmMgG]) committed, (?<used:KMG>\\d+[bBkKmMgG]) used");
    }
    public Exp gcShenandoahDetailHeapRegions(){
        return new Exp("gcShenandoahDetailHeapRegions",
            "\\s*(?<count>\\d+) x (?<size:KMG>\\d+[bBkKmMgG]) regions");
    }
    public Exp gcShenandoahDetailHeapRegionsActiveTotal(){
        return new Exp("gcShenandoahDetailHeapRegionsActiveTotal",
            "\\s*(?<size:KMG>\\d+[bBkKmMgG]) regions, (?<active>\\d+) active, (?<total>\\d+) total");
    }
    public Exp gcShenandoahDetailsHeapStatus(){
        return new Exp("gcShenandoahDetailsHeapStatus",
            "^Status: (?<status>.*\\S)\\s*$");
    }
    public Exp gcShenandoahDetailsHeapReservedHeader() {
        return new Exp("gcShenandoahDetailsHeapReservedHeader", "Reserved region:").eat(Eat.Line);
    }
    public Exp gcShenandoahDetailsHeapReserved(){
        return new Exp("gcShenandoahDetailsHeapReserved",
            " - \\[(?<start>0x[0-9a-f]+), (?<end>0x[0-9a-f]+)\\)");
    }
    public Exp gcShenandoahDetailsHeapVirtualHeader(){
        return new Exp("gcShenandoahDetailsHeapVirtualHeader","^Virtual space: \\(pinned in memory\\)");
    }
    public Exp gcShenandoahDetailsHeapVirtualCommitted(){
        //" - committed: 19327352832"
        return new Exp("gcShenandoahDetailsHeapVirtualCommitted",
            "\\s*- committed:\\s+(?<committed:KMG>\\d+[bBkKmMgG]?)");
    }
    public Exp gcShenandoahDetailsHeapVirtualReserved(){
        //" - reserved:  19327352832"
        return new Exp("gcShenandoahDetailsHeapVirtualReserved",
            "\\s*- reserved:\\s+(?<reserved:KMG>\\d+[bBkKmMgG]?)");
    }
    public Exp gcShenandoahDetailsHeapVirtualRange(){
        //" - [low_b, high_b]: [0x0000000340000000, 0x00000007c0000000]"
        return new Exp("gcShenandoahDetailsHeapVirtualRange",
            "- \\[(?<first>[^,]+), (?<second>[^\\]]+)\\]:\\s*\\[(?<start>0x[0-9a-f]+), (?<end>0x[0-9a-f]+)\\]")
            .set("first","start")
            .set("second","end");
    }

    //-XX:+UnlockDiagnosticVMOptions -XX:+ShenandoahAllocationTrace
    //
    public Exp gcShenandoahAllocationTraceLatencyHeader(){
        return new Exp("gcShenandoahAllocationTraceLatencyHeader","Latencies \\(in microseconds\\):");
    }
    public Exp gcShenandoahAllocationTraceSizeHeader(){
        return new Exp("gcShenandoahAllocationTraceSizeHeader","Sizes \\(in bytes\\):");
    }
    //TODO ask openJDK what on eart these numbers mean
    public Exp gcShenandoahAllocationTraceEntry(){//      0 -       1:           3           0           0           3
        return new Exp("gcShenandoahAllocationTraceEntry",
        "\\s*(?<first>\\d+) -\\s+(?<second>\\d+):\\s+(?<shared>\\d+)\\s+(?<sharedGc>\\d+)\\s+(?<tlab>\\d+)\\s+(?<gclab>\\d+)"
        );
    }

    //-XX:+PrintAdaptiveSizePolicy, also inject into the middle of [GC (reason)...
    //
    public Exp gcAdaptiveSizePolicyAverages(){//AdaptiveSizePolicy::update_averages:  survived: 1023701448  promoted: 8192  overflow: false
        //guessing that overlow is true or false
        return new Exp("gcAdaptiveSizePolicyAverages","AdaptiveSizePolicy::update_averages:  survived: (?<survived>\\d+)\\s+promoted: (?<promoted>\\d+)\\s+overflow: (?<overflow>true|false)");
    }
    public Exp gcAdaptiveSizeStart(){//AdaptiveSizeStart: 238.944 collection: 11
        return new Exp("gcAdaptiveSizeStart","AdaptiveSizeStart: \\d+\\.\\d{3} collection: (?<collection>\\d+)");
    }

    public Exp gcAdaptiveSizePolicyAvg(){//  avg_survived_padded_avg: 1233046272.000000  avg_promoted_padded_avg: 87401392.000000  avg_pretenured_padded_avg: 0.000000  tenuring_thresh: 2  target_size: 2147483648
        return new Exp("gcAdaptiveSizePolicyAvg","avg_survived_padded_avg: (?<avgSurvivedPadded>\\d+\\.\\d{6})  avg_promoted_padded_avg: (?<avgPromotedPadded>\\d+\\.\\d{6})  avg_pretenured_padded_avg: (?<avgPretenuredPadded>\\d+\\.\\d{6})  tenuring_thresh: (?<tenuringThreshold>\\d+)  target_size: (?<targetSize>\\d+)");
    }

    public Exp gcAdaptiveSizePolicyEdenLimits(){//PSAdaptiveSizePolicy::compute_eden_space_size limits: desired_eden_size: 13679211440 old_eden_size: 8589934592 eden_limit: 8589934592 cur_eden: 8589934592 max_eden_size: 8589934592 avg_young_live: 273553920
        return new Exp("gcAdaptiveSizePolicyEdenLimits",
            "PSAdaptiveSizePolicy::compute_eden_space_size limits: desired_eden_size: (?<desiredEdenSize>\\d+) old_eden_size: (?<oldEdenSize>\\d+) eden_limit: (?<edenLimit>\\d+) cur_eden: (?<currentEden>\\d+) max_eden_size: (?<maxEdenSize>\\d+) avg_young_live: (?<avgYoungLive>\\d+)");
    }
    //TODO combine patterns above and below into a single parent + children pattern?
    public Exp gcAdaptiveSizePolicyOldGenLimits(){
        //PSAdaptiveSizePolicy::compute_old_gen_free_space limits: desired_promo_size: 4402127704 promo_limit: 4294967296 free_in_old_gen: 4282070528 max_old_gen_size: 4294967296 avg_old_live: 12896847
        return new Exp("gcAdaptiveSizePolicyOldGenLimits",
            "PSAdaptiveSizePolicy::compute_old_gen_free_space limits: desired_promo_size: (?<desiredPromoSize>\\d+) promo_limit: (?<promoLimit>\\d+) free_in_old_gen: (?<freeOldGen>\\d+) max_old_gen_size: (?<maxOldGenSize>\\d+) avg_old_live: (?<avgOldLive>\\d+)");
    }

    public Exp gcAdaptiveSizePolicySizes(){
        //AdaptiveSizePolicy::survivor space sizes: collection: 12 (2147483648, 1073741824) -> (2147483648, 2147483648)
        return new Exp("gcAdaptiveSizePolicySizes","AdaptiveSizePolicy::survivor space sizes: collection: (?<collection>\\d+) \\((?<beforeA>\\d+), (?<beforeB>\\d+)\\) -> \\((?<afterA>\\d+), (?<afterB>\\d+)\\)");
    }

    public Exp gcAdaptiveSizePolicyEdenCosts(){
        //PSAdaptiveSizePolicy::compute_eden_space_size: costs minor_time: 0.141031 major_cost: 0.097008 mutator_cost: 0.761961 throughput_goal: 0.990000 live_space: 570756352 free_space: 12884901888 old_eden_size: 8589934592 desired_eden_size: 8589934592
        return new Exp("gcAdaptiveSizePolicyEdenCosts",
            "PSAdaptiveSizePolicy::compute_eden_space_size: costs minor_time: (?<minorTime>\\d+\\.\\d+) major_cost: (?<majorCost>\\d+\\.\\d+) mutator_cost: (?<mutatorCost>\\d+\\.\\d+) throughput_goal: (?<throughputGoal>\\d+\\.\\d+) live_space: (?<liveSpace>\\d+) free_space: (?<freeSpace>\\d+) old_eden_size: (?<oldEdenSize>\\d+) desired_eden_size: (?<desiredEdenSize>\\d+)");
    }
    //TODO replace above and below Exp with common Exp that nests by "PSAdaptiveSizePolicy::<name> costs" and has child Exp for name,value pairs
    public Exp gcAdaptiveSizePolicyOldGenCosts(){
        //PSAdaptiveSizePolicy::compute_old_gen_free_space: costs minor_time: 0.567664 major_cost: 0.014526 mutator_cost: 0.417810 throughput_goal: 0.990000 live_space: 292574784 free_space: 10737418240 old_promo_size: 4294967296 desired_promo_size: 4294967296
        return new Exp("gcAdaptiveSizePolicyOldGenCosts",
            "PSAdaptiveSizePolicy::compute_old_gen_free_space: costs minor_time: (?<minorTime>\\d+\\.\\d+) major_cost: (?<majorCost>\\d+\\.\\d+) mutator_cost: (?<mutatorCost>\\d+\\.\\d+) throughput_goal: (?<throughputGoal>\\d+\\.\\d+) live_space: (?<liveSpace>\\d+) free_space: (?<freeSpace>\\d+) old_promo_size: (?<oldPromoSize>\\d+) desired_promo_size: (?<desiredPromoSize>\\d+)");
    }
    public Exp gcAdaptiveSizeStop(){//AdaptiveSizeStop: collection: 12
        return new Exp("gcAdaptiveSizeStop","AdaptiveSizeStop: collection: (?<collection>\\d+)");
    }

    //PrintGCDateStamps
    public Exp gcDateStamps(){//2018-04-17T10:42:28.747-0500 | 2018-05-07T17:34:40.035+0000
        return new Exp("gcDateStamps","(?<datestamp:first>\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3}[-+]\\d{4}):\\s+");
    }

    //PrintGCTimeStamps
    public Exp gcTimestamp(){//0.071:
        return new Exp("gcTimestamp","(?<timestamp:first>\\d+\\.\\d{3}):\\s*");
    }
    //PrintGCID
    public Exp gcId(){//#1
        return new Exp("gcId","#(?<gcId:first>\\d+):\\s*");
    }

    //PrintGC
    //
    public Exp gcMemoryLine(){//Memory: 4k page, physical 263842984k(199299872k free), swap 4194300k(4194300k free)
        return new Exp("gcMemory","Memory: (?<page:KMG>\\d+[bBkKmMgG]) page, physical (?<physical:KMG>\\d+[bBkKmMgG])\\((?<physicalFree:KMG>\\d+[bBkKmMgG]) free\\), swap (?<swap:KMG>\\d+[bBkKmMgG])\\((?<swapFree:KMG>\\d+[bBkKmMgG]) free\\)");
    }
    public Exp gcCommandLine(){//"CommandLine flags: -XX:InitialHeapSize=260834624 -XX:MaxHeapSize=4173353984 -XX:+PrintGC -XX:+PrintGCTimeStamps -XX:+UseCompressedClassPointers -XX:+UseCompressedOops -XX:+UseSerialGC "
        return new Exp("gcCommandLine","^CommandLine flags: (?<commandLine>.+)$");
    }
    public Exp gcType(){//GC | Full GC (Serial|Parallel|CMS)
        //added pause\\s? for G1GC "0.118: [GC pause (G1 Humongous Allocation) (young) (initial-mark) 98M->19M(250M), 0.0033460 secs]"
        return new Exp("gcType","\\[(?<type>GC|Full\\sGC) ");
    }
    public Exp gcReason(){// (G1 Humongous Allocation)
        //added (?:\\(\\))? for System.gc()
        return new Exp("gcReason","\\((?<reason>\\D[^(]+(?:\\(\\))?)\\)");//added \\D to prevent it from matching size in resize 10M->0M(250M)
    }
    public Exp gcG1Phase(){//G1GC

        return new Exp("g1Phase","^(?<phase>[\\w\\-]+?)(?=[\\s,\\]])");
    }
    public Exp gcResize(){//61852K->15323K(247488K)
        return new Exp("gcResize","\\s*(?<before:KMG>\\d+[KMG])->(?<after:KMG>\\d+[KMG])\\((?<capacity:KMG>\\d+[KMG])\\)");
    }
    public Exp gcSecs(){
        return new Exp("gcSecs","\\s*, (?<seconds>\\d+\\.\\d{7}) secs\\]");
    }

    // UseConcMarkSweep
    public Exp gcCmsUsed(){//279891K(370368K) | 0 K (180032 K)
        return new Exp("gcCmsUsed","\\s(?<used:KMG>\\d+\\s?[KMG])\\s?\\((?<capacity:KMG>\\d+\\s?[KMG])\\)");
    }
    public Exp gcCmsUsedCloser(){
        return new Exp("gcCmsusedCloser","^\\]?");
    }
    // UseConcMarkSweep
    public Exp gcCmsTimed(){// [Rescan (parallel) , 0.0004003 secs]
        return new Exp("gcTimedPhase","\\[(?<phase>\\S[^:]*?\\S)\\s*,\\s*(?<secs>\\d+\\.\\d{7}) secs]");
    }

    //PrintTenuringDistribution
    public Exp gcTenuringDistribution(){//"Desired survivor size 2097152 bytes, new threshold 7 (max 15)"
        return new Exp("gcTenuringDistribution","Desired survivor size (?<survivorSize>\\d+) bytes, new threshold (?<threshold>\\d+) \\(max (?<maxThreshold>\\d+)\\)");
    }
    public Exp gcTenuringAgeDetails(){//"- age   1:    4606400 bytes,    4606400 total"
        return new Exp("gcTenuringAgeDetails","- age\\s+(?<age>\\d+):\\s+(?<size>\\d+) bytes,\\s+(?<total>\\d+) total");
    }
    //G1GC
    //
    public Exp gcG1Tag(){
        //added ^ to prevent it from eating (promotion failed) from CMS
        return new Exp("g1Tag","^\\s+\\((?<g1Tag>[^\\(]+)\\)");
    }
    public Exp gcG1TimedStep(){
        return new Exp("g1TimedStep","\\[(?<step>\\w+(?:\\s[\\w-]+)*), (?<seconds>\\d+\\.\\d{7}) secs\\]");
    }
    public Exp gcG1DetailsNest(){//[Parallel Time: | Ext Root Scanning (ms):
        return new Exp("g1DetailsNest","^(?<detail:nestLength>\\s+)\\[");//\\[(?<category>[^:]+):");
    }
    public Exp gcG1DetailsNestHeapResize(){//"[Eden: 1024.0K(16.0M)->0.0B(183.0M) Survivors: 1024.0K->1024.0K Heap: 3666.8M(3682.0M)->936.5M(3682.0M)]"
        return new Exp("g1DetailsNestHeapResize","^(?<category>\\w+): (?<before>\\d+\\.\\d+[BKMG])\\((?<beforeSize>\\d+\\.\\d+[BKMG])\\)->(?<after>\\d+\\.\\d+[BKMG])\\((?<afterSize>\\d+\\.\\d+[BKMG])\\)[\\s\\]]");
    }
    public Exp gcG1DetailsNestHeapBeforeAfter(){
        return new Exp("g1DetailsNestHeapResize","^(?<category>\\w+): (?<before>\\d+\\.\\d+[BKMG])->(?<after>\\d+\\.\\d+[BKMG])[\\s\\]]");
    }
    public Exp gcG1DetailsNestCategory(){
        return new Exp("g1DetailsNestCategory","^(?<category>\\w[^:]+):");
    }
    public Exp gcG1DetailsNestTime(){//" 1.6 ms"
        return new Exp("g1DetailsNestTime"," (?<millliseconds>\\d+\\.\\d+) ms");
    }
    public Exp gcG1DetailsNestThreads(){//" GC Workers: 4"
        return new Exp("g1DetailsNestThreads"," GC Workers: (?<workers>\\d+)");
    }
    public Exp gcG1DetailsNestMinMax(){// Min: 104.7, Avg: 104.8, Max: 105.0, Diff: 0.3
        return new Exp("g1DetailsNestMinMax"," Min: (?<min>\\d+\\.?\\d*), Avg: (?<avg>\\d+\\.?\\d*), Max: (?<max>\\d+\\.?\\d*), Diff: (?<diff>\\d+\\.?\\d*)");
    }
    public Exp gcG1DetailsNestSum(){
        return new Exp("g1DetailsNestSum",", Sum: (?<sum>\\d+\\.?\\d*)");
    }
    public Exp gcG1DetailsNestUserSys(){//" user=0.01 sys=0.00, real=0.00 secs"
        return new Exp("g1DetailsNestuserSys"," user=(?<user>\\d+\\.\\d+) sys=(?<sys>\\d+\\.\\d+), real=(?<real>\\d+\\.\\d+) secs");
    }
    public Exp gcG1DetailsNestResize(){//" 4096.0K(14.0M)->0.0B(16.0M)"
        return new Exp("g1DetailsNestResize"," (?<before>\\d+\\.\\d+[KMG])\\((?<beforeSize>\\d+\\.\\d+[KMG])\\)->(?<after>\\d+\\.\\d+[KMG])\\((?<afterSize>\\d+\\.\\d+[KMG])\\)");
    }
    public Exp gcG1DetailsNestPeerCategory(){//" Survivors:"
        return new Exp("g1DetailsNestPeerCategory"," (?<category>\\w+):");
    }
    public Exp gcG1DetailsNestClose(){
        return new Exp("g1DetailsNestClose","\\]");
    }

    //PrintGCDetails
    //
    public Exp gcDetailsRegionName(){//[DefNew:|[Tenured:|[Metaspace:|[Finalize Marking|[ParNew$|[ParNew
        //changed : to (?::|\z) to deal with lines broken by PrintTenuringDistribution in newParser_cms_ParNew_tenuringDistribution
        //changed (?::|\z) to (?::|\z|(?=\s\()) because of ParNew (promotion failed) in newParser_cms_promotionFailed
        return new Exp("gcDetailsRegionName","\\[(?<region>\\w+(?:\\s[\\w-]+)*)(?::|\\z|(?=\\s\\())")
        ;
    }
    public Exp gcDetailsRegionWarning(){
        return new Exp("gcDetailsRegionWarning","^\\s*\\((?<warning>[^\\)]+)\\):");
    }
    public Exp gcDetailsRegionClose(){//"\\["
        return new Exp("gcDetailsRegionClose","^\\]");
    }
    public Exp gcDetailsTimes(){//[Times: user=0.08 sys=0.00, real=0.08 secs]
        return new Exp("gcDetailsTimes","\\[Times: user=(?<user>\\d+\\.\\d+),? sys=(?<sys>\\d+\\.\\d+),? real=(?<real>\\d+\\.\\d+) secs]");
    }
    //heap at end of gc file or PrintHeapAtGC
    public Exp gcDetailsHeap(){//Heap
        return new Exp("gcDetailsHeap","^Heap$");
    }
    public Exp gcHeapAtGcHeader(){
        return new Exp("gcHeapAtGcHeader","\\{?Heap (?<phase>before|after) GC invocations=(?<gcCount>\\d+) \\(full (?<fullCount>\\d+)\\):");
    }
    public Exp gcHeapAtGcSuffix(){
        return new Exp("gcHeapAtGcSuffix","^}$");
    }
    public Exp gcDetailsHeapRegion(){//def new generation   total 1223296K, used 43469K [0x00000006c7200000, 0x000000071a150000, 0x000000071a150000)
        return new Exp("gcDetailsHeapRegion",
        "(?<region>\\S.+\\S)\\s+total (?<total:KMG>\\d+[KMG]), used (?<used:KMG>\\d+[KMG])"+
            "\\s+\\[(?<start>0x[0-9a-fA-F]+), (?<current>0x[0-9a-fA-F]+), (?<end>0x[0-9a-fA-F]+)\\)"
        );
    }
    public Exp gcDetailsHeapSpace(){//eden space 1087424K,   3% used [0x00000006c7200000, 0x00000006c9c73588, 0x00000007097f0000)
        return new Exp("gcDetailsHeapSpace",
        "(?<space>\\w+)\\s+space (?<size:KMG>\\d+[KMG]),\\s+(?<percentUsed>\\d+)% used"+
            "\\s+\\[(?<start>0x[0-9a-fA-F]+),\\s?(?<current>0x[0-9a-fA-F]+),\\s?(?<end>0x[0-9a-fA-F]+)\\)"
        );//serail uses spaces between addresses, parallel does not
    }
    public Exp gcDetailsHeapSpaceExtraAddress(){//"   the space 2718400K,  70% used [0x000000071a150000, 0x000000078f1a0538, 0x000000078f1a0600, 0x00000007c0000000)"
        return new Exp("gcDetailsHeapSpaceExtraAddress",
        "(?<space>\\w+)\\s+space (?<size:KMG>\\d+[KMG]),\\s+(?<percentUsed>\\d+)% used"+
            "\\s+\\[(?<start>0x[0-9a-fA-F]+), (?<currentA>0x[0-9a-fA-F]+), (?<currentB>0x[0-9a-fA-F]+), (?<end>0x[0-9a-fA-F]+)\\)"
        );
    }
    public Exp gcDetailsHeapSpaceG1(){//"   region size 1024K, 5 young (5120K), 0 survivors (0K)"
        return new Exp("gcDetailsHeapSpaceG1","region size (?<regionSize:KMG>\\d+[bBkKmMgG]), (?<youngCount>\\d+) young \\((?<youngSize:KMG>\\d+[bBkKmMgG])\\), (?<survivorCount>\\d+) survivors \\((?<survivorSize:KMG>\\d+[bBkKmMgG])\\)");
    }
    public Exp gcDetailsHeapMeta(){//Metaspace       used 2967K, capacity 4486K, committed 4864K, reserved 1056768K
        return new Exp("gcDetailsHeapMeta","(?<region>\\w+)\\s+used (?<used:KMG>\\d+[KMG]), capacity (?<capacity:KMG>\\d+[KMG]), committed (?<committed:KMG>\\d+[KMG]), reserved (?<reserved:KMG>\\d+[KMG])");
    }
    public Exp gcDetailsHeapMetaSpace(){//class space    used 321K, capacity 386K, committed 512K, reserved 1048576K
        return new Exp("gcDetailsHeapMetaSpace","(?<space>\\w+)\\s+space\\s+used (?<used:KMG>\\d+[KMG]), capacity (?<capacity:KMG>\\d+[KMG]), committed (?<committed:KMG>\\d+[KMG]), reserved (?<reserved:KMG>\\d+[KMG])");
    }
    //PrintGCApplicationConcurrentTime
    public Exp gcApplicationConcurrent(){
        return new Exp("gcApplicationConcurrent","Application time: (?<applicationConcurrent>\\d+\\.\\d{7}) seconds");
    }
    //PrintGCApplicationStoppedTime
    public Exp gcApplicationStopped(){
        return new Exp("gcApplicationStopped","Total time for which application threads were stopped: (?<applicationStopped>\\d+\\.\\d{7}) seconds, Stopping threads took: (?<threadStopping>\\d+\\.\\d{7}) seconds");
    }


}
