package io.hyperfoil.tools.parse.file;

import io.hyperfoil.tools.yaup.StringUtil;
import io.hyperfoil.tools.yaup.file.FileUtility;
import io.hyperfoil.tools.yaup.json.Json;
import org.jboss.logging.Logger;

import java.lang.invoke.MethodHandles;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class MatchCriteria {

    final static Logger logger = Logger.getLogger(MethodHandles.lookup().lookupClass());

    private String pathPattern ="";
    private int headerLines = 0;
    private final LinkedHashSet<String> findPatterns = new LinkedHashSet<>();
    private final LinkedHashSet<String> notFindPatterns = new LinkedHashSet<>();

    @Override
    public String toString(){
        return pathPattern+" lines="+headerLines+" find="+findPatterns+" avoid="+notFindPatterns;
    }

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


    public record MatchResult(boolean matched,Json extracted){}
    /**
     * returns true if any pathPattern matches, any findPattern matches, and no notFindPatterns match.
     * @param input
     * @return
     */
    public MatchResult match(String input){
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
        if(rtrn && logger.isDebugEnabled()){
            if(toMerge.isEmpty()) {
                logger.debug("matched " + input);
            }else{
                logger.debugf("matched " + input+" and captured "+toMerge);
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
            if(!rtrn && logger.isDebugEnabled()){
                logger.debug("did not match a header pattern");
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
            if(!rtrn && logger.isDebugEnabled()){
                logger.debug("matched a negative header pattern");
            }
        }
        return new MatchResult(rtrn,toMerge);
    }
}
