package perf.parse.file;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.ReadContext;
import perf.yaup.StringUtil;
import perf.yaup.json.Json;
import perf.yaup.json.YaupJsonProvider;
import perf.yaup.xml.pojo.Xml;
import perf.yaup.xml.pojo.XmlPath;

import java.util.Arrays;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 */
public class Filter {

    private static Configuration yaup = Configuration.defaultConfiguration().jsonProvider(new YaupJsonProvider());

    public enum Type {Json,Xml};

    private String path="";

    private String nest="";
    private String regex="";
    private String result="";
    private Type type = Type.Json;
    private boolean negated = false;

    public Filter(){}

    public boolean isNegated(){return this.negated;}
    public Filter setNegated(boolean negated){
        this.negated = negated;
        return this;
    }

    public void setType(Type type){this.type = type;}
    public Type getType(){return this.type;}

    public boolean hasPath(){return path!=null && !path.isEmpty();}
    public String getPath() {return path;}
    public Filter setPath(String path) {
        this.path = path;
        return this;
    }

    public boolean hasNest(){return nest!=null && !nest.isEmpty();}
    public String getNest() {return nest;}
    public Filter setNest(String nest) {
        this.nest = nest;
        return this;
    }

    public boolean hasRegex(){return regex!=null & !regex.isEmpty();}
    public String getRegex() {return regex;}
    public Filter setRegex(String regex) {
        this.regex = regex;
        return this;
    }

    public boolean hasResult(){return result!=null && !result.isEmpty();}
    public String getResult() {return result;}
    public Filter setResult(String result) {
        this.result = result;
        return this;
    }
    public boolean apply(Object obj, BiConsumer<String,Object> callback){
        boolean matched = false;
        if(Type.Json.equals(getType())){
            if( !(obj instanceof Json)){
                return matched;
            }
            matched = true;//
            Json rtrn = (Json)obj;
            if(hasPath()) {
                matched = false;//needs to be set true by path matching
                JsonPath jsonPath = JsonPath.compile(getPath());
                ReadContext ctx = JsonPath.parse(rtrn, yaup);//JsonPath.parse(json.toString());
                Object results = ctx.read(jsonPath);
                if(results instanceof Json) {
                    matched = true;
                    rtrn = (Json)results;
                }
            }
            if(matched && hasRegex()) {
                Matcher m = Pattern.compile(getRegex()).matcher("");
                List<String> keys = StringUtil.getCaptureNames(getRegex());
                Json newValues = new Json();
                Consumer<Object> applyRegex = (value) -> {
                    m.reset(value.toString());
                    if (m.matches()) {
                        if (!keys.isEmpty()) {
                            Json newValuesEntry = new Json();
                            keys.forEach(key -> {
                                newValuesEntry.set(key, m.group(key));
                            });
                            newValues.add(newValuesEntry);
                        } else {
                            newValues.add(value.toString().substring(m.start(), m.end()));
                        }
                    }
                };
                if (rtrn.isArray()) {
                    rtrn.values().forEach(applyRegex::accept);
                } else {
                    applyRegex.accept(rtrn);
                }
                if(newValues.isEmpty()){
                    matched = false;
                }else {
                    rtrn = newValues;
                }
            }
            if(matched && hasResult()) {
                Json newValues = new Json();
                if(getResult().startsWith("function") || getResult().contains("=>")){
                    //TODO javascript value conversion
                    throw new UnsupportedOperationException("js function result generation is not yet supported");
                }else {
                    if (rtrn.isArray()) {
                        rtrn.values().forEach(value->{
                            String newValue = getResult();
                            if(value instanceof Json){
                                newValue = StringUtil.populatePattern(getResult(),Json.toObjectMap((Json)value));
                            }
                            newValues.add(newValue);
                        });
                    } else {
                        newValues.add(getResult());
                    }
                }
                rtrn = newValues;
            }
            if(matched) {
                callback.accept(getNest(), rtrn);
            }

        }else if (Type.Xml.equals(getType())){
            if (!(obj instanceof Xml)) {
                return false;
            }
            XmlPath xmlPath = XmlPath.parse(getPath());
            List<Xml> list = Arrays.asList((Xml)obj);
            if(hasPath()){
                matched = false;
                List<Xml> found = xmlPath.getMatches(list.get(0));//we know there's only one entry at this point
                if(!found.isEmpty()){
                    matched = true;
                    list = found;
                }

            }
            if(matched && hasRegex()){
                List<String> keys = StringUtil.getCaptureNames(getRegex());
                Matcher m = Pattern.compile(getRegex()).matcher("");
                list.forEach(xml->{
                    String value = xml.documentString(0,false);
                    m.reset(value);
                    if(value.contains(getRegex()) || m.find()){
                        if(!keys.isEmpty() || m.groupCount() > 0){
                            Json captured = new Json();
                            if(keys.isEmpty()){
                                keys.forEach(key->{
                                    captured.set(key,m.group(key));
                                });
                            }else{
                                for(int i=0; i<m.groupCount(); i++){
                                    captured.set(i,m.group(i));
                                }
                            }
                            if(hasResult()){
                                callback.accept(getNest(),StringUtil.populatePattern(getResult(),Json.toObjectMap(captured)));
                            }else{
                                callback.accept(getNest(),captured);
                            }
                        }else{
                            if(hasResult()){
                                callback.accept(getNest(),getResult());
                            }else{
                                callback.accept(getNest(),value.substring(m.start(),m.end()));
                            }
                        }
                    }
                });
            }else if (matched){//no regex on result from path (if there was one
                if(hasResult()){//result without regex means just pass back the result value
                    callback.accept(getNest(),getResult());
                }else{//
                    if(list.size()==1){
                        callback.accept(getNest(),list.get(0).toJson());
                    }else {
                        Json toReturn = new Json();
                        list.forEach(xml -> toReturn.add(xml.toJson()));
                        callback.accept(getNest(), toReturn);
                    }
                }
            }
        }
        return matched;
    }
}
