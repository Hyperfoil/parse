package io.hyperfoil.tools.parse.factory;

import io.hyperfoil.tools.parse.Exp;
import io.hyperfoil.tools.parse.ExpRule;
import io.hyperfoil.tools.parse.Parser;

/**
 * Created by wreicher
 */
public class JmapHistoFactory implements ParseFactory{

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
            .addRule(ExpRule.PreClose)
        );
    }
}
