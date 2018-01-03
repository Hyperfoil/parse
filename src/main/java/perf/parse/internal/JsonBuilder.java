package perf.parse.internal;

import org.json.JSONObject;

import java.util.Stack;
import java.util.function.Consumer;

/**
 * Created by wreicher
 */
public class JsonBuilder {
    private JSONObject root;
    private Stack<JSONObject> currentContext;

    private JSONObject closedRoot;
    private Stack<JSONObject> closedContext;

    public JsonBuilder(){
        this(new JSONObject());
    }
    public JsonBuilder(JSONObject json){
        root = json;
        currentContext = new Stack<JSONObject>();

        closedRoot = null;
        closedContext = new Stack<JSONObject>();;
    }


    public boolean close(){
        if(closedRoot == null ){
            closedRoot = root;
            Stack<JSONObject> tmp = closedContext;
            closedContext = currentContext;
            currentContext = tmp;
            root = new JSONObject();
            return true;
        }
        return false;
    }

    public boolean wasClosed(){
        return closedRoot != null;
    }
    public JSONObject takeClosedRoot(){
        if(!wasClosed())
            return null;

        JSONObject rtrn = closedRoot;
        if(rtrn.length()==0){
            rtrn = null;
        }
        closedRoot = null;
        closedContext.clear();
        return rtrn;
    }
    public int depth(){return currentContext.size();}
    public boolean hasContext(){return !currentContext.isEmpty();}
    public JSONObject getRoot(){return root;}

    public JSONObject getCurrentContext(){
        if(currentContext.isEmpty())
            return root;
        else
            return currentContext.peek();
    }
    public void setCurrentContext(JSONObject json){
        if(currentContext.isEmpty() || currentContext.peek()!=json) {

            currentContext.push(json);
        } else {
        }
    }
    public void popContext(){
        if(!currentContext.isEmpty())
            currentContext.pop();
    }
    public void clearContext(){
        currentContext.clear();
    }
    public void reset(){
        reset(new JSONObject());
    }
    public void reset(JSONObject json){
        this.root = json;
        clearContext();
    }
    public void printContext(){
        currentContext.forEach(System.out::println);

    }
}
