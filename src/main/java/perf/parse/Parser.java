package perf.parse;

import perf.parse.internal.CheatChars;
import perf.parse.internal.JsonBuilder;
import perf.yaup.json.Json;

import java.util.*;

/**
 *
 */
public class Parser {


    private List<JsonConsumer> consumers;
    private ArrayList<Exp> patterns;
    private HashMap<String,Boolean> states;
    private JsonBuilder builder;

    public Parser(){
        consumers = new LinkedList<JsonConsumer>();
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
        consumers.add(consumer);
    }
    public List<Exp> exps(){return Collections.unmodifiableList(patterns);}
    public int remove(String patternName) {
        int index = -1;
        for(int i=0; i<patterns.size(); i++){
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
            if(pattern.is(Merge.NewStart)){
                if(pattern.test(line)){
                    return true;
                }
            }
        }
        return false;
    }
    public Json onLine(String str){
        return onLine(new CheatChars(str));
    }
    public Json onLine(CheatChars line){
        boolean matched = false;

        int size = patterns.size();
        for(int i=0; i<size; i++){
            Exp exp = patterns.get(i);
            matched = exp.apply(line,builder,this) && matched;
            int newSize = patterns.size();
            if(newSize!=size){
                int diff = newSize-size;
                i+=diff;
            }
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
        Json toEmit = builder.takeClosedRoot();
        if(toEmit != null) {
            for (JsonConsumer consumer : consumers) {
                consumer.consume(toEmit);
            }
        }
        return toEmit;
    }
}
