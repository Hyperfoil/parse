package perf.parse.consumers.gc;

import perf.parse.JsonConsumer;
import perf.yaup.json.Json;

/**
 * Created by wreicher
 */
public class GarbageRateConsumer implements JsonConsumer {


    private double previousYoungSize;
    private double previousYoungAfterGC;

    private double previousHeapSize;
    private double previousHeapAfterGC;

    @Override
    public void consume(Json object) {
        if(object.has("heap")){
            object.getJson("heap").getDouble("postgc");
            object.getJson("heap").getDouble("size");

        }
        if(object.has("region") && object.getJson("regoin").has("PSYoungGen")){

        }
    }
}
