package perf.parse.factory;

import perf.parse.*;
import perf.yaup.json.Json;

/**
 * Created by wreicher
 * parsers faban xan files
 */
public class XanFactory implements ParseFactory{

    public Exp blank(){
        return new Exp("blank","^\\s*$");
    }
    public Exp title(){
        return new Exp("title","Title: (?<title>.*)");
    }
    public Exp section(){
        return new Exp("section","Section: (?<section>.*)");
    }
    public Exp display(){
        return new Exp("display","Display: (?<display>.*)");
    }
    public Exp dashes(){
        return new Exp("dashes","^-[ -]+$");
    }
    public Exp header(){
        return new Exp("headers","(?<header>.{2,}?)(?:\\s{2,}|$)");
    }

    public Parser newParser(){
        Parser p = new Parser();
        addToParser(p);
        return p;
    }
    public void addToParser(Parser p){
        p.add(
            blank()
            .eat(Eat.Line)
            .execute((line, match, pattern, parser) -> {
                if( parser != null ){
                    int removedIndex = parser.remove("row");

                }
            })
        );
        p.add(
            title()
            .eat(Eat.Line)
            .setRule(ExpRule.PreClose)
        );
        p.add(
            section()
            .eat(Eat.Line)
            .setRule(ExpRule.PreClose)
        );
        p.add(
            display()
            .eat(Eat.Line)
        );
        p.add(
            dashes()
            .eat(Eat.Line)
        );
        p.add(
            header()
            .setRule(ExpRule.Repeat)
            .eat(Eat.Line)
            .execute((line, match, pattern, parser) -> {
                Json arry = match.getJson("header");
                StringBuilder sb = new StringBuilder();
                for(int i=0; i<arry.size(); i++){
                    String header = arry.getString(i);
                    arry.set(i,header);
                    sb.append("(?<"+header+">[^ -].*?)(?:\\s{2,}|$)");
                }
                Exp rowExp = new Exp("row",sb.toString())
                        .eat(Eat.Line).setMerge(ExpMerge.AsEntry);
                if(parser!=null) {
                    int removedIndex = parser.remove("row");

                    parser.addAt(rowExp,1);


                }
            })
        );
    }
}
