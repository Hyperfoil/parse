package perf.parse;

import perf.util.json.Json;

/**
 *
 */
public interface JsonConsumer {

    default public void start(){}
    default public void close(){}
    public void consume(Json object);

}
