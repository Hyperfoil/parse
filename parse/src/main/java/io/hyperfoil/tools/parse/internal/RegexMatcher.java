package io.hyperfoil.tools.parse.internal;

import io.hyperfoil.tools.parse.Exp;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 */
public class RegexMatcher implements IMatcher {

    private static final String FIELD_PATTERN = Exp.CAPTURE_GROUP_PATTERN;
    private static final String SAFE_PREFIX = "x";


    private Matcher matcher;
    ThreadLocal<Matcher> cachedMatcher;
    private final Matcher fieldMatcher = java.util.regex.Pattern.compile(FIELD_PATTERN).matcher("");
    private LinkedHashMap<String,String> renames;
    private String pattern;
    private String safePattern;
    private int regionStart=-1;
    private int regionStop=-1;

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
        int captureCounter=0;
        fieldMatcher.reset(newPattern);
        while(fieldMatcher.find()){
            int start = fieldMatcher.start(1);
            int end = fieldMatcher.end(1);
            String realName = fieldMatcher.group(1);
            String compName = realName.replaceAll("[^a-zA-Z0-9]",SAFE_PREFIX);
            if(!Character.isLetter(compName.charAt(0))){
                compName = SAFE_PREFIX + compName;
            }
            if(!compName.equals(realName)){
                //append the counter to avoid creating matching new names
                compName=compName+String.format("%03d",captureCounter++);
                newPattern = newPattern.substring(0,start)+compName+newPattern.substring(end);
                fieldMatcher.reset(newPattern);
            }else{

            }
            renames.put(realName,compName);
        }
        this.pattern = pattern;
        safePattern = newPattern;
        cachedMatcher = new ThreadLocal<>();
        cachedMatcher.set(Pattern.compile(newPattern).matcher(""));
        matcher = Pattern.compile(newPattern).matcher("");
    }


    public void reset(CharSequence input){
        if(cachedMatcher.get()==null){
            cachedMatcher.set(Pattern.compile(safePattern).matcher(""));
        }
        cachedMatcher.get().reset(input);
        matcher.reset(input);
    }
    public boolean find(){

        if(cachedMatcher.get()!=null){
            return cachedMatcher.get().find();
        }else {
            return matcher.find();
        }
    }

    @Override
    public boolean find(CharSequence input, int start, int end) {
        reset(input);
        region(start,end);
        return find();
    }

    public void region(int start,int end){
        this.regionStart=start;
        this.regionStop=end;
        if(cachedMatcher.get()!=null) {
            cachedMatcher.get().region(start, end);
        }else {
            matcher.region(start, end);
        }
    }
    public int start(){
        if(cachedMatcher.get()!=null) {
            return cachedMatcher.get().start();
        }else {
            return matcher.start();
        }
    }
    public int end(){
        if(cachedMatcher.get()!=null) {
            return cachedMatcher.get().end();
        }else {
            return matcher.end();
        }
    }
    public Set<String> groups(){
        return renames.keySet();
    }
    public String group(String name){
        String newName = renames.containsKey(name) ? renames.get(name) : name;
        if(cachedMatcher.get()!=null) {
            return cachedMatcher.get().group( newName );
        }else {
            return matcher.group(newName);
        }
    }
}
