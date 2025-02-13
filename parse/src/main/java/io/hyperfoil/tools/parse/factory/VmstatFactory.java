package io.hyperfoil.tools.parse.factory;

import io.hyperfoil.tools.parse.Eat;
import io.hyperfoil.tools.parse.Exp;
import io.hyperfoil.tools.parse.ExpRule;
import io.hyperfoil.tools.parse.Parser;
import io.hyperfoil.tools.yaup.json.Json;

import java.util.ArrayList;

/**
 * Created by wreicher
 * VmstatFactory - creates a Parser for vmstat logs
 */
public class VmstatFactory implements ParseFactory{

    public Exp headerGroup(){
        return new Exp("headerGroup","(?<header>(?:^| -+)[^ ]+)");
    }
    public Exp columnGroup(){
        return new Exp("columnGroup","(?<column>\\s*\\S+)");
    }
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
                arry.forEach(entry->headers.add(entry.toString()));
            })
        );
        p.add(
            columnGroup()
            .addRule(ExpRule.PreClose)
            .addRule(ExpRule.Repeat)
            .eat(Eat.Line)
            .execute((line, match, pattern, parser) -> {
                Json columns = match.getJson("column");
                StringBuilder sb = new StringBuilder();
                sb.append("\\s*");
                int h = 0;
                int headerIndex=0;
                int headerEnd=headers.get(headerIndex).length();
                int columnEnd=0;
                for(int i=0; i<columns.size(); i++){
                    String column = columns.getString(i);
                    columnEnd+=column.length();

                    column = column.trim().replaceAll("[_\\-/ ]", "");

                    if(columnEnd > headerEnd){
                        headerIndex++;
                        headerEnd+=headers.get(headerIndex).length();
                    }
                    String header = headers.get(headerIndex).replaceAll("[_\\-/ ]","");
                    sb.append("(?<");
                    sb.append(header);
                    sb.append("\\.");
                    sb.append(column);

                    if ("timestamp".equals(header)) {
                        sb.append(">");
                        sb.append("\\d{4}\\-\\d{1,2}-\\d{1,2} \\d{2}:\\d{2}:\\d{2}");
                    } else {
                        sb.append(":KMG>");
                        sb.append("\\-|\\d+\\.?\\d*[KkMmGgBb]?");
                    }
                    sb.append(")");

                    sb.append("\\s*");
                }
                Exp entryExp = new Exp("vmstat",sb.toString());
                entryExp.addRule(ExpRule.PreClose);
                entryExp.eat(Eat.Line);
                parser.addAhead(entryExp);
            })

        );
    }
}
