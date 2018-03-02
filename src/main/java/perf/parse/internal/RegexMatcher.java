package perf.parse.internal;

import perf.yaup.StringUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 */
public class RegexMatcher implements IMatcher {

    private static final String FIELD_PATTERN = "\\(\\?<([^>]+)>";

    private Matcher matcher;
    private final Matcher fieldMatcher = java.util.regex.Pattern.compile(FIELD_PATTERN).matcher("");
    private LinkedHashMap<String,String> renames;


    public static List<String> getAllNames(String pattern){
        List<String> rtrn = new ArrayList<>();
        Matcher matcher = Pattern.compile(FIELD_PATTERN).matcher(pattern);
        while(matcher.find()){
            String match = matcher.group(1);
            rtrn.add(match);
        }
        return rtrn;
    }

    public RegexMatcher(String pattern){
        String newPattern = pattern;
        renames = new LinkedHashMap<>();

        fieldMatcher.reset(pattern);
        while(fieldMatcher.find()){
            String realName = fieldMatcher.group(1);
            String compName = realName.replaceAll("[\\.\\\\_]","x");
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
