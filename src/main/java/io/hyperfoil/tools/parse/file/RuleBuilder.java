package io.hyperfoil.tools.parse.file;

import io.hyperfoil.tools.parse.Exp;
import io.hyperfoil.tools.parse.Parser;
import io.hyperfoil.tools.yaup.json.Json;

import java.util.function.Function;
import java.util.function.Supplier;

public class RuleBuilder {

    private final String name;
    private final FileRule rule;
    public RuleBuilder(String name){
        this.name = name;
        this.rule = new FileRule(name);
    }

    public RuleBuilder path(String path){
        rule.getCriteria().setPathPattern(path);
        return this;
    }
    public RuleBuilder header(int size){
        rule.getCriteria().setHeaderLines(size);
        return this;
    }
    public RuleBuilder findHeader(String line){
        rule.getCriteria().addFindPattern(line);
        return this;
    }
    public RuleBuilder avoidHeader(String line){
        rule.getCriteria().addNotFindPattern(line);
        return this;
    }
    public RuleBuilder nest(String nest){
        rule.setNest(nest);
        return this;
    }
    public FileRule asPath(Function<String, Json> converter){
        rule.setConverter(converter);
        return rule;
    }
    public FileRule asText(Supplier<Parser> supplier){
        rule.setConverter(new TextConverter().addFactory(supplier));
        return rule;
    }
    public FileRule asText(Exp...exp){
        TextConverter converter = new TextConverter();
        for(int i=0; i<exp.length; i++){
            converter.addExp(exp[i]);
        }
        rule.setConverter(converter);
        return rule;
    }
    public FileRule asJbossCli(){
        rule.setConverter(new JbossCliConverter());
        return rule;
    }
    public FileRule asJbossCli(Function<Json,Json> then){
        rule.setConverter(new JbossCliConverter().andThen(then));
        return rule;
    }
    public FileRule asJson(){
        rule.setConverter(new JsonConverter());
        return rule;
    }
    public FileRule asJson(Function<Json,Json> then){
        rule.setConverter(new JsonConverter().andThen(then));
        return rule;
    }
    public FileRule asXml(){
        rule.setConverter(new XmlConverter());
        return rule;
    }
    public FileRule asXml(Filter...filters){
        XmlConverter converter = new XmlConverter();
        for(int i=0; i<filters.length;i++){
            converter.addFilter(filters[i]);
        }
        rule.setConverter(converter);
        return rule;
    }
}
