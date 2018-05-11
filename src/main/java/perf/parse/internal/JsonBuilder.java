package perf.parse.internal;

import perf.yaup.json.Json;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

/**
 * Created by wreicher
 *
 */

/**
 * Needs a re-design
 * The idea is to track target json and info about that target
 * right now we use a stack for targets, should we be able to close entries in the middle?
 * maybe add exp name to the info and be able to close by name?
 */
public class JsonBuilder {

    public static final String NAME_KEY = "_TARGET_NAME_"+System.currentTimeMillis();



    private Stack<Json> targets;
    private Stack<Map<String,Object>> targetInfo;
    private Json closedJson;

    public JsonBuilder(){
        targets = new Stack<>();
        targetInfo = new Stack<>();
        closedJson = null;
        pushTarget(new Json());
    }

    public boolean close(){
        if(!wasClosed() && !getRoot().isEmpty()){
            closedJson = getRoot();
            targets.clear();
            pushTarget(new Json());
            return true;
        }
        return false;
    }

    public boolean wasClosed(){
        return closedJson!=null;
    }
    public Json takeClosed(){
        if(!wasClosed()) {
            return null;
        }
        Json rtrn = closedJson;
        closedJson = null;
        return rtrn;
    }
    public Json getRoot(){return targets.get(0);}

    public Json peekTarget(int ahead){
        if(ahead > targets.size()-2){
            return null;
        }
        return targets.get(targets.size()-1-ahead);
    }

    public Json getTarget(){
        return targets.peek();
    }
    public Json getTarget(String name){
        Json rtrn = null;
        int index = namedTargetIndex(name);
        if(index>=0){
            rtrn = targets.get(index);
        }
        return rtrn;
    }
    public int pushTarget(Json json){
        return pushTarget(json,null);
    }
    public int pushTarget(Json json,String name){
        int rtrn = -1;
        synchronized (this) {
            Map<String,Object> infoMap = new ConcurrentHashMap<>();
            if(name!=null && !name.isEmpty()) {
                infoMap.put(NAME_KEY, name);
            }
            rtrn = targets.size();
            targets.push(json);
            targetInfo.push(infoMap);
        }
        return rtrn;

    }
    public Json popTargetIndex(int index){
        synchronized (this){
            if(targets.size()>index && index > 0){
                Json rtrn = targets.remove(index);
                targetInfo.remove(index);
                return rtrn;
            }
        }
        return null;
    }
    public Json popTarget(){
        return popTarget(1);
    }
    public Json popTarget(String name){
        int index = namedTargetIndex(name);
        Json rtrn = null;
        if(index >= 0){
            rtrn = popTargetIndex(index);
        }
        return rtrn;
    }
    public int namedTargetIndex(String name){
        int index = targetInfo.size()-1;
        boolean found = false;
        do {
            String indexName = (String)targetInfo.get(index).get(NAME_KEY);
            found = name.equals(indexName);
        }while(!found && index-- >= 0);

        return index;
    }
    public Json popTarget(int count){
        if(count >= targets.size()-1){//-1

        }
        Json rtrn = null;
        synchronized (this){
            for(int i=0; i<count; i++){
                if(targets.size()>1) {//TODO remove size check and see why Exp is trying to pop root
                    rtrn = targets.pop();
                    targetInfo.pop();
                }
            }
        }
        return rtrn;
    }

    public int size(){return targets.size();}

    public String debug(boolean recursive){
        StringBuilder sb = new StringBuilder();
        int infoWidth = targetInfo.stream().mapToInt((s)->s.toString().length()).max().orElse(2);
        int idxWidth = Math.max((int)Math.round(Math.ceil(Math.log10(targets.size()))),1);
        int limit = recursive?0:targets.size()-1;
        for(int i=targets.size()-1;i>=limit; i--){
            if(i<targets.size()-1){
                sb.append("\n");
            }
            sb.append(String.format("%-"+infoWidth+"s %"+idxWidth+"d %s",targetInfo.get(i),i,targets.get(i)));
        }
        return sb.toString();
    }
    public void clearTargets(){
        synchronized (this){
            while(targets.size()>1){
                targets.pop();
                targetInfo.pop();
            }
        }
    }
    public void reset(){
        synchronized (this){
            targets.clear();
            targetInfo.clear();
            Json root = new Json();
            targets.push(root);
            targetInfo.push(new ConcurrentHashMap<>());
        }
    }

    public boolean hasContext(String key,boolean recursive){
        int index = targetInfo.size()-1;
        boolean rtrn;
        do {
            rtrn = targetInfo.get(index).containsKey(key);
            index--;
        }while(!rtrn && recursive && index >= 0);
        return rtrn;
    }
    public void setContext(String key,Object value){
        targetInfo.peek().put(key,value);
    }
    private Object getContext(String key,boolean recursive,Object defaultValue){
        int index = targets.size()-1;
        Object rtrn = defaultValue;
        boolean found = false;
        do {
            if(targetInfo.get(index).containsKey(key)){
                found = true;
                rtrn = targetInfo.get(index).get(key);
            }
            index--;
        }while(!found && recursive && index>=0);
        return rtrn;
    }
    public String getContextString(String key,boolean recursive){
        Object rtrn = getContext(key,recursive,"");
        return (String)rtrn;
    }
    public int getContextInteger(String key,boolean recursive){
        Object rtrn = getContext(key,recursive,0);
        return (Integer)rtrn;
    }
    public boolean getContextBoolean(String key,boolean recursive){
        Object rtrn = getContext(key,recursive,false);
        return (Boolean)rtrn;
    }
}
