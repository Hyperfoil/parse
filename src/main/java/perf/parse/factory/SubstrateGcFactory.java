package perf.parse.factory;

import perf.parse.Exp;
import perf.parse.ExpMerge;
import perf.parse.ExpRule;
import perf.parse.Parser;

public class SubstrateGcFactory implements ParseFactory{

//PrintGC

//[Incremental GC (CollectOnAllocation.Sometimes) 261108K->1019K, 0.0055645 secs]
   public Exp incrementalGc(){
      return new Exp("incremental","\\[Incremental GC \\((?<cause>[^\\)]+)\\) (?<before:kmg>\\d+[kKmMgG])->(?<after:kmg>\\d+[kKmMgG]), (?<seconds>\\d+\\.\\d{7}) secs\\]");

   }

//VerboseGC
   public Exp policyParametersHeader(){
      return new Exp("policyParametersHeader", "\\[Heap policy parameters:");
   }
   public Exp policyParameter(){
      return new Exp("policyParameter","\\s+(?<policy>[^:]+): (?<value>\\d+)");
   }
   public Exp collectionTime(){
      return new Exp("collectionTime","  collection time: (?<nanoSeconds>\\d+) nanoSeconds\\]\\]");
   }
   public Exp gcPhase(){
      return new Exp("timestamp","\\[(?<timestamp>\\d{15,}) GC: (?<phase>\\S+)\\s{2,}+epoch: (?<epoch:targetId>\\d+)\\s+{2,}")
         .add(new Exp("cause","cause: (?<cause>.*?)(?:\\s{2,}|\\])"))
         .add(new Exp("policy","policy: (?<policy>.*?)(?:\\s{2,}|\\])"))
         .add(new Exp("type","type: (?<type>.*?)(?:\\s{2,}|\\]|$)"));
   }

   @Override
   public void addToParser(Parser p) {
      p.add(policyParametersHeader().enables("policyParameter"));
      p.add(policyParameter()
         .requires("policyParameter")
         .group("parameters")
         .setMerge(ExpMerge.AsEntry)
         .add(new Exp("closeBracket","]").disables("policyParameter"))
      );
      p.add(gcPhase()
         .group("phase")
         .setMerge(ExpMerge.AsEntry)
      );
      p.add(incrementalGc()
         .addRule(ExpRule.PreClose,"cause")
      );
      p.add(collectionTime()
         .addRule(ExpRule.PostClose)
      );
   }

   @Override
   public Parser newParser() {
      Parser parser = new Parser();
      addToParser(parser);
      return parser;
   }

//VerboseGC

/*
[Heap policy parameters:
  YoungGenerationSize: 268435456
      MaximumHeapSize: 13353028800
      MinimumHeapSize: 536870912
     AlignedChunkSize: 1048576
  LargeArrayThreshold: 131072]
[[105503979854228 GC: before  epoch: 1  cause: CollectOnAllocation.Sometimes]
 [105503985983529 GC: after   epoch: 1  cause: CollectOnAllocation.Sometimes  policy: by space and time: 50% in incremental collections  type: incremental
  collection time: 6119125 nanoSeconds]]

[[105511888991426 GC: before  epoch: 2  cause: CollectOnAllocation.Sometimes]
 [105511895880370 GC: after   epoch: 2  cause: CollectOnAllocation.Sometimes  policy: by space and time: 50% in incremental collections  type: incremental
  collection time: 6871815 nanoSeconds]]

[[105536086179961 GC: before  epoch: 5  cause: CollectOnAllocation.Sometimes]
 [105536090483706 GC: after   epoch: 5  cause: CollectOnAllocation.Sometimes  policy: by space and time: 50% in incremental collections  type: incremental
  collection time: 4280867 nanoSeconds]]

[[105568449832090 GC: before  epoch: 9  cause: CollectOnAllocation.Sometimes]
 [105568456438016 GC: after   epoch: 9  cause: CollectOnAllocation.Sometimes  policy: by space and time: 50% in incremental collections  type: incremental
  collection time: 6577196 nanoSeconds]]

 */


/*-XX:+VerboseGC -XX:+PrintGC

[Heap policy parameters:
  YoungGenerationSize: 268435456
      MaximumHeapSize: 13353028800
      MinimumHeapSize: 536870912
     AlignedChunkSize: 1048576
  LargeArrayThreshold: 131072]
[[105820269449889 GC: before  epoch: 1  cause: CollectOnAllocation.Sometimes]
[Incremental GC (CollectOnAllocation.Sometimes) 261108K->1019K, 0.0062683 secs]
 [105820275730890 GC: after   epoch: 1  cause: CollectOnAllocation.Sometimes  policy: by space and time: 50% in incremental collections  type: incremental
  collection time: 6268316 nanoSeconds]]

[[105828483633311 GC: before  epoch: 2  cause: CollectOnAllocation.Sometimes]
[Incremental GC (CollectOnAllocation.Sometimes) 262127K->1019K, 0.0044387 secs]
 [105828488119905 GC: after   epoch: 2  cause: CollectOnAllocation.Sometimes  policy: by space and time: 50% in incremental collections  type: incremental
  collection time: 4438734 nanoSeconds]]


 */
}
