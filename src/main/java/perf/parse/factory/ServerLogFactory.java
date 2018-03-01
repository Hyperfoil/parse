package perf.parse.factory;

import perf.parse.Eat;
import perf.parse.Exp;
import perf.parse.Merge;
import perf.parse.Parser;
import perf.parse.Rule;
import perf.parse.Value;
import perf.parse.reader.TextLineReader;

/**
 * Created by wreicher
 */
public class ServerLogFactory {

    private Parser parser;

    public Exp newStartEntryExp(){
        return new Exp("timestamp","(?<timestamp>\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2},\\d{3})")
            .eat(Eat.Match)
            .set(Merge.NewStart)
            .add(new Exp("level", " (?<level>[A-Z]+)\\s+")
                    .eat(Eat.Match))
            .add(new Exp("component","\\[(?<component>[^\\]]+)\\]\\s+")
                    .eat(Eat.Match))
            .add(new Exp("threadName","\\((?<threadName>.+?)\\) ")
                    .eat(Eat.Match))
            .add(new Exp("message","(?<message>.+\n?)")
                    .eat(Eat.Match));
    }
    public Exp newFrameExp(){
        return new Exp("frame","\\s+at (?<frame>[^\\(]+)")
            .eat(Eat.Match)
            .group("stack").set(Merge.Entry)
            //.debug()
            .add(new Exp("nativeMethod", "\\((?<nativeMethod>Native Method)\\)")
                    .eat(Eat.Line)
                    .set("nativeMethod", Value.BooleanKey))
            .add(new Exp("lineNumber","\\((?<file>[^:]+):(?<line>[^\\)]+)\\)")
                    .eat(Eat.Line)
                    .set("line",Value.Number))
            .add(new Exp("unknownSource","\\((?<unknownSource>Unknown Source)\\)")
                    .eat(Eat.Line)
                    .set("unknownSource", Value.BooleanKey));
    }
    public Exp newCausedByExp(){
        return new Exp("causedBy","Caused by: (?<exception>[^:]+): (?<message>.+\n?)")
            //.group("stack")
            .group("causedBy")
            .set(Rule.PushTarget).eat(Eat.Line);
    }
    public Exp newStackRemainderExp(){
        return new Exp("more","\\s+\\.\\.\\. (?<stackRemainder>\\d+) more")
                .set("stackRemainder",Value.Number)
                .eat(Eat.Line);
    }
    public Exp newMessageExp(){
        return new Exp("message","(?<message>.+\n?)").set("message", Value.String);
    }
    public Parser newLogEntryParser(){
        Parser p = new Parser();
            p.add(newStartEntryExp());
            p.add(newFrameExp());
            p.add(newCausedByExp());
            p.add(newStackRemainderExp());
            p.add(newMessageExp());
        return p;
    }

    public static void main(String[] args) {

        String filePath = null;
        filePath = "/home/wreicher/specWork/reentrant/reentrant-aio-196/log/server.log";
        filePath = "/home/wreicher/specWork/server.246Y.log";
        filePath = "/home/wreicher/runtime/wildfly-10.0.0.Final-invm/standalone/log/server.log";
        ServerLogFactory f = new ServerLogFactory();
        Parser p = f.newLogEntryParser();
        TextLineReader r = new TextLineReader();
        r.addParser(p);

        System.out.println("☐\n☑\n☒\n✓\n✔\n✕\n✖\n✗\n✘\n⒛\n");
        System.out.println(p.getNames().toString(2));

        p.add(json->{
            if(json.toString().contains("LEAK")){
                String message[] = json.getString("message").split("\n");

                for(int i=message.length-1; i>=0; i--){
                    System.out.println(i+":"+message[i]);
                    if(message[i].startsWith("\t")){
                        System.out.println("tabbbbb");
                    }
                }
            }
        });

        System.out.println(System.currentTimeMillis());
        r.read(filePath);
        System.out.println(System.currentTimeMillis());

    }

}
