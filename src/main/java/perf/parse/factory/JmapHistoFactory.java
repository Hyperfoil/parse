package perf.parse.factory;

import perf.parse.Exp;
import perf.parse.Merge;
import perf.parse.Parser;
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

    public Exp classEntry(){
        return new Exp("class","\\s*(?<num>\\d+):\\s+(?<instances>\\d+)\\s+(?<bytes>\\d+)\\s+(?<className>.+)");
    }
    public Parser newParser(){
        Parser p = new Parser();
        addToParser(p);
        return p;
    }
    public void addToParser(Parser p){
        p.add(
            classEntry()
            .set(Merge.NewStart)
        );
    }
}
