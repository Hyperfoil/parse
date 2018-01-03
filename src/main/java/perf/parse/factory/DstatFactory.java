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
import perf.util.AsciiArt;
import perf.util.json.Json;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.function.Consumer;

/**
 * DstatFactory - creates a Parser for
 */
public class DstatFactory {


    //public static final String ANSI_RESET =  "\u001B[0m";

    private final ArrayList<String> headers = new ArrayList<String>();

    private MatchAction headerMatch = (match, pattern, parser) -> {
        JSONArray arry = match.getJSONArray("header");
        for(int i=0; i<arry.length(); i++){
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
    private MatchAction logMatch = (match, pattern, parser) -> {

    };
    private MatchAction columnMatch = (match, pattern, parser) -> {

        JSONArray arry = match.getJSONArray("column");
        StringBuilder sb = new StringBuilder();
        sb.append("\\s*");
        for(int i=0,h=0; i<arry.length(); i++){
            String column = arry.getString(i);
            if("|".equals(column) || ":".equals(column)){
                sb.append("[:\\|]?");

                h++;
            } else {
                String header = headers.get(h);
                sb.append("(?<");
                sb.append(header.replaceAll("[_\\-/]", ""));
                sb.append(".");
                sb.append(column.replaceAll("[_\\-/]", ""));
                sb.append(">");
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

        entryExp.forEachField(new Consumer<String>() {
            @Override
            public void accept(String s) {
                entryExp.set(s, Value.KMG);
            }
        });
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
        LinkedHashMap<String,String> rEntrantDstatPaths = new LinkedHashMap<>();
//        rEntrantDstatPaths.set("client1","/home/wreicher/specWork/reentrant/reentrant-aio-196/client1.dstat.log");
//        rEntrantDstatPaths.set("client4","/home/wreicher/specWork/reentrant/reentrant-aio-196/client4.dstat.log");
        rEntrantDstatPaths.put("server4","/home/wreicher/perfWork/amq/jdbc/00259/dstat.log");

        for(String fKey : rEntrantDstatPaths.keySet()){
            String fPath = rEntrantDstatPaths.get(fKey);

            TextLineReader r = new TextLineReader();
            DstatFactory f = new DstatFactory();
            final Parser p = new Parser();

            int w = 13;
            StringBuilder v = new StringBuilder();
            StringBuilder h = new StringBuilder();

            p.add(f.defaultMessageExp());
            p.add(f.headerGroupExp());
            p.add(f.columnGroupExp());


            p.add(new JsonConsumer() {
                @Override
                public void start() {}

                @Override
                public void consume(Json object) {
                    if(object.has("totalCpuUsage.idl")){
                        int val = (int)object.getLong("totalCpuUsage.idl");
                        v.append("┃"+ AsciiArt.vert(100-val,100,w,true)+"│");
                        v.append("\n");
                        h.append(AsciiArt.horiz(100-val,100));
                    }
                }
                @Override
                public void close() {}
            });

            r.addParser(p);
            r.read(fPath);
            System.out.printf("%10s.cpu │%s│\n",fKey,h.toString());
            //System.out.println(v.toString());


        }



    }
}
