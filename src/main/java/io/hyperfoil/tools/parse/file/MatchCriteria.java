package io.hyperfoil.tools.parse.file;

import io.hyperfoil.tools.yaup.StringUtil;
import io.hyperfoil.tools.yaup.file.FileUtility;
import io.hyperfoil.tools.yaup.json.Json;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class MatchCriteria {

    private String pathPattern ="";
    private int headerLines = 0;
    private final LinkedHashSet<String> findPatterns = new LinkedHashSet<>();
    private final LinkedHashSet<String> notFindPatterns = new LinkedHashSet<>();

    public MatchCriteria(){}
    public MatchCriteria(String pathPattern) {
        this.pathPattern = pathPattern;
    }

    public boolean isSet(){return hasPathPattern() || hasFindPatterns() || hasNotFindPatterns();}
    public MatchCriteria setPathPattern(String pattern){
        pathPattern=pattern;
        return this;
    }
    public MatchCriteria addFindPattern(String pattern){
        findPatterns.add(pattern);
        return this;
    }
    public MatchCriteria addNotFindPattern(String pattern){
        notFindPatterns.add(pattern);
        return this;
    }
    public boolean hasPathPattern(){return !pathPattern.isEmpty();}
    public String getPathPattern(){
        return pathPattern;
    }
    public Iterator<String> getFindPatterns(){
        return findPatterns.iterator();
    }
    public Iterator<String> getNotFindPatterns(){
        return notFindPatterns.iterator();
    }
    public MatchCriteria setHeaderLines(int headerLines){
        this.headerLines = headerLines;
        return this;
    }
    public int getHeaderLines(){return headerLines;}
    public boolean hasFindPatterns(){return !findPatterns.isEmpty();}
    public boolean hasNotFindPatterns(){return !notFindPatterns.isEmpty();}
    public boolean usesHeader(){return getHeaderLines()>0 && (hasFindPatterns() || hasNotFindPatterns());}

    /**
     * returns true if any pathPattern matches, any findPattern matches, and no notFindPatterns match.
     * @param input
     * @param state
     * @return
     */
    public boolean match(String input, Json state){
        boolean rtrn = true;
        Matcher m;
        Json toMerge = new Json();
        if(hasPathPattern()){
            rtrn = false;//we have header patterns that need to match
            if( (rtrn = input.contains(pathPattern))){

            }else {
                try{
                    if ( (m = Pattern.compile(pathPattern).matcher(input)).find()){
                        rtrn = true;
                        List<String> keys = StringUtil.getCaptureNames(pathPattern);
                        for(String key : keys){
                            toMerge.set(key,m.group(key));
                        }
                    }
                }catch(PatternSyntaxException e){
                    rtrn = false;//rtrn should already be false
                }
            }
        }
        if(rtrn && usesHeader()){//if a pathPattern matches or there were none
            String headerLines = FileUtility.readHead(input,getHeaderLines());
            if(hasFindPatterns()) {
                rtrn = false;//we need a find pattern to match
                for (Iterator<String> iter = getFindPatterns(); iter.hasNext(); ) {
                    try {
                        String findPattern = iter.next();
                        if (headerLines.contains(findPattern)) {
                            rtrn = true;
                            break;
                        } else if ((m = Pattern.compile(findPattern).matcher(headerLines)).find()) {
                            rtrn = true;
                            List<String> keys = StringUtil.getCaptureNames(findPattern);
                            for (String key : keys) {
                                toMerge.set(key, m.group(key));
                            }
                            break;
                        }
                    }catch(PatternSyntaxException e){
                        rtrn = false;//ignore because the pattern may not mean to be regex
                    }
                }
            }
            if(rtrn && hasNotFindPatterns()){
                for(Iterator<String> iter = getNotFindPatterns(); iter.hasNext();){
                    String notFindPattern = iter.next();
                    try {
                        if (headerLines.contains(notFindPattern) || (m = Pattern.compile(notFindPattern).matcher(headerLines)).find()) {
                            rtrn = false;
                            break;
                        }
                    }catch(PatternSyntaxException e){
                        //don't change rtrn because an exception means it didn't match
                    }
                }
            }
        }
        if(rtrn && !toMerge.isEmpty()){
            state.merge(toMerge);
        }
        return rtrn;
    }
}
