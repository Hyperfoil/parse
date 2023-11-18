package io.hyperfoil.tools.parse.file;

import io.hyperfoil.tools.parse.JsStringFunction;
import io.hyperfoil.tools.parse.Parser;
import io.hyperfoil.tools.parse.Exp;
import io.hyperfoil.tools.parse.JsJsonFunction;
import io.hyperfoil.tools.yaup.PopulatePatternException;
import io.hyperfoil.tools.yaup.StringUtil;
import io.hyperfoil.tools.yaup.json.Json;
import io.hyperfoil.tools.yaup.json.graaljs.JsException;
import org.slf4j.ext.XLogger;
import org.slf4j.ext.XLoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

public class FileRule {

    final static XLogger logger = XLoggerFactory.getXLogger(MethodHandles.lookup().lookupClass());

    public static FileRule fromJson(Json json, Map<Object,Object> state){
        FileRule rtrn = new FileRule(json.getString("name",""));
        rtrn.addWith(state);
        if(json.has("nest")){
            rtrn.setNest(json.getString("nest"));
        }
        if(json.has("path")){
            rtrn.getCriteria().setPathPattern(json.getString("path"));
        }
        if(json.has("headerLines")){
            rtrn.getCriteria().setHeaderLines((int)json.getLong("headerLines"));
        }
        if(json.has("filters")){
            json.getJson("filters").values().stream().map(o->(Json)o).forEach(jsonFilter->{
                Filter f = Filter.fromJson(jsonFilter);
                if(f != null){
                    rtrn.addFilter(f);
                }else{
                    //TODO log error creating filter?
                }
            });
        }
        if(json.has("findHeader")){
            Object findHeader = json.get("findHeader");
            if(findHeader instanceof String){
                rtrn.getCriteria().addFindPattern(findHeader.toString());
            }else if (findHeader instanceof Json){
                Json findList = (Json)findHeader;
                findList.values().forEach(findIt->{
                    rtrn.getCriteria().addFindPattern(findIt.toString());
                });
                if(rtrn.getCriteria().getHeaderLines() < findList.size()){
                    rtrn.getCriteria().setHeaderLines(findList.size());
                }
            }else{
                //TODO alert error
            }
        }
        if(json.has("avoidHeader")){
            Object avoidheader = json.get("avoidHeader");
            if(avoidheader instanceof String){
                rtrn.getCriteria().addNotFindPattern(avoidheader.toString());
            }else if (avoidheader instanceof Json){
                Json avoidList = (Json)avoidheader;
                avoidList.values().forEach(avoidIt->{
                    rtrn.getCriteria().addNotFindPattern(avoidIt.toString());
                });
            }else{
                //TODO alert error
            }
        }
        if (json.has("asContent")) {
            Object asContent = json.get("asContent");
            ContentConverter converter = new ContentConverter();
            String asContentStr = asContent.toString();
            try{
                asContentStr = StringUtil.populatePattern(asContentStr,state);
            }catch(PopulatePatternException e){
                logger.error(e.getMessage());
            }
            converter.setKey(asContentStr);
            rtrn.setConverter(converter);
        }
        if(json.has("asText")){
            TextConverter converter = new TextConverter();
            rtrn.setConverter(converter);
            Object asText = json.get("asText");
            if(asText instanceof String){
                String str = asText.toString();
                try{
                    str = StringUtil.populatePattern(str,state);
                }catch(PopulatePatternException e){
                    logger.error(e.getMessage());
                }
                String ref = str;
                converter.addFactory(()-> Parser.fromJson(ref));
            }else if (asText instanceof Json){
                Json expList = (Json)asText;
                expList.values().forEach(expData ->{
                    if(expData instanceof String){
                        Exp exp = new Exp(expData.toString());
                        converter.addExp(exp);
                    }else if (expData instanceof Json){
                        Exp exp = Exp.fromJson((Json)expData);
                        converter.addExp(exp);
                    }else{
                        //TODO alert error
                    }
                });
            }
        }
        if(json.has("asJbossCli")){
            Object asJbossCli = json.get("asJbossCli");
            if(asJbossCli instanceof String){
                String asString = asJbossCli.toString();
                try{
                    asString = StringUtil.populatePattern(asString,state);
                }catch(PopulatePatternException e){
                    logger.error(e.getMessage());
                }

                if(asString.isEmpty()) {
                    rtrn.setConverter(new JbossCliConverter());
                }else{
                    rtrn.setConverter(new JbossCliConverter().andThen(new JsJsonFunction(asString)));
                }
            }else {
                //TODO alert error
            }
        }
        if(json.has("asJson")){
            Object asJson = json.get("asJson");
            if(asJson instanceof String){
                String asString = (String)asJson;
                try {
                    asString = StringUtil.populatePattern(asString,state);
                }catch (PopulatePatternException e){
                    logger.error(e.getMessage());
                }
                if(asString.isEmpty()){
                    rtrn.setConverter(new JsonConverter());
                }else{
                    rtrn.setConverter(new JsonConverter().andThen(new JsJsonFunction(asString)));
                }
            }
        }
        if(json.has("asXml")){
            Object asXml = json.get("asXml");
            if(asXml instanceof String){
                String asString = (String)asXml;
                try {
                    asString = StringUtil.populatePattern(asString,state);
                }catch (PopulatePatternException e){
                    logger.error(e.getMessage());
                }
                if(asString.isEmpty()){
                    rtrn.setConverter(new XmlConverter());
                }else{
                    rtrn.setConverter(new XmlConverter().andThen(new JsJsonFunction(asString)));
                }
            }else if (asXml instanceof Json){
                XmlConverter converter = new XmlConverter();
                ((Json)asXml).values().stream().map(o->(Json)o).forEach(entry->{
                    Filter f = Filter.fromJson(entry);
                    if(f!=null) {
                        converter.addFilter(f);
                    }
                });
                rtrn.setConverter(converter);
            }
        }
        if(json.has("asPath")){
            rtrn.setConverter(FileRule.asPath(json.getString("asPath")));
        }
        return rtrn;
    }
    public static Function<String,Json> asPath(String function){
        return new JsStringFunction(function);
//        return (path)->{
//            try(Context context = Context.newBuilder("js").allowAllAccess(true).allowHostAccess(true).build()){
//                context.enter();
//                try {
//                    context.eval("js", "function milliseconds(v){ return Packages.io.hyperfoil.tools.yaup.StringUtil.parseKMG(v)};");
//                    context.eval("js", "const StringUtil = Packages.io.hyperfoil.tools.yaup.StringUtil;");
//                    context.eval("js", "const FileUtility = Packages.io.hyperfoil.tools.yaup.file.FileUtility;");
//                    context.eval("js", "const Exp = Java.type('io.hyperfoil.tools.parse.Exp');");
//                    context.eval("js", "const ExpMerge = Java.type('io.hyperfoil.tools.parse.ExpMerge');");
//                    context.eval("js", "const MatchRange = Java.type('io.hyperfoil.tools.parse.MatchRange');");
//                    context.eval("js", "const Xml = Java.type('io.hyperfoil.tools.yaup.xml.pojo.Xml');");
//                    context.eval("js", "const Json = Java.type('io.hyperfoil.tools.yaup.json.Json');");
//                    context.eval("js", "const Eat = Java.type('io.hyperfoil.tools.parse.Eat');");
//                    context.eval("js", "const ValueType = Java.type('io.hyperfoil.tools.parse.ValueType')");
//                    context.eval("js", "const ValueMerge = Java.type('io.hyperfoil.tools.parse.ValueMerge');");
//                    context.eval("js", "const ExpRule = Java.type('io.hyperfoil.tools.parse.ExpRule')");
//                    //context.eval("js","");
//
//                    //context.eval("js","const console = {log: print}");
//
//                    Value matcher = context.eval("js", function);
//                    Value result = matcher.execute(path);
//                    if (result.isHostObject()) {
//                        Object hostObj = result.asHostObject();
//                        if (hostObj instanceof Json) {
//                            return (Json) hostObj;
//                        }
//                    } else if (result.hasMembers()) {
//                        //TODO convert value to Json
//                    }
//                }catch(Exception e){
//                    throw new RuntimeException("asPath exception for "+path,e);
//                }finally {
//                    context.leave();
//                }
//            }
//            //TODO alert that failed to return from converter
//            return new Json();
//        };
    }
    public static Json getSchema(){
        Json rtrn = new Json();
        rtrn.set("$schema","http://json-schema.org/draft-07/schema");
        rtrn.set("definitions",new Json());
        rtrn.getJson("definitions").set("filter",Filter.getSchemaDefinition());
        rtrn.getJson("definitions").set("rule",getSchemaDefinition("filter"));
        rtrn.getJson("definitions").set("exp",Exp.getSchemaDefinition("exp"));
        rtrn.set("$ref","#/definitions/rule");
        return rtrn;
    }
    public static Json getSchemaDefinition(String filterRef){
        return Json.fromJs("{" +
           "type: 'object'," +
           "properties: {" +
           "  name: {type: 'string'}," +
           "  nest: {type: 'string'}," +
           "  path: {type: 'string'}," +
           "  headerLines: {type: 'number'}," +
           "  filter: {type: 'array', items: { $ref: '#/definitions/"+filterRef+"' } }," +
           "  findHeader: { oneOf: [ {type: 'string'} , { type: 'array', items: { type: 'string' } } ] }," +
           "  avoidHeader: { oneOf: [ {type: 'string'} , { type: 'array', items: { type: 'string' } } ] }," +
           "  asText: {oneOf: [" +
           "    { type: 'string'}," +//supplier name or exp string
           "    { type: 'array', items: { $ref: '#/definitions/exp' } }," +
           "  ]}," +
           "  asJbossCli: {oneOf: [" +
           "    { type: 'string'}," +//empty string or javascript function (json)=>json
           "  ]}," +
           "  asJson: {oneOf: [" +
           "    { type: 'string'}," +//empty string or javascript function (json)=>json
           "  ]}," +
           "  asXml: {oneOf: [" +
           "    { type: 'string'}," +
           "    { type: 'array', items: { $ref: '#/definitions/filter'} }," +
           "  ]}," +
           "  asPath: {type: ['string','null']}," +
           "}," +
           "oneOf: [" +
           "  { required: ['asContent'] }," +
           "  { required: ['asJbossCli'] }," +
           "  { required: ['asJson'] }," +
           "  { required: ['asText'] }," +
           "  { required: ['asPath'] }," +
           "  { required: ['asXml'] }," +
           "]," +
           "additionalProperties: false" +
           "}");
    }


    private String name;

    private Json with;
    private MatchCriteria criteria = new MatchCriteria();
    private final List<Filter> filters = new LinkedList<>();
    private String nest="";
    private Function<String, Json> converter =null;

    public FileRule(){this("");}
    public FileRule(String name){
        this.name = name;
        this.with = new Json(false);
    }

    public void addWith(Map<Object,Object> toLoad){
        if(toLoad!=null) {
            toLoad.forEach((k, v) -> addWith(k,v));
        }
    }
    public void addWith(Object key, Object value){
        with.add(key,value);
    }

    public void setName(String name){this.name = name;}
    public String getName(){return name;}

    public boolean isValid(){
        return converter !=null && nest!=null;
    }
    public FileRule setCriteria(MatchCriteria criteria){
        this.criteria=criteria;
        return this;
    }
    public FileRule withCriteria(Consumer<MatchCriteria> consumer){
        consumer.accept(this.criteria);
        return this;
    }
    public FileRule addFilter(Filter filter){
        this.filters.add(filter);
        return this;
    }
    public FileRule accept(String acceptPattern){
        //TODO create a filter that accepts json that match
        return this;
    }
    public FileRule block(String pattern){
        //TODO create a filter aht acceps json that do NOT match
        return this;
    }

    public String getNest(){return nest;}
    public FileRule setNest(String nest){
        this.nest = nest;
        return this;
    }

    public List<Filter> getFilters(){return filters;}
    public MatchCriteria getCriteria(){return criteria;}
    public Function<String,Json> getConverter(){return converter;}
    public FileRule setConverter(Function<String,Json> converter){
        this.converter = converter;
        return this;
    }

    public boolean apply(String path, BiConsumer<String,Json> callback){
        try {
            Json state = with.clone();
            boolean matched = getCriteria().match(path, state);

            if (matched) {
                try {
                    Json result = getConverter().apply(path);
                    String nestPath = StringUtil.populatePattern(getNest(), Json.toObjectMap(state));

                    if (getFilters().isEmpty()) {
                        callback.accept(nestPath, result);
                    } else {
                        final Json postFilter = new Json();
                        BiConsumer<String, Object> filterCallback = (nest, json) -> {
                            Json.chainSet(postFilter, nest, json);
                        };
                        for (Filter filter : getFilters()) {
                            filter.apply(result, filterCallback);
                        }
                        callback.accept(nestPath, postFilter);
                    }
                }catch(JsException jse){
                    logger.error("Javacript exception processing "+path+"\n"+jse,jse);
                }
            }
            return matched;
        }catch(Exception e){
            logger.error("Failed to convert "+path,e);
        }
        return false;
    }
}
