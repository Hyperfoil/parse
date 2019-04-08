package perf.parse.internal;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Set;

/**
 *
 */
public class StringMatcher implements IMatcher {


    private static class Matchable {
        public String prefix = null;
        public String name = null;
        public String expected = null;
        public String suffix = null;
        public boolean lastSuffix = false;
        public boolean forceStart = false;
        public boolean expectString = false;
        public boolean forceEnd = false;
    }

    private LinkedList<Matchable> matchers;

    private int regionStart;
    private int regionEnd;

    private int matchStart;
    private int matchEnd;

    private CharSequence input;

    private HashMap<String,String> matches;

    public StringMatcher(String pattern){
        this.matchers = parsePattern(pattern);
        matches = new HashMap<>();
    }

    @Override
    public void reset(CharSequence input) {
        this.input = input;
        regionStart = 0;
        regionEnd = input.length();
        matchStart = 0;
        matchEnd = -1;
    }

    @Override
    public boolean find() {
        int startIdx = matchEnd+1;
        matches.clear();
        String str = input.toString();
        matchEnd = regionStart;
        matchStart = regionEnd;
        for( Matchable m : this.matchers ){
            int prefixIdx = str.indexOf(m.prefix, startIdx);
            if(prefixIdx < 0 || (m.forceStart && prefixIdx > 0) ){
                return false;
            }

            int suffixIdx = prefixIdx+m.prefix.length();
            if( m.lastSuffix ){
                suffixIdx = str.lastIndexOf(m.suffix,regionEnd);
            } else {
                suffixIdx = str.indexOf(m.suffix,suffixIdx);
            }
            if(suffixIdx < 0 || ( suffixIdx + m.suffix.length() ) > regionEnd ){
                return false;
            }
            if(m.forceEnd && ( suffixIdx + m.suffix.length() ) != regionEnd){
                return false;
            }

            if( m.expectString ) {
                int expectIdx = str.indexOf(m.expected,prefixIdx+m.prefix.length());
                if(expectIdx < 0 || expectIdx + m.expected.length() != suffixIdx){
                    return false;
                }
            }

            matches.put(m.name,str.substring(prefixIdx+m.prefix.length(),suffixIdx));

            if( prefixIdx < matchStart ) {
                matchStart = prefixIdx;
            }
            if(suffixIdx + m.suffix.length() > matchEnd){
                matchEnd = suffixIdx + m.suffix.length();
            }
            startIdx = suffixIdx;
        }
        if(matches.size() < matchers.size()) {
            return false;
        }
        return true;
    }

    @Override
    public boolean find(CharSequence input, int start, int end) {
        return false;
    }

    @Override
    public void region(int start, int end) {
        regionStart = start;
        regionEnd = end;
        matchStart = regionStart;
        matchEnd = regionStart-1;
    }

    @Override
    public int start() {
        return matchStart;
    }

    @Override
    public int end() {
        return matchEnd;
    }

    public Set<String> groups(){return matches.keySet();}

    @Override
    public String group(String name) {
        return matches.get(name);
    }



















    public void print(){
        System.out.println("StringMatcher: ");
        System.out.println("input="+input);
        for(Matchable m : matchers){
            System.out.println("  Matchable="+m.prefix+"("+m.name+":"+m.expected+")"+m.suffix+" start="+m.forceStart+" end="+m.forceEnd+" last"+m.lastSuffix+" expect="+m.expectString);
        }
        System.out.println("region=("+regionStart+", "+regionEnd+") match=("+matchStart+", "+matchEnd);
        for(String key : matches.keySet()){
            System.out.println("  Match <"+key+"> = "+matches.get(key));
        }

    }



    public static boolean canMatch(String pattern){
        return parsePattern(pattern)!=null; // wasteful use of the parsing but nothing compared to the waste of using Regex
    }
    private static LinkedList<Matchable> parsePattern(String pattern){
        LinkedList<Matchable> rtrn = new LinkedList<>();
        int idx=0;
        int startIdx = 0;
        int closeCarretIdx = 0;
        int closePatternIdx = 0;
        Matchable prev = null;
        while(idx<pattern.length() && ( startIdx=pattern.indexOf("(?<",idx)) > -1){
            closeCarretIdx = pattern.indexOf(">",startIdx);
            closePatternIdx=closeCarretIdx;
            while( ( closePatternIdx=pattern.indexOf(")",closePatternIdx+1)) > -1 && pattern.charAt(closePatternIdx-1)== '\\' ) {
                //do nothing
            }
            Matchable next = new Matchable();
            if (startIdx <= idx ) {
                return null; // cannot use this form of matcher if each pattern does not have a prefix
            }
            next.prefix = pattern.substring(idx,startIdx);
            if(next.prefix.startsWith("^")){
                next.forceStart=true;
                next.prefix = next.prefix.substring(1);
            }
            if(prev!=null){
                prev.suffix=next.prefix;
                if(prev.suffix.endsWith("$")){
                    prev.forceEnd = true;
                    prev.suffix=prev.suffix.substring(0,prev.suffix.length()-1);
                }
            }
            if(closeCarretIdx <= startIdx+3){
                return null; //each pattern must have a Name
            }
            next.name = pattern.substring(startIdx+3,closeCarretIdx);
            String p = pattern.substring(closeCarretIdx + 1, closePatternIdx);
            if(p == ".*" || p == ".+") {
                next.lastSuffix=true;
            }
            if(!p.matches(".*(?<=\\\\)[dDsSwW].*") && !p.matches(".*(?<!\\\\)[\\.\\*].*")){
                next.expected = p;
                next.expectString=true;
            }
            rtrn.add(next);
            prev = next;
            idx=closePatternIdx+1;
        }
        if(closePatternIdx+1 < pattern.length()){
            prev.suffix = pattern.substring(closePatternIdx+1);
            if(prev.suffix.endsWith("$")){
                prev.forceEnd = true;
                prev.suffix=prev.suffix.substring(0,prev.suffix.length()-1);
            }
        }
        LinkedList<String> l = new LinkedList<>();
        l.add(pattern.substring(closePatternIdx+1));

        return rtrn;
    }

    public static String pad(int i){
        if(i<=0)
            return "";
        return "                                                                                                                                                                                                              ".substring(0,i);
    }


    public static void main(String[] args) {
        String pattern = "^(?<timestamp>.{10}T.{17}): ";

        System.out.println(canMatch(pattern));

        String line = "OpenJDK 64-Bit Server VM (25.0-b70) for linux-amd64 JRE (1.8.0-internal-benchuser_2015_01_29_15_41-b00), built on Jan 29 2015 15:43:22 by \"benchuser\" with gcc 4.8.2 20140120 (Red Hat 4.8.2-16)\n";
        StringMatcher m = new StringMatcher("^\\[(?<gctype>GC) ");
        m.reset(line);
        boolean found = m.find();
        System.out.println("found = "+found);
        m.print();
    }
}
