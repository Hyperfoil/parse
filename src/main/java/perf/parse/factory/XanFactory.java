package perf.parse.factory;

import perf.parse.*;
import perf.parse.reader.TextLineReader;
import perf.yaup.json.Json;

import javax.xml.bind.annotation.XmlList;
import java.util.ArrayList;

/**
 * Created by wreicher
 * parsers faban xan files
 */
public class XanFactory {

    private MatchAction emptyMatch = (line, match, pattern, parser) -> {
        if( parser != null ){
            int removedIndex = parser.remove("row");
        }
    };
    private MatchAction headerMatch = (line, match, pattern, parser) -> {

        Json arry = match.getJson("header");

        StringBuilder sb = new StringBuilder();
        for(int i=0; i<arry.size(); i++){
            String header = arry.getString(i);
            if(header.indexOf(" ")>0){
                header = header.substring(0,header.indexOf(" "));
            }
            header = header.replaceAll("[_\\-/]", "");
            arry.set(i,header);
            sb.append("(?<"+header+">[^ -].*?)(?:\\s{2,}|$)");
        }
        Exp rowExp = new Exp("row",sb.toString())
            .eat(Eat.Line).set(Merge.Entry);

        if(parser!=null) {

            int removedIndex = parser.remove("row");
            parser.addAt(rowExp,1);


        }
    };

    public Exp blankExp(){
        return new Exp("blank","^\\s*$").eat(Eat.Line).execute(emptyMatch);
    }
    public Exp titleExp(){
        return new Exp("title","Title: (?<title>.*)").eat(Eat.Line).set(Merge.NewStart);
    }
    public Exp sectionExp(){
        return new Exp("section","Section: (?<section>.*)").eat(Eat.Line).set(Merge.NewStart);
    }
    public Exp displayExp(){
        return new Exp("display","Display: (?<display>.*)").eat(Eat.Line);
    }

    public Exp dashExp(){return new Exp("dashes","-[ -]+").eat(Eat.Line);}
    public Exp headerExp(){
        return new Exp("headers","(?<header>.{2,}?)(?:\\s{2,}|$)")
            .set(Rule.Repeat)
            .eat(Eat.Line)
            .execute(headerMatch);
    }

    public Parser newParser(){
        Parser rtrn = new Parser();

        rtrn.add(this.blankExp());
        rtrn.add(this.titleExp());
        rtrn.add(this.sectionExp());
        rtrn.add(this.displayExp());
        rtrn.add(this.dashExp());
        rtrn.add(this.headerExp());

        return rtrn;
    }
}
