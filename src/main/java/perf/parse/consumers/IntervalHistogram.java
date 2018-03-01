package perf.parse.consumers;

import perf.parse.JsonConsumer;
import perf.parse.Parser;
import perf.parse.factory.OpenJdkGcFactory;
import perf.parse.reader.TextLineReader;
import perf.yaup.AsciiArt;
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

    public static void main(String[] args) {

        TextLineReader r = new TextLineReader();
        OpenJdkGcFactory f = new OpenJdkGcFactory();
        Parser p = f.newGcParser();

        JsonKeyMapConsumer keyMap = new JsonKeyMapConsumer();
        IntervalHistogram ih = new IntervalHistogram("elapsed",50,1,0);

        p.add(keyMap);
        p.add(ih);

        r.addParser(p);
        r.read("/home/wreicher/specWork/server.256Q.gclog");

        System.out.println("Time between GC:");
        long countA = ih.getCount();
        long histoA[] = ih.getBuckets();
        ih.reset();
        r.read("/home/wreicher/specWork/server.258A.gclog");
        long countB = ih.getCount();
        long histoB[] = ih.getBuckets();
        ih.reset();

        String aColor = AsciiArt.ANSI_YELLOW;
        String bColor = AsciiArt.ANSI_CYAN;

        StringBuilder sb = new StringBuilder();
        for(int i=0; i<ih.getBucketCount(); i++){
            sb.append(aColor);
            char a = AsciiArt.horiz(1.0*histoA[i]/countA,1.0);
            sb.append(a);
            sb.append(bColor);
            char b = AsciiArt.horiz(1.0*histoB[i]/countB,1.0);
            sb.append(b);
        }
        sb.append(AsciiArt.ANSI_RESET);
        System.out.println(aColor+"256Q - "+countA+AsciiArt.ANSI_RESET+" "+bColor+"258A - "+countB+AsciiArt.ANSI_RESET);
        System.out.println(sb.toString());

    }
}
