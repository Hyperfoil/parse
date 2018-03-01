package perf.parse.factory;

import perf.parse.Exp;
import perf.parse.Merge;
import perf.parse.Parser;
import perf.parse.Value;
import perf.parse.reader.TextLineReader;
import perf.yaup.TimestampedData;
import perf.yaup.file.FileUtility;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by wreicher
 */
public class JmapHistoFactory {

    public Exp classEntryPattern(){
        return new Exp("class","\\s*(?<num>\\d+):\\s+(?<instances>\\d+)\\s+(?<bytes>\\d+)\\s+(?<className>.+)")
            .set(Merge.NewStart)
            .set("num", Value.Number)
            .set("instances",Value.Number)
            .set("bytes",Value.Number);
    }

    public Parser newParser(){
        Parser p = new Parser();
        p.add(classEntryPattern());
        return p;
    }

    public static void main(String[] args) {

        PrintStream out = System.out;

        String filePath = "";
        filePath = "/home/wreicher/specWork/260L/28031.20160310_131404.histo.txt";

        SimpleDateFormat sdf = new SimpleDateFormat("YYYYMMdd_HHmmss");

        Matcher m = Pattern.compile(".*(\\d{4}\\d{2}\\d{2}_\\d{2}\\d{2}\\d{2}).*").matcher("");

        TimestampedData<Double> count = new TimestampedData<>();
        TimestampedData<Double> bytes = new TimestampedData<>();

        List<String> inputFiles = FileUtility.getFiles("/home/wreicher/specWork/260P/",".histo.",true);

        try {
            out = new PrintStream(new FileOutputStream("/home/wreicher/specWork/260P/histo.csv"));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            System.exit(0);
        }

        inputFiles.sort(String.CASE_INSENSITIVE_ORDER);
        for(String file : inputFiles){
            System.out.println(file);
            if(m.reset(file).matches()){
                try {
                    Date d = sdf.parse(m.group(1));
                    long timestamp = d.getTime();
                    JmapHistoFactory jhf = new JmapHistoFactory();

                    Parser p = jhf.newParser();
                    p.add(json->{
                        count.add(json.getString("className"),timestamp,json.getDouble("instances"));
                        bytes.add(json.getString("className"),timestamp,json.getDouble("bytes"));
                    });
                    TextLineReader r = new TextLineReader();
                    r.addParser(p);
                    r.read(file);

                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }


        }

        int size = count.names().stream().mapToInt(value -> value.length()).max().getAsInt();
        System.out.println("size = "+size);
        System.out.println(count.timestamps());
        //System.exit(0);
        out.print("className,");
        for( long timestamp : count.timestamps()){
            out.printf("%d,",timestamp);
        }
        out.printf("%n");
        for( String key : count.names() ){
            out.printf("%s, ",key);
            for( long timestamp : count.timestamps() ){
                out.printf("%.0f, ",bytes.get(timestamp,key));
            }
            out.printf("%n");
            //System.exit(0);
        }


    }
}
