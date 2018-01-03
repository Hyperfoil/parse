package perf.parse.reader;

import perf.parse.internal.CheatChars;
import perf.parse.Parser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Iterator;

/**
 *
 */
public class TextLineReader extends AReader {

    @Override protected void processInputStream(InputStream stream) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
        String line = null;
        Iterator<Parser> iter = null;
        try {
            while((line = reader.readLine())!=null){
                iter = parsers();
                while(iter.hasNext()){
                    iter.next().onLine(new CheatChars(line));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
