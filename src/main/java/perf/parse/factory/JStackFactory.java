package perf.parse.factory;

import perf.parse.Eat;
import perf.parse.Exp;
import perf.parse.Merge;
import perf.parse.Parser;
import perf.parse.Rule;
import perf.parse.Value;


/**
 * Created by wreicher
 */
public class JStackFactory {
    public Exp newThreadDump(){
        return new Exp("start", "Full thread dump (?<vm>[^\\(]+)\\((?<version>[^\\(]+)\\)").set(Merge.NewStart);
    }
    public Exp newTidPattern(){
        return new Exp("tid", " tid=(?<tid>0x[0-9a-f]+) nid=(?<nid>0x[0-9a-f]+)")
                .set(Merge.NewStart)
                .add(new Exp("os_prio", " os_prio=(?<osprio>\\d+)")
                    .set(Rule.LineStart))
                .add(new Exp("prio", " prio=(?<prio>\\d+)")
                    .set(Rule.LineStart))
                .add(new Exp("daemon", " (?<daemon>daemon)")
                    .set("daemon", Value.BooleanKey)
                    .set(Rule.LineStart))
                .add(new Exp("name", "\\\"(?<name>.+)\\\"(?: #\\d+)?")
                    .set(Rule.LineStart)
//                    .add(new Exp("#num"," #\\d+").debug()
//                        .set(Rule.LineStart)
//                    )
                )
                .add(new Exp("hex", "\\[(?<hex>0x[0-9a-f]+)\\]")
                    .eat(Eat.Match))
                .add(new Exp("status", " (?<status>[^\\[\n]+) ")
                );
    }
    public Exp newThreadStatePattern(){
        return new Exp("ThreadState","\\s+java\\.lang\\.Thread\\.State: (?<state>.*)");
    }
    public Exp newStackFramePattern(){
        return new Exp("stack", "\\s+at (?<frame>[^\\(]+)").group("stack").set(Merge.Entry)
            .add(new Exp("nativeMethod", "\\((?<nativeMethod>Native Method)\\)").set("nativeMethod", Value.BooleanKey))
            .add(new Exp("lineNumber", "\\((?<file>[^:]+):(?<line>\\d+)\\)"));
    }
    public Exp newLockPattern(){
        return new Exp("stack","\\s+- locked <(?<id>0x[0-9a-f]+)> \\(a (?<class>[^\\)]+)\\)").extend("stack").group("lock").set(Merge.Entry);
    }
    public Exp newWaitPattern(){
        return new Exp("stack","\\s+- waiting on <(?<id>0x[0-9a-f]+)> \\(a (?<class>[^\\)]+)\\)").extend("stack").group("wait").set(Merge.Entry);
    }
    public Parser newFileStartParser(){
        Parser p = new Parser();
        p.add(newThreadDump());
        return p;
    }
    public Parser newThreadParser(){
        Parser p = new Parser();
        p.add(newTidPattern());
        p.add(newThreadStatePattern());
        p.add(newStackFramePattern());
        p.add(newLockPattern());
        p.add(newWaitPattern());
        return p;
    }
}
