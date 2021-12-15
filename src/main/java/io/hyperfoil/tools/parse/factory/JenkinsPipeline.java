package io.hyperfoil.tools.parse.factory;

import io.hyperfoil.tools.parse.Exp;
import io.hyperfoil.tools.parse.ExpRule;
import io.hyperfoil.tools.parse.Parser;

public class JenkinsDeclarativePipeline {
    public Parser newParser() {
        Parser p = new Parser();
        addToParser(p);
        return p;
    }
    public void addToParser(Parser p) {
        addToParser(p,false);
    }
    public void addToParser(Parser p, boolean strict){
        p.add(pipeline().enables("pipeline"));
        p.add(new Exp("{"));
        p.add(new Exp("}").addRule(ExpRule.PostPopTarget));
        p.add(namedKey().requires("pipeline"));
        p.add(keyValue().requires("pipeline"));
        p.add(key().requires("pipeline"));

    }

    public Exp keyValue(){
        return new Exp("keyValue", "(?<key:key=value>\\w+)\\s*:\\s*(?<value>[\"']*\\w*[\"']*)");
    }
    public Exp pipeline(){
        return new Exp("pipeline","^\\s*pipeline");
    }
    public Exp key(){
        return new Exp("key","^\\s*(?<name:ignore>)").key("name").addRule(ExpRule.PushTarget);
    }
    public Exp namedKey(){
        return new Exp("namedKey","\\s+(?<key:ignore>\\w+)\\s*\\(\\s*[\"'](?<name>[^\"']+)[\"']");
    }
}
