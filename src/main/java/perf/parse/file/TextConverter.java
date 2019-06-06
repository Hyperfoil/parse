package perf.parse.file;

import perf.parse.Exp;
import perf.parse.JsonConsumer;
import perf.parse.Parser;
import perf.yaup.file.FileUtility;
import perf.yaup.json.Json;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

public class TextConverter implements Function<String, Json> {

    private final List<Supplier<Parser>> factories = new ArrayList<>();
    private final List<Exp> exps = new LinkedList<Exp>();

    public TextConverter(){}

    public TextConverter addFactory(Supplier<Parser> factory){
        this.factories.add(factory);
        return this;
    }
    public boolean hasFactory(){return !factories.isEmpty();}
    public List<Supplier<Parser>> getFactories(){return factories;}

    public boolean hasExp(){return !exps.isEmpty();}
    public List<Exp> getExps(){return exps;}
    public TextConverter addExp(Exp exp){
        this.exps.add(exp);
        return this;
    }

    @Override
    public Json apply(String s) {
        Json rtrn = new Json();
        JsonConsumer consumer = (event)-> {
            //TODO filtering?
            rtrn.add(event);
        };
        final List<Parser> parsers = new ArrayList<>();
        if(hasExp()){
            Parser p = new Parser();
            getExps().forEach(p::add);
            parsers.add(p);
        }
        getFactories().forEach(factory->parsers.add(factory.get()));
        parsers.forEach(parser->parser.add(consumer));
        int lineNumber=0;
        String line = null;
        try(BufferedReader inputStream = new BufferedReader(new InputStreamReader(FileUtility.getInputStream(s)))){
            while((line=inputStream.readLine())!=null){
                for(Parser parser : parsers){
                    parser.onLine(line,lineNumber);
                }
                lineNumber++;
            }
        }catch (IOException e) {
            e.printStackTrace();
        }
        parsers.forEach(Parser::close);
        return rtrn;
    }
}
