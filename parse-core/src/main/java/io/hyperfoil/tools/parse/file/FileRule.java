package io.hyperfoil.tools.parse.file;

import io.hyperfoil.tools.parse.JsStringFunction;
import io.hyperfoil.tools.parse.Parser;
import io.hyperfoil.tools.parse.Exp;
import io.hyperfoil.tools.parse.JsJsonFunction;
import io.hyperfoil.tools.yaup.PopulatePatternException;
import io.hyperfoil.tools.yaup.StringUtil;
import io.hyperfoil.tools.yaup.json.Json;
import io.hyperfoil.tools.yaup.json.graaljs.JsException;
import org.jboss.logging.Logger;

import java.lang.invoke.MethodHandles;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

public class FileRule {

    final static Logger logger = Logger.getLogger(MethodHandles.lookup().lookupClass());

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
                    logger.error("failed to create filter:\n"+jsonFilter);
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
                logger.error("findHeader needs to be a string or list\n"+findHeader);
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
                logger.error("avoidHeader needs to be a string or list\n"+avoidheader);
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
                        Exp exp = Exp.fromJson((Json)expData,state);
                        converter.addExp(exp);
                    }else{
                        logger.error("cannot create an expression from input:\n"+expData);
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
                logger.error("cannot create asJbossCli from input:\n"+asJbossCli);
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
            }else{
                logger.error("cannot create asJson from input:\n"+asJson);
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
            }else{
                logger.error("cannot create asXml from input:\n"+asXml);
            }
        }
        if(json.has("asPath")){
            rtrn.setConverter(FileRule.asPath(json.getString("asPath")));
        }
        return rtrn;
    }
    public static BiFunction<String,Json,Json> asPath(String function){
        return new JsStringFunction(function);
    }

    private String name;

    private final Json with;
    private MatchCriteria criteria = new MatchCriteria();
    private final List<Filter> filters = new LinkedList<>();
    private String nest="";
    private BiFunction<String, Json, Json> converter =null;

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
    public BiFunction<String,Json,Json> getConverter(){return converter;}
    public FileRule setConverter(BiFunction<String,Json,Json> converter){
        this.converter = converter;
        return this;
    }

    public record RuleResult(String nest,Json result,Json state){}

    public boolean apply(String path, Consumer<RuleResult> callback){
        try {
            Json state = with.clone();
            MatchCriteria.MatchResult matchResult = getCriteria().match(path);

            if (matchResult.matched()) {
                state.merge(matchResult.extracted());
                try {
                    Json result = getConverter().apply(path, state);
                    String nestPath = StringUtil.populatePattern(getNest(), Json.toObjectMap(state));

                    if (getFilters().isEmpty()) {
                        callback.accept(new RuleResult(nestPath, result,state));
                    } else {
                        final Json postFilter = new Json();
                        BiConsumer<String, Object> filterCallback = (nest, json) -> {
                            Json.chainSet(postFilter, nest, json);
                        };
                        for (Filter filter : getFilters()) {
                            filter.apply(result, filterCallback);
                        }
                        callback.accept(new RuleResult(nestPath, postFilter,state));
                    }
                }catch(JsException jse){
                    logger.error("Javascript exception processing "+path+"\n"+jse,jse);
                }
            }
            return matchResult.matched();
        }catch(Exception e){
            logger.error("Failed to convert "+path,e);
        }
        return false;
    }
}
