package perf.parse;

import perf.parse.internal.CheatChars;
import perf.parse.internal.JsonBuilder;
import perf.yaup.json.Json;

import java.util.*;
import java.util.stream.Collectors;

/**
 *
 */
public class ParserOld {

    public static interface UnparsedConsumer {
        void accept(String remainder, String original, int lineNumber);
    }

    private List<JsonConsumer> consumers;
    private ArrayList<ExpOld> patterns;
    private HashMap<String,Boolean> states;
    private JsonBuilder builder;
    private List<UnparsedConsumer> unparsedConsumers;


    public List<String> patternNames(){return patterns.stream().map(p->p.getName()).collect(Collectors.toList());}

    public ParserOld(){
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
        for(ExpOld p : patterns){
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

    public void addAhead(ExpOld pattern){
        patterns.add(0,pattern);
    }
    public void addAt(ExpOld pattern, int order){
        patterns.add(order,pattern);
    }
    public void add(ExpOld pattern){
        patterns.add(pattern);
    }
    public void add(JsonConsumer consumer){
        if(consumer!=null) {
            consumers.add(consumer);
        }
    }
    public List<ExpOld> exps(){return Collections.unmodifiableList(patterns);}
    public ExpOld get(String patternName){
        ExpOld rtrn = null;
        for(int i=0; i<patterns.size() && rtrn==null; i++){
            ExpOld exp = patterns.get(i);
            if(exp.getName().equals(patternName)){
                rtrn = exp;
            }
        }
        return rtrn;
    }
    public int remove(String patternName) {
        int index = -1;
        for(int i=0; i<patterns.size() && index==-1; i++){
            ExpOld exp = patterns.get(i);
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
        for(ExpOld pattern : patterns){
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
            ExpOld exp = patterns.get(i);
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
            unparsedConsumers.forEach(consumer -> consumer.accept(line.toString(),line.getLine(),lineNumber));
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