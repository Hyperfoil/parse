package io.hyperfoil.tools.parse.factory;

import io.hyperfoil.tools.parse.*;
import io.hyperfoil.tools.yaup.AsciiArt;
import io.hyperfoil.tools.yaup.StringUtil;
import io.hyperfoil.tools.yaup.json.Json;

public class JenkinsPipelineFactory {

    private String getQuotePattern(String name,int group){
        return "((?<![\\\\])['\"])(?<"+name+">(?:.(?!(?<![\\\\])\\"+group+"))*.?)\\"+group;
    }

    public Parser newParser(){
        Parser p = new Parser();

        p.add(new Exp("comment","//s*\\.*$"));//removes comments from end of line
        p.add(new Exp("trailing-spaces","\\s*$"));//removes spaces from end of line
        p.add(
                new Exp("","^\\s*(?<value>.*)")
                .requires("script")
                .execute((line, match, pattern, parser) -> {
                    int openCount = StringUtil.countOccurances(line,"{");
                    int closeCount = StringUtil.countOccurances(line,"}");
                    parser.addCount("script",openCount-closeCount);
                    if(parser.getCount("script") == 0){
                        parser.setState("script",false);
                    }else{
                        parser.setState("script",true);
                    }
                })
        );
        p.add(
            new Exp("step-key-value","^\\s*,?\\s*(?<key:key=value>[^:\\s]+)\\s*:\\s*(?<value>[^'\",$\\s]+)")
                .add(new Exp("trailing-comma","\\s*,?"))
            .requires("step")
        );

        p.add(
            new Exp("step-key-quoted_value","^\\s*,?\\s*(?<key:key=value>[^:\\s]+)\\s*:\\s*"+getQuotePattern("value",2))
                .add(new Exp("trailing-comma","\\s*,?"))
                .add(new Exp("plus-inline","^\\s*\\+\\s*"+getQuotePattern("value",1)).addRule(ExpRule.Repeat))
                .add(new Exp("plus-more","\\s*\\+\\s*$").enables("plus-more"))
            .requires("step")
        );
        p.add(
            new Exp("step-close-paren","^\\s*\\)")
                .disables("step")
                .addRule(ExpRule.PostPopTarget)
        );
        p.add(new Exp("plus-more-string","^\\s*\\+?\\s*"+getQuotePattern("value:add",1))
            .requires("plus-more")
            .disables("plus-more")
            .add(new Exp("plus-more","\\s*\\+\\s*$").enables("plus-more"))
        );
        p.add(new Exp("timeout-name","^\\s*timeout\\(\\s*(?<amount>[^'\"\\)]+)\\s*\\)")
            .nest("timeout")
            .setMerge(ExpMerge.AsEntry)
        );
        p.add(new Exp("close-curley","^\\s*}").addRule(ExpRule.PostPopTarget));
        p.add(new Exp("node-quote-name-nest","^\\s*(?<key>\\w+)\\s*\\(\\s*"+getQuotePattern("name",2)+"\\s*\\)\\s*\\{")
            .nest("${{key}}")
            .setMerge(ExpMerge.AsEntry)
            .addRule(ExpRule.PushTarget)
        );
        p.add(new Exp("node-quote-name","^\\s*(?<key>\\w+)\\s*\\(\\s*"+getQuotePattern("name",2)+"\\s*\\)")
                .nest("${{key}}")
                .setMerge(ExpMerge.AsEntry)
        );
        p.add(new Exp("node-name-nest","^\\s*(?<key>\\w+)\\s*\\(\\s*(?<name>[^'\"\\)]+)\\s*\\)\\s*\\{")
                .nest("${{key}}")
                .setMerge(ExpMerge.AsEntry)
                .addRule(ExpRule.PushTarget)
        );
        p.add(new Exp("node-name","^\\s*(?<key>\\w+)\\s*\\(\\s*(?<name>[^'\"\\)]+)\\s*\\)")
                .nest("${{key}}")
                .setMerge(ExpMerge.AsEntry)
        );
        p.add(new Exp("node-nest","^\\s*(?<key>\\w+)\\s*\\{")
                .nest("${{key}}")
                .setMerge(ExpMerge.AsEntry)
                .addRule(ExpRule.PushTarget)
        );
        p.add(new Exp("script","^\\s*script\\s*\\{\\s*")
            .execute((line, match, pattern, parser) -> {
                int openCount = StringUtil.countOccurances(line,"{");
                int closeCount = StringUtil.countOccurances(line,"}");
                parser.addCount("script",openCount-closeCount);
                if(openCount > closeCount){
                    parser.setState("script",true);
                    parser.setCount("script",openCount-closeCount-1);
                }else{
                    parser.setCount("script",0);
                    parser.setState("script",false);
                }
            })
            .with("section","script")
            .add(
                new Exp("content","^\\s*(?<value:list>.+?)\\}?$|//")
            )
        );

        p.add(
                new Exp("step","^\\s*(?<step>\\S+)\\s*(?![\\(\\{])")
                    .execute((line, match, pattern, parser) -> {
                    })
                    .add(
                            new Exp("step-key-value","^\\s*(?<key:key=value>[^:\\s]+)\\s*:\\s*(?<value>[^'\",$\\s]+)")
                            .add(new Exp("trailing-comma","\\s*,?"))
                    )
                    .add(
                            new Exp("step-key-quoted_value","^\\s*(?<key:key=value>[^:\\s]+)\\s*:\\s*"+getQuotePattern("value",2))
                                .add(new Exp("trailing-comma","\\s*,?"))
                                .add(new Exp("plus-inline","^\\s*\\+\\s*"+getQuotePattern("value",1)).addRule(ExpRule.Repeat))
                                .add(new Exp("plus-more","\\s*\\+\\s*$").enables("plus-more"))
                    )
                    .add(
                            new Exp("step-quoted-arg","^\\s*"+getQuotePattern("value",1))
                                .add(new Exp("plus-inline","^\\s*\\+\\s*"+getQuotePattern("value:add",1)).addRule(ExpRule.Repeat))
                                .add(new Exp("plus-more","\\s*\\+\\s*$").enables("plus-more"))
                    )
                    .add(
                            new Exp("step-close-paren","^\\s*\\)")
                            .disables("step")
                    )
                    .addRule(ExpRule.RepeatChildren)
                    .enables("step")
        );
        return p;
    }
}
