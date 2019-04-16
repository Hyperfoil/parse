package perf.parse.factory;

import perf.parse.*;
import perf.parse.Exp;


/**
 * Created by wreicher
 */
public class JStackFactory implements ParseFactory{
    public Exp threadDumpHeader(){
        return new Exp("start", "Full thread dump (?<vm>[^\\(]+)\\((?<version>[^\\(]+)\\)").setRule(MatchRule.PreClose);
    }
    public Exp threadInfo(){
        return new Exp("tid", " tid=(?<tid>0x[0-9a-f]+) nid=(?<nid>0x[0-9a-f]+)")
                .setRule(MatchRule.PreClose)
                .add(new Exp("os_prio", " os_prio=(?<osprio>\\d+)")
                    .setRange(MatchRange.EntireLine))
                .add(new Exp("prio", " prio=(?<prio>\\d+)")
                   .setRange(MatchRange.EntireLine))
                .add(new Exp("daemon", " (?<daemon>daemon)")
                    .setMerge("daemon", ValueMerge.BooleanKey)
                    .setRange(MatchRange.EntireLine))
                .add(new Exp("Name", "\\\"(?<Name>.+)\\\"(?: #\\d+)?")
                    .setRange(MatchRange.EntireLine)
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
        return new Exp("stack", "\\s+at (?<frame>[^\\(]+)").group("stack").setMerge(ExpMerge.AsEntry)
            .add(new Exp("nativeMethod", "\\((?<nativeMethod>Native Method)\\)").setMerge("nativeMethod", ValueMerge.BooleanKey))
            .add(new Exp("lineNumber", "\\((?<file>[^:]+):(?<line>\\d+)\\)"));
    }
    public Exp locked(){
        return new Exp("stack","\\s+- locked <(?<id>0x[0-9a-f]+)> \\(a (?<class>[^\\)]+)\\)").extend("stack").group("lock").setMerge(ExpMerge.AsEntry);
    }
    public Exp waiting(){
        return new Exp("stack","\\s+- waiting on <(?<id>0x[0-9a-f]+)> \\(a (?<class>[^\\)]+)\\)").extend("stack").group("wait").setMerge(ExpMerge.AsEntry);
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
