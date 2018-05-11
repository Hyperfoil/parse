package perf.parse.factory;

import perf.parse.Exp;
import perf.parse.Merge;
import perf.parse.Parser;

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
            .set(Merge.PreClose)
        );
    }
}
