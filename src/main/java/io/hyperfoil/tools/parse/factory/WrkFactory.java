package io.hyperfoil.tools.parse.factory;

import io.hyperfoil.tools.parse.Eat;
import io.hyperfoil.tools.parse.Exp;
import io.hyperfoil.tools.parse.ExpRule;
import io.hyperfoil.tools.parse.Parser;
import io.hyperfoil.tools.yaup.json.Json;

import java.util.ArrayList;

/**
 * Created by johara
 * WrkFactory - creates a Parser for wrk logs
 */
public class WrkFactory implements ParseFactory{

    String test[] = new String[]{
            "Running 1m test @ http://benchserver4G1:8080/fruits\n",
            "  25 threads and 25 connections\n",
            "  Thread Stats   Avg\t  Stdev     Max   +/- Stdev\n",
            "    Latency   472.18us  747.97us  19.98ms   93.33%\n",
            "    Req/Sec     2.97k   145.08     4.73k    74.41%\n",
            "  4434796 requests in 1.00m, 0.85GB read\n",
            "Requests/sec:  73790.28\n",
            "Transfer/sec:     14.57MB\n",
            "Running 1m test @ http://benchserver4G1:8080/fruits\n",
            "  25 threads and 25 connections\n",
            "  Thread Stats   Avg\t  Stdev     Max   +/- Stdev\n",
            "    Latency   471.32us  703.34us  16.86ms   92.85%\n",
            "    Req/Sec     2.95k   132.44     4.59k    74.09%\n",
            "  4406327 requests in 1.00m, 869.86MB read\n",
            "Requests/sec:  73318.30\n",
            "Transfer/sec:     14.47MB"
    };


    public Exp headerGroup(){
        return new Exp("header","Running (?<time>[0-9]*[a-zA-Z]+ )\\@ (?<url>\\\\b(https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]");

    }

    public Exp statsGroup(){ return null;}
    public Exp summaryGroup(){ return null;}

//    public Exp headerGroup(){
//        return new Exp("headerGroup","[ ]?[\\-]{1,}(?<header>[^ \\-]+(:?[\\-\\/]?[^ \\-\\/]+)*)[\\- ]{1}");
//    }
//    public Exp columnGroup(){
//        return new Exp("columnGroup","\\s*(?<column>[\\:\\|]|[^\\s\\:\\|]+)");
//    }


    public Parser newParser(){
        Parser p = new Parser();
        addToParser(p);
        return p;
    }
    public void addToParser(Parser p){
        final ArrayList<String> headers = new ArrayList<>();
        p.add(
            headerGroup()
            .addRule(ExpRule.PreClose)
            .addRule(ExpRule.Repeat)
            .eat(Eat.Line)
            .execute((line, match, pattern, parser) -> {
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
            })
        );
        p.add(
            columnGroup()
            .addRule(ExpRule.PreClose)
            .addRule(ExpRule.Repeat)
            .eat(Eat.Line)
            .execute((line, match, pattern, parser) -> {
                Json arry = match.getJson("column");
                StringBuilder sb = new StringBuilder();
                sb.append("\\s*");
                int h = 0;
                for(int i=0; i<arry.size(); i++){
                    String column = arry.getString(i);
                    if("|".equals(column) || ":".equals(column)){
                        sb.append("[:\\|]?");
                        h++;
                    } else {
                        if(h>=headers.size()){
                            System.out.println(h+" > "+headers.size()+"\n  headers="+headers+"\n  line="+line);
                        }
                        String header = headers.get(h);
                        if(header==null){
                            System.out.println("header @ "+h+" is null");
                        }
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
                entryExp.addRule(ExpRule.PreClose);
                entryExp.eat(Eat.Line);
                parser.addAhead(entryExp);
            })

        );
    }
}
