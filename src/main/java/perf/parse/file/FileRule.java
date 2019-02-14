package perf.parse.file;

import perf.yaup.StringUtil;
import perf.yaup.json.Json;

import java.util.LinkedList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

public class FileRule {

    private String name="";
    private MatchCriteria criteria = new MatchCriteria();
    private final List<Filter> filters = new LinkedList<>();
    private String nest="";
    private Function<String, Json> converter =null;

    public FileRule(){}
    public FileRule(String name){
        this.name = name;
    }

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

        Json state = new Json();
        boolean matched = getCriteria().match(path,state);
        if(matched){

            Json result = getConverter().apply(path);
            String nestPath = StringUtil.populatePattern(getNest(),Json.toObjectMap(state));
            System.out.println(name+" matched "+path);
            if(getFilters().isEmpty()){
                callback.accept(nestPath,result);
            }else{
                final Json postFilter = new Json();
                BiConsumer<String,Object> filterCallback = (nest,json)->{
                    Json.chainSet(postFilter,nest,json);
                };
                for(Filter filter: getFilters()){
                    filter.apply(result,filterCallback);
                }
                callback.accept(nestPath,postFilter);
            }
        }
        return matched;
    }
}
