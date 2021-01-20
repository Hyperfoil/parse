package io.hyperfoil.tools.parse;

import io.hyperfoil.tools.parse.factory.CsvFactory;
import io.hyperfoil.tools.parse.factory.DstatFactory;
import io.hyperfoil.tools.parse.factory.JStackFactory;
import io.hyperfoil.tools.parse.factory.Jep271Factory;
import io.hyperfoil.tools.parse.factory.JmapHistoFactory;
import io.hyperfoil.tools.parse.factory.PrintGcFactory;
import io.hyperfoil.tools.parse.factory.ServerLogFactory;
import io.hyperfoil.tools.parse.factory.SubstrateGcFactory;
import io.hyperfoil.tools.parse.factory.WrkFactory;
import io.hyperfoil.tools.parse.factory.XanFactory;
import io.hyperfoil.tools.parse.internal.CheatChars;
import io.hyperfoil.tools.parse.internal.JsonBuilder;
import io.hyperfoil.tools.yaup.json.Json;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 *
 */
public class Parser {

    public static interface UnparsedConsumer {
        void accept(String remainder,String original,int lineNumber);
    }

    public static Parser fromJson(Object obj){
            if(obj instanceof String){
                switch (obj.toString().toLowerCase()){
                    case "csvfactory": return new CsvFactory().newParser();
                    case "dstatfactory": return new DstatFactory().newParser();
                    case "jep271factory": return new Jep271Factory().newParser();
                    case "jmaphistofactory": return new JmapHistoFactory().newParser();
                    case "jstackfactory": return new JStackFactory().newParser();
                    case "printgcfactory": return new PrintGcFactory().newParser();
                    case "serverlogfactory": return new ServerLogFactory().newParser();
                    case "substrategcfactory": return new SubstrateGcFactory().newParser();
                    case "xanfactory": return new XanFactory().newParser();
                    case "wrkfactory": return new WrkFactory().newParser();
                    default:
                        throw new IllegalArgumentException("unknown parser "+obj.toString());
                }
            }else if (obj instanceof Json){
                Json json = (Json)obj;
                //same for array or map
                Parser p = new Parser();
                json.values().forEach(entry->{
                    if(entry instanceof String){
                        Exp exp = new Exp(entry.toString());
                        p.add(exp);
                    }else if (entry instanceof Json){
                        Exp exp = Exp.fromJson((Json)entry);
                        p.add(exp);
                    }else{
                        throw new IllegalArgumentException("cannot create expression from "+entry);
                    }
                });
                return p;
            }else{
                throw new IllegalArgumentException("unknown parser "+obj);
            }
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
    public void addAt(Exp pattern, int order){
        if(patterns.size()<=order){
            patterns.add(pattern);
        }else {
            patterns.add(order, pattern);
        }
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
    public boolean remove(Exp exp){
        return patterns.remove(exp);
    }
    public void clearConsumers(){consumers.clear();}

    public boolean test(CharSequence line){
        for(Exp pattern : patterns){
            if(pattern.hasRule(ExpRule.PreClose)){
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
            unparsedConsumers.forEach(consumer -> consumer.accept(line.toString(),line.getLine(),lineNumber));
        }

        return emit();
    }

    public void setup(){
        for (JsonConsumer consumer : consumers) {
            consumer.start();
        }
        for(Exp exp : patterns){
            exp.onSetup(this);
        }
    }

    public Json close(){
        builder.close();
        Json rtrn = emit();
        for (JsonConsumer consumer : consumers) {
            consumer.close();
        }
        for(Exp exp : patterns){
            exp.onClose(this);
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
