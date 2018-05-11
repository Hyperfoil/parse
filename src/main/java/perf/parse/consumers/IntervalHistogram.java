package perf.parse.consumers;

import perf.parse.JsonConsumer;
import perf.yaup.json.Json;

import java.util.function.Function;

/**
 * Created by wreicher
 */
public class IntervalHistogram implements JsonConsumer {

    private static class JsonAccessorFunction implements Function<Json, Double> {

        private String key;
        public JsonAccessorFunction(String keyName){
            this.key = keyName;
        }

        @Override
        public Double apply(Json jsonObject) {
            Double value = jsonObject.getDouble(key);
            if(value.isNaN()){
                //System.out.println(jsonObject.toString(2));
            }
            return value;
        }
    }

    int bucketCount;
    int bucketSize;
    long buckets[];
    long count=0;
    Function<Json,Double> converter;

    double previousValue;
    double initialValue;

    public IntervalHistogram(String key){
        this(new JsonAccessorFunction(key),100,1,0);
    }
    public IntervalHistogram(String key,int bucketCount,int bucketSize,double initialValue){
        this(new JsonAccessorFunction(key),bucketCount,bucketSize,initialValue);

    }
    public IntervalHistogram(Function<Json,Double> converter, int bucketCount, int bucketSize, double initialValue){
        this.converter = converter;
        this.bucketCount = bucketCount;
        this.bucketSize = bucketSize;
        this.buckets = new long[this.bucketCount];
        this.initialValue = initialValue;
        this.previousValue = initialValue;
    }

    public void reset(){
        this.count = 0;
        this.buckets = new long[this.bucketCount];
        this.previousValue = this.initialValue;
    }

    @Override
    public void consume(Json object) {
        double newValue = converter.apply(object);

        int bucketId = (int)((newValue-previousValue) / bucketSize);
        if(bucketId>=bucketCount){
            //System.out.println("  "+bucketId+"...");
            bucketId=bucketCount-1;
        }
        //System.out.println(newValue+"->"+bucketId);
        buckets[bucketId]++;
        previousValue = newValue;
        count++;
    }

    public long getCount(){return this.count;}
    public int getBucketCount(){return bucketCount;}
    public int getBucketSize(){return bucketSize;}
    public long[] getBuckets(){return buckets;}

}
