package perf.parse.reader;


import perf.parse.Parser;
import perf.yaup.HashedList;
import perf.yaup.file.FileUtility;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.io.Reader;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 *
 */
public abstract class AReader {


    protected abstract void processInputStream(InputStream stream);

    public void read(String path) {
        setup();
        processInputStream(FileUtility.getInputStream(path));
        close();
    }
    public void read(InputStream stream){
        setup();
        processInputStream(stream);
        close();
    }
    private HashedList<Parser> parsers;

    public AReader(){
        parsers = new HashedList<Parser>();
    }

    public void addParser(Parser toAdd){
        parsers.add(toAdd);
    }
    public int parserCount(){return parsers.size();}

    public void onLine(String line){
        for(int i=0; i<parsers.size();i++){
            parsers.get(i).onLine(line);
        }
    }
    protected void setup(){
        for(int i=0; i<parsers.size();i++){
            parsers.get(i).setup();
        }
    }
    protected void close(){
        for(int i=0; i<parsers.size();i++){
            parsers.get(i).close();
        }
    }
    //protected Iterator<Parser> parsers(){return parsers.iterator();}
}
