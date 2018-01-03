package perf.parse;

import org.json.JSONObject;
import perf.parse.internal.CheatChars;
import perf.parse.internal.JsonBuilder;
import perf.util.json.Json;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

/**
 *
 */
public class Parser {


    private List<JsonConsumer> consumers;
    private LinkedList<Exp> patterns;
    private HashMap<String,Boolean> states;
    private JsonBuilder builder;

    public Parser(){
        consumers = new LinkedList<JsonConsumer>();
        patterns = new LinkedList<Exp>();
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

    public JSONObject getNames(){
        JSONObject rtrn = new JSONObject();
        for(Exp p : patterns){
            p.appendNames(rtrn);
        }
        return rtrn;
    }
    public JsonBuilder getBuilder(){return builder;}

    public void addAhead(Exp pattern){
        patterns.add(0,pattern);
    }
    public void add(Exp pattern){
        patterns.add(pattern);
    }
    public void add(JsonConsumer consumer){
        consumers.add(consumer);
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
    public JSONObject onLine(String str){
        return onLine(new CheatChars(str));
    }
    public JSONObject onLine(CheatChars line){

        boolean matched = false;

        for(Exp pattern : patterns){
            matched = pattern.apply(line,builder,this) && matched;
            if(line.isEmpty()){
                break;
            }
        }
        return emit();
    }

    public void setup(){
        for (JsonConsumer consumer : consumers) {
            consumer.start();
        }
    }

    public JSONObject close(){
        builder.close();
        JSONObject rtrn = emit();
        for (JsonConsumer consumer : consumers) {
            consumer.close();
        }
        return rtrn;
    }
    private JSONObject emit(){
        JSONObject toEmit = builder.takeClosedRoot();
        if(toEmit != null) {
            Json json = Json.fromJSONObject(toEmit);
            for (JsonConsumer consumer : consumers) {
                consumer.consume(json);
            }
        }
        return toEmit;
    }
}
