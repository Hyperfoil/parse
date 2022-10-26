package io.hyperfoil.tools.parse.file;

import io.hyperfoil.tools.yaup.json.Json;
import io.hyperfoil.tools.yaup.xml.pojo.Xml;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Reads the content of the file as xml and converst the entire document to json
 */
public class XmlConverter implements Function<String, Json> {

    private final List<Filter> filters = new ArrayList<>();

    public boolean hasFilters(){return !filters.isEmpty();}
    public XmlConverter addFilter(Filter filter){
        filters.add(filter);
        return this;
    }

    @Override
    public Json apply(String s) {
        Xml xml = Xml.parseFile(s);
        if(filters.isEmpty()){
            return xml.toJson();
        }else{
            final Json toRtrn = new Json();
            BiConsumer<String,Object> filterCallback = (nest,obj)->{
                Json.chainSet(toRtrn,nest,obj);
            };
            for(Filter filter : filters){
              filter.apply(xml,filterCallback);
            }
            return toRtrn;
        }
    }



}
