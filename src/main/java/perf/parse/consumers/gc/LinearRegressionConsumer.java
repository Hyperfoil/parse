package perf.parse.consumers.gc;

import org.apache.commons.math3.stat.regression.RegressionResults;
import org.apache.commons.math3.stat.regression.SimpleRegression;
import perf.parse.JsonConsumer;
import perf.parse.Parser;
import perf.parse.consumers.JsonKeyMapConsumer;
import perf.parse.factory.OpenJdkGcFactory;
import perf.parse.reader.TextLineReader;
import perf.yaup.AsciiArt;
import perf.yaup.file.FileUtility;
import perf.yaup.json.Json;

import java.util.List;
import java.util.function.Function;

/**
 * Created by wreicher
 */
public class LinearRegressionConsumer implements JsonConsumer {

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

    Function<Json,Double> converterX;
    Function<Json,Double> converterY;
    SimpleRegression regression;

    long count = 0;
    double sum = 0;

    public LinearRegressionConsumer(String keyX, String keyY){
        this(new JsonAccessorFunction(keyX),new JsonAccessorFunction(keyY));
    }
    public LinearRegressionConsumer(Function<Json,Double> converterX, Function<Json,Double> converterY){
        this.converterX = converterX;
        this.converterY = converterY;
        this.regression = new SimpleRegression();
    }

    @Override
    public void consume(Json object) {
        Double x = converterX.apply(object);
        Double y = converterY.apply(object);

        if(!y.isNaN() && !x.isNaN() && x > 120){
            regression.addData(x,y);
        }
    }

    public RegressionResults getRegression(){
        return regression.regress();
    }

    public static void main(String[] args) {
        TextLineReader r = new TextLineReader();
        OpenJdkGcFactory f = new OpenJdkGcFactory();
        Parser p = f.newGcParser();

        JsonKeyMapConsumer keyMap = new JsonKeyMapConsumer();
        p.add(keyMap);

        LinearRegressionConsumer sc = new LinearRegressionConsumer(
                new JsonAccessorFunction("elapsed"),
                json-> {
                    if( json.has("heap") ){
                     return json.getJson("heap").getDouble("postgc");
                    }
                     return Double.NaN;
                });
        p.add(sc);

        r.addParser(p);

        List<String> files = FileUtility.getFiles("/home/wreicher/perfWork/amq/jdbc/00259/client1/",".gclog",true);

        files.clear();

        files.add("/home/wreicher/perfWork/amq/jdbc/00259/client1/specjms.verbose-gc-dc.gclog");
        files.add("/home/wreicher/perfWork/amq/jdbc/00259/client1/specjms.verbose-gc-hq.gclog");
        files.add("/home/wreicher/perfWork/amq/jdbc/00259/client1/specjms.verbose-gc-sm.gclog");
        files.add("/home/wreicher/perfWork/amq/jdbc/00259/client1/specjms.verbose-gc-sp.gclog");

        for(String file : files){
            System.out.println(AsciiArt.ANSI_BLUE+file+AsciiArt.ANSI_RESET);
            r.read(file);
            System.out.printf("[%6d] %s±%s/s  + %s±%s%n",
                sc.regression.getN(),
                AsciiArt.printKMG(sc.regression.getSlope()),
                AsciiArt.printKMG(sc.regression.getSlopeConfidenceInterval()),
                AsciiArt.printKMG(sc.regression.getIntercept()),
                AsciiArt.printKMG(sc.regression.getSlopeConfidenceInterval()));
            sc.regression.clear();
        }
    }
}
