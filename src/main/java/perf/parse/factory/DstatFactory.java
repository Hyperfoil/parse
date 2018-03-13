package perf.parse.factory;

import org.json.JSONArray;
import perf.parse.Eat;
import perf.parse.Exp;
import perf.parse.JsonConsumer;
import perf.parse.MatchAction;
import perf.parse.Merge;
import perf.parse.Parser;
import perf.parse.Rule;
import perf.parse.Value;
import perf.parse.reader.TextLineReader;
import perf.yaup.AsciiArt;
import perf.yaup.json.Json;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.function.Consumer;

/**
 * Created by wreicher
 * DstatFactory - creates a Parser for
 */
public class DstatFactory {


    //public static final String ANSI_RESET =  "\u001B[0m";

    private final ArrayList<String> headers = new ArrayList<String>();

    private MatchAction headerMatch = (line, match, pattern, parser) -> {
        Json arry = match.getJson("header");
        for(int i=0; i<arry.size(); i++){
            String header = arry.getString(i);
            if(header.matches(".*[_\\-/].*")){
                StringBuilder fixHeader = new StringBuilder(header.length());
                for(int c=0; c<header.length(); c++){
                    char l = header.charAt(c);
                    if( "_/-".indexOf(l)>-1){
                        c++;
                        if(c<header.length()){
                          fixHeader.append(Character.toUpperCase(header.charAt(c)));
                        }
                    }else{
                        fixHeader.append(l);
                    }
                }
                headers.add(fixHeader.toString());
            }else{
                headers.add(header);
            }

        }
    };
    private MatchAction logMatch = (line, match, pattern, parser) -> {

    };
    private MatchAction columnMatch = (line, match, pattern, parser) -> {
        Json arry = match.getJson("column");
        StringBuilder sb = new StringBuilder();
        sb.append("\\s*");
        for(int i=0,h=0; i<arry.size(); i++){
            String column = arry.getString(i);
            if("|".equals(column) || ":".equals(column)){
                sb.append("[:\\|]?");
                h++;
            } else {
                String header = headers.get(h);
                sb.append("(?<");
                sb.append(header.replaceAll("[_\\-/]", ""));
                sb.append("\\.");
                sb.append(column.replaceAll("[_\\-/]", ""));
                sb.append(":KMG>");
                if ("time".equals(column)) {
                    sb.append("\\d{1,2}\\-\\d{1,2} \\d{2}:\\d{2}:\\d{2}");
                } else {
                    sb.append("\\-|\\d+\\.?\\d*[KkMmGgBb]?");
                }
                sb.append(")");
            }
            sb.append("\\s*");
        }
        Exp entryExp = new Exp("dstat",sb.toString());
            entryExp.execute(logMatch);
            entryExp.set(Merge.NewStart);
            entryExp.eat(Eat.Line);
        parser.addAhead(entryExp);
    };

    public Exp defaultMessageExp(){
        return new Exp("default","You did not select any stats, using -cdngy by default")
            .set(Merge.NewStart)
            .eat(Eat.Line);
    }
    public Exp headerGroupExp(){
        return new Exp("header","[ ]?[\\-]{1,}(?<header>[^ \\-]+(:?[\\-\\/]?[^ \\-\\/]+)*)[\\- ]{1}")
            .set(Merge.NewStart)
            .set(Rule.Repeat)
            .eat(Eat.Line)
            .execute(headerMatch);
    }
    public Exp columnGroupExp(){
        return new Exp("columns","\\s*(?<column>[\\:\\|]|[^\\s\\:\\|]+)")
            .set(Merge.NewStart)
            .set(Rule.Repeat)
            .eat(Eat.Line)
            .execute(columnMatch);
    }

    public Parser newParser(){
        Parser rtrn = new Parser();
        rtrn.add(this.defaultMessageExp());
        rtrn.add(this.headerGroupExp());
        rtrn.add(this.columnGroupExp());
        return rtrn;
    }

    public static void main(String[] args) {
        String filePath = "/home/wreicher/perfWork/meltdown/after/2010/365J/archive/run/benchclient3.perf.lab.eng.rdu.redhat.com/dstat.log";

        Parser p =  new DstatFactory().newParser();
        p.add((object -> {
            System.out.println(object.toString());
        }));

        try {
            Files.lines(new File(filePath).toPath()).forEach(p::onLine);
            p.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
