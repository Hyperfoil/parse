package io.hyperfoil.tools.parse.reader;


import io.hyperfoil.tools.parse.Parser;
import io.hyperfoil.tools.yaup.HashedList;
import io.hyperfoil.tools.yaup.file.FileUtility;

import java.io.IOException;
import java.io.InputStream;

/**
 *
 */
public abstract class AReader {


    protected abstract void processInputStream(InputStream stream);

    public void read(String path) {
        setup();
        try(InputStream inputStream = FileUtility.getInputStream(path)){
            processInputStream(inputStream);
        } catch (IOException e) {
            e.printStackTrace();
        }
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
