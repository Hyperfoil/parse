package perf.parse.internal;

import perf.yaup.json.Json;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.OptionalInt;
import java.util.Stack;
import java.util.stream.Stream;

/**
 * Created by wreicher
 */
public class JsonBuilder {

    private class Context {

        private Map<String,Object> data;
        private Context parent;

        public Context(){
            data = new LinkedHashMap<>();
            parent = null;
        }

        public Context popContext(){
            return parent==null ? this : parent;
        }
        public Context pushContext(){
            Context rtrn = new Context();
            rtrn.parent = this;
            return rtrn;
        }

        public void set(String key,Object value){
            data.put(key,value);
        }
        public boolean has(String key,boolean recursive){
            Context target = this.parent;
            boolean rtrn = data.containsKey(key);
            while(recursive && !rtrn && target!=null){
                rtrn = target.data.containsKey(key);
                target = target.parent;
            }
            return rtrn;
        }
        public Object get(String key,boolean recursive){
            Context target = this.parent;
            Object rtrn = data.get(key);
            while(recursive && rtrn==null && target!=null){
                rtrn = target.data.get(key);
                target = target.parent;
            }
            return rtrn;
        }
        public int getInteger(String key,boolean recursive,int defaultValue){
            Object val = get(key,recursive);
            int rtrn = defaultValue;
            if(val !=null && val instanceof Integer){
                rtrn = (Integer)val;
            }
            return rtrn;
        }
        public String getString(String key,boolean recursive,String defaultValue){
            Object val = get(key,recursive);
            String rtrn = defaultValue;
            if(val !=null && val instanceof String){
                rtrn = (String)val;
            }
            return rtrn;
        }

        public boolean hasParent() {
            return parent!=null;
        }
        public Context getParent(){return parent;}

        public boolean getBoolean(String key, boolean recursive, boolean defaultValue) {
            Object val = get(key,recursive);
            Boolean rtrn = defaultValue;
            if(val !=null && val instanceof Boolean){
                rtrn = (Boolean)val;
            }
            return rtrn;

        }
        @Override
        public String toString(){
            return toString(false);
        }

        public String toString(boolean recursive){
            StringBuilder sb = new StringBuilder();
            if(!recursive){
                sb.append(data.toString());
            }else{
                int index = 0;
                Context target = this;
                while(target!=null){
                    if(sb.length()>0){
                        sb.append(System.lineSeparator());
                    }
                    sb.append("["+index+"]=");
                    sb.append(target.data.toString());
                    index++;
                    target = target.parent;
                }
            }
            return sb.toString();
        }
    }

    private class Instance {
        private Json root;
        private Stack<Json> targetStack;
        private Context context;

        public Instance() {
            targetStack = new Stack<>();
            context = new Context();
            reset();
        }
        public void pushTarget(Json target) {
            synchronized (this) {
                targetStack.push(target);
                context = context.pushContext();
            }
        }
        public Json popTarget(){
            synchronized (this){
                if(targetStack.size()>1){
                    context = context.popContext();
                    return targetStack.pop();
                }
            }
            return null;
        }
        public boolean hasPrevious(){
            return targetStack.size()>2;
        }
        public Json peekTarget(int ahead){
            if(targetStack.size()>ahead+1){
                return targetStack.elementAt(targetStack.size()-(ahead+1));
            }
            return null;
        }
        public Json peekTarget(){
            return targetStack.peek();
        }
        public Json getRoot(){return root;}
        public Json getTarget(){return targetStack.peek();}

        public void setContext(String key,Object value){
            context.set(key,value);
        }
        public boolean hasContext(String key,boolean recursvive){
            return context.has(key,recursvive);
        }
        public boolean getContextBoolean(String key,boolean recursive) {
            return context.getBoolean(key,recursive,false);
        }
        public int getContextInteger(String key,boolean recursive){
            return context.getInteger(key,recursive,0);
        }
        public String getContextString(String key,boolean recursive){
            return context.getString(key,recursive,"");
        }

        public void reset(){
            root = new Json();
            targetStack.clear();
            targetStack.push(root);
            context = new Context();
        }
        public void clearTargets(){
            while(targetStack.size()>1){
                popTarget();
            }
            while(context.hasParent()){
                context = context.popContext();
            }
        }

        public String debugParallel(boolean recursive){
            if(!recursive){
                return context.toString()+" : "+getTarget().toString();
            }else{
                StringBuilder sb = new StringBuilder();
                String contextStr[] = debugContextString(true).split("\n");
                String targetStr[] = debugTargetString(true).split("\n");
                OptionalInt width = Stream.of(contextStr).mapToInt(String::length).max();
                width.ifPresent((w)->{
                    for(int i=0; i<contextStr.length; i++){
                        sb.append(String.format("%"+w+"s : %s%n",contextStr[i],targetStr[i]));
                    }
                });
                return sb.toString();
            }
        }
        public String debugContextString(boolean recursive){return context.toString(recursive);}
        public String debugTargetString(boolean recursive){
            if(!recursive){
                return current.getTarget().toString();
            }else{
                StringBuilder sb = new StringBuilder();

                for(int i=0; i<targetStack.size();i++){
                    if(sb.length()>0){
                        sb.append(System.lineSeparator());
                    }
                    sb.append("["+i+"]=");
                    sb.append(targetStack.elementAt(targetStack.size()-(1+i)));
                }
                return sb.toString();
            }
        }
        public boolean isEmpty(){
            return targetStack.size()==1 && root.size()==0;
        }
        public int depth(){
            return targetStack.size()-1;
        }

    }

    private Instance current;
    private Instance previous;

    public JsonBuilder(){
        this(new Json());
    }
    public JsonBuilder(Json json){

        current = new Instance();
        previous = new Instance();
    }

    public boolean close(){
        if(previous.isEmpty()){
            Instance tmp = previous;
            previous = current;
            current = tmp;
            return true;
        }
        return false;
    }

    public boolean wasClosed(){
        return !previous.isEmpty();
    }
    public Json takeClosedRoot(){
        if(!wasClosed())
            return null;

        Json rtrn = previous.getRoot();
        if(rtrn.size()==0){
            rtrn = null;
        }
        previous.reset();
        return rtrn;
    }
    public int depth(){return current.depth();}
    public boolean hasTargets(){return !current.isEmpty();}
    public Json getRoot(){return current.getRoot();}

    public Json peekTarget(int ahead){return current.peekTarget(ahead);}

    public Json getTarget(){
        return current.getTarget();
    }
    public void pushTarget(Json json){
        current.pushTarget(json);
    }
    public void popTarget(){
        popTarget(1);
    }

    public String debugParallel(boolean recursive){
        return current.debugParallel(recursive);
    }
    public String debugContextString(boolean recursive){
        return current.debugContextString(recursive);
    }
    public String debugTargetString(boolean recursive){
        return current.debugTargetString(recursive);
    }
    public void popTarget(int count){
        for(int i=0; i<count; i++){
            current.popTarget();
        }
    }
    public void clearTargets(){
        current.clearTargets();
    }
    public void reset(){
        current.reset();
        previous.reset();
    }

    public boolean hasContext(String key,boolean recursive){
        return current.hasContext(key,recursive);
    }
    public String getContextString(String key,boolean recursive){
        return current.getContextString(key,recursive);
    }
    public void setContext(String key,Object value){
        current.setContext(key,value);
    }
    public int getContextInteger(String key,boolean recursive){
        return current.getContextInteger(key,recursive);
    }

    public boolean getContextBoolean(String key,boolean recursive){
        return current.getContextBoolean(key,recursive);
    }
}
