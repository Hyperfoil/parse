package perf.parse.factory;


import org.junit.BeforeClass;
import org.junit.Test;
import perf.parse.Exp;
import perf.parse.Parser;
import perf.parse.Value;
import perf.parse.internal.CheatChars;

import static org.junit.Assert.assertEquals;

/**
 *
 */
public class OpenJdkGcFactoryTest {


    private static OpenJdkGcFactory f;


    @BeforeClass
    public static void staticInit(){
        f = new OpenJdkGcFactory();

    }
    @Test
    public void heap(){
        Parser p = f.newGcParser();

        p.onLine(new CheatChars("2015-03-24T13:20:27.638-0400: 0.975: [Full GC (Metadata GC Threshold) [PSYoungGen: 10047K->0K(8388608K)] [ParOldGen: 24K->9138K(2097152K)] 10071K->9138K(10485760K), [Metaspace: 19144K->19144K(1067008K)], 0.0357059 secs] [Times: user=0.46 sys=0.03, real=0.04 secs] \n"));

    }

    @Test
    public void timestamp(){
        Parser p = new Parser();
        p.add(f.newTimestampPattern());
        p.onLine(new CheatChars("2015-02-03T21:55:06.443-0500: 1.290: [Full GC (Metadata GC Threshold) AdaptiveSizeStart: 1.334 collection: 3 \n"));

        assertEquals("2015-02-03T21:55:06.443-0500",p.getBuilder().getRoot().getString("timestamp"));
    }

    @Test
    public void region(){
        Parser p = new Parser();
        p.add(f.newRegionPattern());

        p.onLine(new CheatChars("[PSYoungGen: 13873K->0K(8388608K)] [ParOldGen: 24K->12771K(2097152K)] 13897K->12771K(10485760K), [Metaspace: 19024K->19024K(1067008K)], 0.0440153 secs] [Times: user=0.69 sys=0.02, real=0.05 secs]"));



    }

    @Test
    public void combined(){
        Parser p = new OpenJdkGcFactory().newGcParser();

        p.add(new Exp("tenured", "PERF tenuring (?<class>\\S+) ").group("tenured").set("class", Value.Count));

        String toTest[] = new String[]{
            "2015-02-03T21:55:06.443-0500: 1.290: [Full GC (Metadata GC Threshold) AdaptiveSizeStart: 1.334 collection: 3 \n" ,
            "PSAdaptiveSizePolicy::compute_eden_space_size limits: desired_eden_size: 9574551008 old_eden_size: 6442450944 eden_limit: 6442450944 cur_eden: 6442450944 max_eden_size: 6442450944 avg_young_live: 7334511\n" ,
            "PSAdaptiveSizePolicy::compute_eden_space_size: costs minor_time: 0.031639 major_cost: 0.033440 mutator_cost: 0.934921 throughput_goal: 0.990000 live_space: 275769952 free_space: 8589934592 old_eden_size: 6442450944 desired_eden_size: 6442450944\n" ,
            "PSAdaptiveSizePolicy::compute_old_gen_free_space limits: desired_promo_size: 3250933941 promo_limit: 2147483648 free_in_old_gen: 2134406016 max_old_gen_size: 2147483648 avg_old_live: 13077664\n" ,
            "PSAdaptiveSizePolicy::compute_old_gen_free_space: costs minor_time: 0.031639 major_cost: 0.033440 mutator_cost: 0.934921 throughput_goal: 0.990000 live_space: 288847616 free_space: 8589934592 old_promo_size: 2147483648 desired_promo_size: 2147483648\n" ,
            "AdaptiveSizeStop: collection: 3 \n" ,
            "[PSYoungGen: 13873K->0K(8388608K)] [ParOldGen: 24K->12771K(2097152K)] 13897K->12771K(10485760K), [Metaspace: 19024K->19024K(1067008K)], 0.0440153 secs] [Times: user=0.69 sys=0.02, real=0.05 secs]",
            "PERF tenuring java.lang.String 3\n",
            "PERF tenuring java.lang.String 3\n",
            "PERF tenuring java.lang.Class 12\n",
        };

        for(String line : toTest){
            p.onLine(new CheatChars(line));
        }


    }
}
