package io.hyperfoil.tools.parse.reader;

import org.json.JSONObject;
import org.json.JSONTokener;
import io.hyperfoil.tools.parse.JsonConsumer;
import io.hyperfoil.tools.yaup.json.Json;

import java.io.InputStream;
import java.util.LinkedList;

/**
 *
 */
public class JsonArrayReader extends AReader {

    JSONTokener tokener;

    private LinkedList<JsonConsumer> consumers;

    public JsonArrayReader(){
        consumers = new LinkedList<>();
    }

    public void add(JsonConsumer consumer){
        consumers.add(consumer);
    }

    @Override protected void processInputStream(InputStream stream) {
        for(JsonConsumer c : consumers){
            c.start();
        }
        tokener = new JSONTokener(stream);
        char ch = tokener.next();//ignore the initial [ in the array, throw exception if not the expected char
        if(ch!='['){
            throw new IllegalArgumentException("Expect stream to start with [ but encountered ["+ch+"]");
        }
        while(tokener.more()){
            JSONObject obj = (JSONObject) tokener.nextValue();
            Json json = Json.fromJSONObject(obj);
            for(JsonConsumer jc : consumers){
                jc.consume(json);
            }
            ch = tokener.next(); // ignore the separating , or terminal ]
            if(ch!=',' && ch!=']'){
                throw new IllegalArgumentException("Expect trailing , or ] after each entry but encountered ["+ch+"]");
            }
        }
        for(JsonConsumer c : consumers){
            c.close();
        }
    }
}
