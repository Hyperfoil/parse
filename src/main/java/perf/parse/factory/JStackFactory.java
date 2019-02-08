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
    public Exp threadDumpHeader(){
        return new Exp("start", "Full thread dump (?<vm>[^\\(]+)\\((?<version>[^\\(]+)\\)").set(Merge.PreClose);
    }
    public Exp threadInfo(){
        return new Exp("tid", " tid=(?<tid>0x[0-9a-f]+) nid=(?<nid>0x[0-9a-f]+)")
                .set(Merge.PreClose)
                .add(new Exp("os_prio", " os_prio=(?<osprio>\\d+)")
                    .set(Rule.LineStart))
                .add(new Exp("prio", " prio=(?<prio>\\d+)")
                    .set(Rule.LineStart))
                .add(new Exp("daemon", " (?<daemon>daemon)")
                    .set("daemon", Value.BooleanKey)
                    .set(Rule.LineStart))
                .add(new Exp("Name", "\\\"(?<Name>.+)\\\"(?: #\\d+)?")
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
    public Exp threadState(){
        return new Exp("ThreadState","\\s+java\\.lang\\.Thread\\.State: (?<state>.*)");
    }
    public Exp stackFrame(){
        return new Exp("stack", "\\s+at (?<frame>[^\\(]+)").group("stack").set(Merge.Entry)
            .add(new Exp("nativeMethod", "\\((?<nativeMethod>Native Method)\\)").set("nativeMethod", Value.BooleanKey))
            .add(new Exp("lineNumber", "\\((?<file>[^:]+):(?<line>\\d+)\\)"));
    }
    public Exp locked(){
        return new Exp("stack","\\s+- locked <(?<id>0x[0-9a-f]+)> \\(a (?<class>[^\\)]+)\\)").extend("stack").group("lock").set(Merge.Entry);
    }
    public Exp waiting(){
        return new Exp("stack","\\s+- waiting on <(?<id>0x[0-9a-f]+)> \\(a (?<class>[^\\)]+)\\)").extend("stack").group("wait").set(Merge.Entry);
    }
    public Parser newParser(){
        Parser p = new Parser();
        addToParser(p);
        return p;
    }
    public void addToParser(Parser p){
        p.add(threadDumpHeader());
        p.add(threadInfo());
        p.add(threadState());
        p.add(stackFrame());
        p.add(locked());
        p.add(waiting());
    }
}
