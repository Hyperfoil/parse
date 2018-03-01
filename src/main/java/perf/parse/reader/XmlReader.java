package perf.parse.reader;

import org.json.JSONObject;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;
import perf.parse.JsonConsumer;
import perf.yaup.json.Json;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Stack;

/**
 *
 */
public class XmlReader extends AReader {

    private Stack<String> nameStack;
    private Stack<JSONObject> jsonStack;
    private int qDepth = 0;

    private StringBuilder sb;

    private DefaultHandler handler;
    private Map<String,LinkedList<JsonConsumer>> consumers;

    public XmlReader(){
        nameStack = new Stack<>();
        jsonStack = new Stack<>();
        sb = new StringBuilder();

        consumers = new LinkedHashMap<>();
        handler = new DefaultHandler(){
            @Override public void  startDocument(){ }
            @Override public void endDocument(){ }
            @Override public void startElement(String uri, String localName, String qName, Attributes attributes){
                qDepth++;
                if( !jsonStack.isEmpty() || consumers.containsKey(qName) ) {
                    JSONObject toPush = new JSONObject();
                    if(!jsonStack.isEmpty()){
                        JSONObject peek = jsonStack.peek();
                        peek.accumulate(qName,toPush);
                    }
                    nameStack.push(qName);
                    jsonStack.push(toPush);
                }
            }
            @Override public void endElement(String uri, String localName, String qName){
                qDepth--;
                if(!jsonStack.isEmpty()){
                    if( sb.length() > 0 ) {

                        jsonStack.pop();
                        jsonStack.peek().put(qName,sb.toString());
                    }else{
                        JSONObject pop = jsonStack.pop();
                        Json poped = Json.fromJSONObject(pop);
                        if(consumers.containsKey(qName)){
                            for( JsonConsumer c : consumers.get(qName) ) {
                                c.consume(poped);
                            }
                        }
                    }
                    nameStack.pop();
                    sb.setLength(0);
                }
            }
            @Override public void characters(char[] ch, int start, int length){

                if(!jsonStack.isEmpty()){
                    String characters = new String(ch,start,length).trim();
                    if(!characters.isEmpty()){

                        sb.append(characters);
                    }
                }
            }
            @Override public void ignorableWhitespace(char[] ch,int start,int length) throws SAXException{}

        };
    }
    public void add(String qName,JsonConsumer consumer){
        LinkedList<JsonConsumer> list = consumers.get(qName);
        if( list == null){
            list = new LinkedList<>();
            consumers.put(qName,list);
        }
        list.add(consumer);
    }

    @Override protected void processInputStream(InputStream stream) {
        SAXParserFactory saxFactory = SAXParserFactory.newInstance();

        for(List<JsonConsumer> list : consumers.values()){
            for(JsonConsumer c : list){
                c.start();
            }
        }

        try {
            SAXParser saxParser = saxFactory.newSAXParser();
            saxParser.parse(stream,handler);

        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        for(List<JsonConsumer> list : consumers.values()){
            for(JsonConsumer c : list){
                c.close();
            }
        }
    }
}
