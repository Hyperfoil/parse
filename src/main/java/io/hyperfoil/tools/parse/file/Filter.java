package io.hyperfoil.tools.parse.file;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.ReadContext;
import io.hyperfoil.tools.parse.Exp;
import io.hyperfoil.tools.parse.Parser;
import io.hyperfoil.tools.yaup.StringUtil;
import io.hyperfoil.tools.yaup.json.Json;
import io.hyperfoil.tools.yaup.json.YaupJsonProvider;
import io.hyperfoil.tools.yaup.xml.pojo.Xml;
import io.hyperfoil.tools.yaup.xml.pojo.XmlPath;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 */
public class Filter {

    public Json toJson(){
        Json rtrn = new Json();
        if(hasPath()){
            rtrn.set("path",getPath());
        }
        if(hasNest()){
            rtrn.set("nest",getNest());
        }
        if(hasRegex()){
            rtrn.set("regex",getRegex());
        }
        if(hasResult()){
            rtrn.set("result",getResult());
        }
        if(hasExp()){
            Json exps = new Json(true);
            getExps().forEach(exp->{
                exps.add(exp.toJson());
            });
        }

        return rtrn;
    }
    public static Filter fromJson(Json json){
        Filter rtrn = new Filter();
        if(json == null){
            throw new IllegalArgumentException("cannot create Filter from null");
        }

        if(json.has("path")){
            rtrn.setPath(json.getString("path"));
        }
        if(json.has("nest")){
            rtrn.setNest(json.getString("nest"));
        }
        if(json.has("regex")){
            rtrn.setRegex(json.getString("regex"));
        }
        if(json.has("result")){
            rtrn.setResult(json.getString("result"));
        }
        if(json.has("children")){
            if(json.get("children") instanceof Json){
                Json children = json.getJson("children");
                if(children.isArray()){
                    children.forEach(child->{
                        if(child instanceof Json){
                            Filter childFilter = Filter.fromJson((Json)child);
                            rtrn.addChild(childFilter);
                        }else{
                            throw new IllegalArgumentException("cannot create filter child from "+child);
                        }
                    });
                }else{
                    throw new IllegalArgumentException("filter children must be an array: "+json.toString());
                }
            }else{
                throw new IllegalArgumentException("filter children must be an array: "+json.toString());
            }
        }
        if(json.has("exp")){
            if(json.getJson("exp") instanceof Json){
                Json exp = json.getJson("exp");
                if(exp.isArray()){
                    exp.forEach(entry->{
                        if(entry instanceof Json){
                            Exp e = Exp.fromJson((Json)entry);
                            if(e!=null){
                                rtrn.addExp(e);
                            }
                        }else if (entry instanceof String){
                            Exp e = new Exp((String)entry);
                            rtrn.addExp(e);
                        }
                    });
                }else{
                    Exp e = Exp.fromJson(exp);
                    if(e!=null){
                        rtrn.addExp(e);
                    }
                }
            }else{
                throw new IllegalArgumentException("exp must be json"+json.toString());
            }
        }
        return rtrn;
    }
    public static Json getSchemaDefinition(){
        return Json.fromJs(
           "{" +
           "  type: 'object'," +
           "  properties: {" +
           "    path: {type: 'string'}," +
           "    nest: {type: 'string'}," +
           "    regex: {type: 'string'}," +
           "    result: {type: 'string'}," +
           "  }," +
           "  required: ['regex']," +
           "}");
    }

    private static Configuration yaup = Configuration.defaultConfiguration().jsonProvider(new YaupJsonProvider());

    public enum Type {Json,Xml,Unknown};

    private String path="";

    private String nest="";
    private String regex="";
    private String result="";
    private Type type = Type.Json;
    private List<Exp> exps = new ArrayList<>();
    private List<Filter> children = new ArrayList<>();
    public Filter(){}


    public List<Filter> getChildren(){return children;}
    public boolean hasChildren(){return !children.isEmpty();}
    public void addChild(Filter filter){
        this.children.add(filter);
    }

    public List<Exp> getExps(){return  exps;}
    public boolean hasExp(){return !exps.isEmpty();}
    public void addExp(Exp exp){
        this.exps.add(exp);
    }
    public void setType(Type type){this.type = type;}
    public Type getType(){return this.type;}

    public boolean hasPath(){return path!=null && !path.isEmpty();}
    public String getPath() {return path;}
    public Filter setPath(String path) {
        this.path = path;
        if(path.startsWith("$.")){
            this.type = Type.Json;
        }else if (path.startsWith("/")){
            this.type = Type.Xml;
        }else{
            this.type = Type.Unknown;
        }
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
            if( obj instanceof Xml){
                obj = ((Xml)obj).toJson();
            }
            if( !(obj instanceof Json)){
                return matched;
            }
            matched = true;//
            Object rtrn = obj;
            Json inputJson = (Json)obj;
            if(hasPath()) {
                matched = false;//needs to be set true by path matching
                Object results = Json.find(inputJson,getPath(),null);
                if (results != null){
                    matched = true;
                    rtrn = results;
                }
            }
            if(matched){
                if(hasChildren()){
                    Json fromChildren = new Json();
                    Object childInput = rtrn;
                    if(childInput instanceof Json){
                        Json rtrnJson = (Json)rtrn;
                        if(rtrnJson.isArray()){ // apply the children to each entry in the json
                            rtrnJson.values().forEach(rtrnEntry->{
                                Json entryFromChildren = new Json();
                                children.forEach(child->{
                                    child.apply(rtrnEntry,(nest,out)->{
                                        if(nest.isEmpty()){
                                            if(out instanceof Json){
                                                ((Json)out).forEach((k,v)->{
                                                    entryFromChildren.set(k,v);
                                                });
                                            }else{
                                                entryFromChildren.add(out);
                                            }
                                        }else{
                                            Json.chainSet(entryFromChildren, nest, out);
                                        }
                                    });
                                });
                                if(!entryFromChildren.isEmpty()){
                                    fromChildren.add(entryFromChildren);
                                }
                            });
                        }else{
                            children.forEach(child->{
                                child.apply(childInput,(nest,out)->{
                                    Json.chainSet(fromChildren,nest,out);
                                });
                            });
                        }
                    }else{
                        children.forEach(child->{
                            child.apply(childInput,(nest,out)->{
                                Json.chainSet(fromChildren,nest,out);
                            });
                        });
                    }
                }
                if(hasRegex() || hasExp()) {

                    Consumer<Object> consumer = null;
                    Runnable cleanup = null;
                    Json newValues = new Json();
                    if(hasRegex()){
                        Matcher m = Pattern.compile(getRegex()).matcher("");
                        List<String> keys = StringUtil.getCaptureNames(getRegex());
                        consumer = (value) -> {
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
                    }else if (hasExp()){
                        Parser parser = new Parser();
                        parser.add((v)->{
                            newValues.add(v);
                        });
                        getExps().forEach(parser::add);
                        consumer = (value) ->{
                            parser.onLine(value.toString());

                        };
                        cleanup = () -> {
                            parser.close();
                        };
                    }
                    if(rtrn instanceof Json){
                        Json rtrnJson = (Json)rtrn;
                        if(rtrnJson.isArray()){
                            rtrnJson.values().forEach(consumer::accept);
                        }else{
                            consumer.accept(rtrnJson);
                        }
                    }else{
                        consumer.accept(rtrn);
                    }
                    if(cleanup!=null){
                        cleanup.run();
                    }
                    if(newValues.isEmpty()){ // had regex or exp and did not populate a new json
                        matched = false;
                    }else {
                        if(newValues.size()==1){
                            rtrn = newValues.get(0); //if only one result then that is the output, not the array
                        }else {
                            rtrn = newValues;
                        }
                    }
                }
            }

            if(matched && hasResult()) {
                Json newValues = new Json();
                if(getResult().startsWith("function") || getResult().contains("=>")){
                    //TODO javascript value conversion
                    throw new UnsupportedOperationException("js function result generation is not yet supported");
                }else {
                    if(rtrn instanceof Json){
                        Json rtrnJson = (Json)rtrn;
                        if(rtrnJson.isArray()){
                            rtrnJson.values().forEach(value->{
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
                }
                if(!newValues.isEmpty()){
                    rtrn = newValues;
                }
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
