package io.hyperfoil.tools.parse;

import io.hyperfoil.tools.yaup.json.Json;

import java.util.ArrayList;
import java.util.Collections;

/**
 *
 */
public interface JsonConsumer {

    default public void start(){}
    default public void close(){}
    public void consume(Json object);


    public static class List implements JsonConsumer{
        private final ArrayList<Json> values = new ArrayList<>();

        @Override
        public void consume(Json object){
            values.add(object);
        }
        public void clear(){
            values.clear();
        }
        public java.util.List<Json> getJson(){return Collections.unmodifiableList(values);}
    }
}
