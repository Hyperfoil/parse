package io.hyperfoil.tools.parse.consumers;

import io.hyperfoil.tools.parse.JsonConsumer;
import io.hyperfoil.tools.yaup.json.Json;

import java.util.HashSet;
import java.util.function.Function;

/**
 * Created by wreicher
 */
public class FilterConsumer implements JsonConsumer {


    private HashSet<JsonConsumer> consumers;
    private Function<Json,Boolean> filter;

    public FilterConsumer(Function<Json,Boolean> filter){
        this.filter = filter;
        this.consumers = new HashSet<>();
    }

    public void addConsumer(JsonConsumer consumer){
        this.consumers.add(consumer);
    }

    @Override
    public void consume(Json object) {
        if( filter.apply(object) ){
            for(JsonConsumer consumer : consumers){
                consumer.consume(object);
            }
        }
    }
}
