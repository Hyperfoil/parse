package io.hyperfoil.tools.parse.reader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 *
 */
public class TextLineReader extends AReader {

    @Override protected void processInputStream(InputStream stream) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
        String line = null;
        try {
            while((line = reader.readLine())!=null){
                onLine(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
