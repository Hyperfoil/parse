package perf.parse.internal;

import java.util.LinkedHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 */
public class RegexMatcher implements IMatcher {

    private Matcher matcher;
    private final Matcher fieldMatcher = java.util.regex.Pattern.compile("\\(\\?<([^>]+)>").matcher("");
    private LinkedHashMap<String,String> renames;

    public RegexMatcher(String pattern){
        String newPattern = pattern;
        renames = new LinkedHashMap<>();
        fieldMatcher.reset(pattern);
        while(fieldMatcher.find()){
            String realName = fieldMatcher.group(1);
            String compName = realName.replaceAll("\\.","xx");
            if(!compName.equals(realName)){
                newPattern = newPattern.replace(realName,compName);
            }
            renames.put(realName,compName);
        }

        matcher = Pattern.compile(newPattern).matcher("");
    }


    public void reset(CharSequence input){
        matcher.reset(input);
    }
    public boolean find(){
        return matcher.find();
    }
    public void region(int start,int end){
        matcher.region(start, end);
    }
    public int start(){
        return matcher.start();
    }
    public int end(){
        return matcher.end();
    }
    public String group(String name){
        String newName = renames.containsKey(name) ? renames.get(name) : name;
        return matcher.group( newName );
    }
}
