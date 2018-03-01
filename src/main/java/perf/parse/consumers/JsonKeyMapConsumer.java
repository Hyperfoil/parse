package perf.parse.consumers;

import perf.parse.JsonConsumer;
import perf.yaup.json.Json;

/**
 *
 */
public class JsonKeyMapConsumer implements JsonConsumer {

    private Json keymap;

    public JsonKeyMapConsumer(){
        keymap = new Json();
    }

    @Override public void start() {  }

    @Override public void consume(Json object) {
        mergeKeys(keymap,object);
    }

    @Override public void close() {  }

    public Json getMap(){return keymap;}

    public void mergeKeys(Json target,Json source){
        if(source.isArray()){
            for(int i=0; i<source.keySet().size();i++){
                Object val = source.get(i);
                if(val instanceof Json){
                    Json valJson = (Json)val;
                    if(((Json) val).isArray()){
                        if(!target.has("#")){
                            target.set("#",new Json());
                        }
                        mergeKeys(target.getJson("#"),valJson);
                    }else{
                        mergeKeys(target,valJson);
                    }
                }
            }
        }else{
            for(Object key : source.keySet()){
                Object val = source.get(key);
                if(target.has(key)){
                    if(val instanceof Json){
                        Json valJson = (Json)val;
                        if(valJson.isArray()){
                            if(!target.getJson(key).has("#")){
                                target.getJson(key).set("#",new Json());
                            }
                            mergeKeys(target.getJson(key).getJson("#"),valJson);
                        }else{
                            mergeKeys(target.getJson(key),valJson);
                        }
                    }
                }else{
                    if(val instanceof Json){
                        Json valJson = (Json)val;
                        if(valJson.isArray()){
                            Json ary = new Json();
                            ary.set("#",new Json());
                            target.set(key,ary);
                            mergeKeys(ary.getJson("#"),valJson);
                        }else{
                            target.set(key,new Json());
                            mergeKeys(target.getJson(key),valJson);
                        }
                    }else{
                        target.set(key,val.getClass().getSimpleName()/*new Json(false)*/);
                    }
                }
            }
        }
    }
//    public void mergeKeys(Json target,JSONArray source){
//        for(int i=0; i<source.length(); i++){
//            Object val = source.get(i);
//            if(val instanceof JSONArray){
//                if(!target.has("#")){
//                    target.set("#",new JSONObject());
//                }
//                mergeKeys(target.getJSONObject("#"),(JSONArray)val);
//            }else if ( val instanceof JSONObject){
//                mergeKeys(target,Json.fromJSONObject((JSONObject)val));
//            }
//        }
//    }
//    public void mergeKeys(JSONObject target,Json source){
//        for(Object key : source.keySet()){
//            Object val = source.get(key);
//            String keyString = key.toString();
//            if(target.has(keyString)){
//
//                if(val instanceof JSONArray){
//                    if(!target.getJSONObject(keyString).has("#")){
//                        target.getJSONObject(keyString).set("#",new JSONObject());
//                    }
//                    mergeKeys(target.getJSONObject(keyString).getJSONObject("#"),(JSONArray)val);
//                }else if (val instanceof JSONObject){
//                    mergeKeys(target.getJSONObject(keyString), new Jsons((JSONObject) val));
//                }
//            }else{
//                if(val instanceof JSONArray){
//                    JSONObject ary = new JSONObject();
//                    ary.set("#",new JSONObject());
//                    target.set( keyString, ary );
//                    mergeKeys(ary.getJSONObject("#"), (JSONArray) val);
//                }else {
//                    target.set(key,new JSONObject());
//                    if (val instanceof JSONObject){
//                        mergeKeys(target.getJSONObject(key),new Jsons((JSONObject)val));
//                    }
//                }
//            }
//        }
//    }
}
