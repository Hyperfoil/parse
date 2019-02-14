package perf.parse;

import perf.parse.internal.CheatChars;
import perf.parse.internal.JsonBuilder;
import perf.yaup.file.FileUtility;
import perf.yaup.json.Json;

import java.util.*;
import java.util.stream.Collectors;

/**
 *
 */
public class Parser {

    public static void main(String[] args) {
        Parser parser = new Parser();
        parser.add(new Exp("timestamp","^(?<timestamp>\\d+)$").set(Merge.PreClose).eat(Eat.Line).debug());
        parser.add(new Exp("cpu",
                "^" +
                        "(?<cpu>cpu\\d*) " +
                        "(?<user>\\d+) " +
                        "(?<nice>\\d+) " +
                        "(?<system>\\d+) " +
                        "(?<idle>\\d+) " +
                        "(?<iowait>\\d+) " +
                        "(?<irq>\\d+) " +
                        "(?<softirq>\\d+) " +
                        "(?<steal>\\d+) " +
                        "(?<guest>\\d+) " +
                        "(?<guest_nice>\\d+) " +
                        "$"
        ).eat(Eat.Line).nest("cpu").set(Merge.Entry).debug());

        parser.add(new Exp("intr","^intr (?<intrTotal>\\d+)[\\s\\d]+$").eat(Eat.Line));
        parser.add(new Exp("ctxt","^ctxt (?<ctxt>\\d+)$").eat(Eat.Line));
        parser.add(new Exp("btime","^btime (?<btime>\\d+)$").eat(Eat.Line));
        parser.add(new Exp("processes","^processes (?<processes>\\d+)$").eat(Eat.Line));
        parser.add(new Exp("procs_running","^procs_running (?<procs_running>\\d+)$").eat(Eat.Line));
        parser.add(new Exp("procs_blocked","^procs_blocked (?<procs_blocked>\\d+)$").eat(Eat.Line));

        parser.add(new Exp("softirq","^softirq (?<softirq>\\d+)[\\s\\d+]$").eat(Eat.Line));

        List<String> lines = FileUtility.lines("/home/wreicher/perfWork/jEnterprise/fullProfile/archive/run/benchserver2.perf.lab.eng.rdu.redhat.com/proc-stats.log");
        for(int i=0; i<10;i++){
            System.out.println("["+i+"/"+(lines.get(i).split("\\s+").length)+"]"+lines.get(i));
            parser.onLine(lines.get(i));
        }
    }

    public static interface UnparsedConsumer {
        void accept(String remainder,String original,int lineNumber);
    }

    private List<JsonConsumer> consumers;
    private ArrayList<Exp> patterns;
    private HashMap<String,Boolean> states;
    private JsonBuilder builder;
    private List<UnparsedConsumer> unparsedConsumers;


    public List<String> patternNames(){return patterns.stream().map(p->p.getName()).collect(Collectors.toList());}

    public Parser(){
        consumers = new LinkedList<>();
        unparsedConsumers = new LinkedList<>();
        patterns = new ArrayList<>();
        builder = new JsonBuilder();
        states = new HashMap<>();
    }

    public void setState(String state,boolean value){
        states.put(state,value);
    }
    public boolean getState(String state){
        if(!states.containsKey(state)){
            states.put(state,false);
        }
        return states.get(state);
    }

    public Json getNames(){
        Json rtrn = new Json();
        for(Exp p : patterns){
            p.appendNames(rtrn);
        }
        return rtrn;
    }
    public JsonBuilder getBuilder(){return builder;}

    public void addUnparsedConsumer(UnparsedConsumer consumer){
        unparsedConsumers.add(consumer);
    }
    public void removeUnparsedConsumer(UnparsedConsumer consumer){
        unparsedConsumers.remove(consumer);
    }
    public void clearUnparsedConsumers(){
        unparsedConsumers.clear();
    }

    public void addAhead(Exp pattern){
        patterns.add(0,pattern);
    }
    public void addAt(Exp pattern,int order){
        patterns.add(order,pattern);
    }
    public void add(Exp pattern){
        patterns.add(pattern);
    }
    public void add(JsonConsumer consumer){
        if(consumer!=null) {
            consumers.add(consumer);
        }
    }
    public List<Exp> exps(){return Collections.unmodifiableList(patterns);}
    public Exp get(String patternName){
        Exp rtrn = null;
        for(int i=0; i<patterns.size() && rtrn==null; i++){
            Exp exp = patterns.get(i);
            if(exp.getName().equals(patternName)){
                rtrn = exp;
            }
        }
        return rtrn;
    }
    public int remove(String patternName) {
        int index = -1;
        for(int i=0; i<patterns.size() && index==-1; i++){
            Exp exp = patterns.get(i);
            if(exp.getName().equals(patternName)){
                index = i;
            }
        }
        if(index>-1) {

            patterns.remove(index);
        }
        return index;
    }
    public void clearConsumers(){consumers.clear();}

    public boolean test(CharSequence line){
        for(Exp pattern : patterns){
            if(pattern.is(Merge.PreClose)){
                if(pattern.test(line)){
                    return true;
                }
            }
        }
        return false;
    }
    public Json onLine(String str){
        return onLine(new CheatChars(str),0);
    }
    public Json onLine(String str,int lineNumber){
        return onLine(new CheatChars(str),lineNumber);
    }
    public Json onLine(CheatChars line,int lineNumber){
        boolean matched = false;

        int size = patterns.size();
        for(int i=0; i<size; i++){ //to get around concurrent mod from exp matching
            Exp exp = patterns.get(i);
            matched = exp.apply(line,builder,this) && matched;
            int newSize = patterns.size();
            if(newSize!=size){ // deal with mod after executing exp
                int newIndex = patterns.indexOf(exp);
                //int diff = newSize-size;
                //i+=diff;
                //size+=diff;
                i=newIndex;
                size=newSize;
            }
        }

        if(!line.isEmpty() && !line.toString().trim().isEmpty() && !unparsedConsumers.isEmpty()){
            unparsedConsumers.forEach(consumer -> consumer.accept(line.toString(),line.getOriginalLine(),lineNumber));
        }

        return emit();
    }

    public void setup(){
        for (JsonConsumer consumer : consumers) {
            consumer.start();
        }
    }

    public Json close(){
        builder.close();
        Json rtrn = emit();
        for (JsonConsumer consumer : consumers) {
            consumer.close();
        }
        return rtrn;
    }
    private Json emit(){
        Json toEmit = builder.takeClosed();
        if(toEmit != null) {
            for (JsonConsumer consumer : consumers) {
                consumer.consume(toEmit);
            }
        }
        return toEmit;
    }
}
