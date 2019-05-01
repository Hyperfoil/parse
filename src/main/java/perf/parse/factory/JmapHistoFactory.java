package perf.parse.factory;

import perf.parse.*;

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
