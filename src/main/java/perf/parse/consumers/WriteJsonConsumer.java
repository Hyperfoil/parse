package perf.parse.consumers;

import perf.parse.JsonConsumer;
import perf.yaup.json.Json;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 *
 */
public class WriteJsonConsumer implements JsonConsumer {

    private String fileName;
    private BufferedWriter writer;
    private boolean firstWrite=true;

    public WriteJsonConsumer(String fileName){
        this.fileName = fileName;

        try {
            writer = new BufferedWriter(new FileWriter((new File(fileName))));

        } catch (IOException e) {
            e.printStackTrace();
            System.exit(0);
        }
    }

    public void setFileName(String fileName){
        this.fileName = fileName;
    }
    public String getFileName(){return fileName;}

    @Override public void start() {
        try{
            writer.write("[");

        }catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override public void consume(Json object) {
        try {
            if(!firstWrite) {
                writer.write(",");
                writer.newLine();
            }else{
                firstWrite=false;
            }
            writer.write(object.toString());
            writer.flush();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void close(){
        try {
            writer.write("]");
            writer.flush();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
