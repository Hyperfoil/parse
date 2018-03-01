package perf.parse.factory;


import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import perf.parse.Exp;
import perf.parse.internal.CheatChars;
import perf.parse.internal.JsonBuilder;
import perf.yaup.json.Json;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * 
 */
public class JStackFactoryTest {

    private static JStackFactory f;

    @BeforeClass
    public static void staticInit(){
        f = new JStackFactory();
    }

    @Before
    public void reset(){}

    @Test
    public void tidPattern(){
        JsonBuilder b = new JsonBuilder();
        Exp p = f.newTidPattern();
        Json o = null;

        p.apply(new CheatChars("\"Name with \" and '\" prio=10 tid=0x00007fe444377000 nid=0x1036 in Object.wait() [0x00007fe42eef5000]"),b,null);

        o = b.getRoot();

        assertEquals("name","Name with \" and '",o.getString("name"));
        assertEquals("prio","10",o.getString("prio"));
        assertEquals("tid","0x00007fe444377000",o.getString("tid"));
        assertEquals("nid", "0x1036", o.getString("nid"));
        assertEquals("status","in Object.wait()",o.getString("status"));
        assertEquals("hex","0x00007fe42eef5000",o.getString("hex"));

        b.close();b.takeClosedRoot();

        p.apply(new CheatChars("\"GC task thread#0 (ParallelGC)\" os_prio=0 tid=0x00007f9b4c023000 nid=0x4d0c runnable "),b,null);

        o = b.getRoot();
        assertEquals("name","GC task thread#0 (ParallelGC)",o.getString("name"));
        assertEquals("os_prio","0",o.getString("osprio"));
        assertEquals("tid","0x00007f9b4c023000",o.getString("tid"));
        assertEquals("nid","0x4d0c",o.getString("nid"));
        assertEquals("status","runnable",o.getString("status"));

        b.close();b.takeClosedRoot();

        p.apply(new CheatChars("\"C1 CompilerThread2\" #7 daemon prio=9 os_prio=0 tid=0x00007f9b4c0b4800 nid=0x4d16 waiting on condition [0x0000000000000000]\n"),b,null);

        o = b.getRoot();
        assertEquals("name","C1 CompilerThread2",o.getString("name"));
        assertTrue("daemon", o.getBoolean("daemon"));
        assertEquals("prio","9",o.getString("prio"));
        assertEquals("os_prio","0",o.getString("osprio"));
        assertEquals("tid","0x00007f9b4c0b4800",o.getString("tid"));
        assertEquals("nid","0x4d16",o.getString("nid"));
        assertEquals("status","waiting on condition",o.getString("status"));
        assertEquals("hex","0x0000000000000000",o.getString("hex"));
    }

    @Test
    public void stack(){
        JsonBuilder b = new JsonBuilder();
        Exp frame = f.newStackFramePattern();
        Exp lock = f.newLockPattern();

        frame.apply(new CheatChars("\tat sun.nio.ch.EPollArrayWrapper.epollWait(Native Method)\n"),b,null);
        frame.apply(new CheatChars("\tat sun.nio.ch.SelectorImpl.lockAndDoSelect(SelectorImpl.java:86)\n"),b,null);
        lock.apply(new CheatChars("\t- locked <0x00000000d440b740> (a io.netty.channel.nio.SelectedSelectionKeySet)\n"),b,null);
        lock.apply(new CheatChars("\t- locked <0x00000000d440b760> (a java.util.Collections$UnmodifiableSet)\n"),b,null);

        Json o = b.getRoot();
        assertEquals("stack should have 2 entries",2,o.getJson("stack").size());
        Json f = o.getJson("stack").getJson(0);
        Json s = o.getJson("stack").getJson(1);
        assertEquals("first stack frame should be native",true,f.getBoolean("nativeMethod"));
        assertEquals("second stack frame should have 2 locks",2,s.getJson("lock").size());
    }
}
