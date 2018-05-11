package perf.parse.internal;

/**
 * A mutable wrapper for String that allows us to repeatedly match a pattern and remove the match substring
 * Created by wreicher
 */
public class CheatChars implements CharSequence {

    private String originalLine;
    private String line;

    public CheatChars(String line){
        this.originalLine = line;
        this.line = line;
    }

    public void drop(int start, int end){
        if(start>end || start < 0 || end > line.length()){
            throw new IllegalArgumentException("Invalid drop range. [length="+line.length()+" start="+start+" end="+end+"] line="+this.toString());
        }
        if( start==0 && end == line.length()){ //shortcut for common case of dropping the entire line
            line = "";
        } else {
            line = line.substring(0, start) + line.substring(end);
        }
    }

    public boolean isEmpty(){ return line.isEmpty();}

    @Override
    public int length() { return line.length(); }
    @Override
    public char charAt(int index) {
        try {
            return line.charAt(index);
        }catch(StringIndexOutOfBoundsException e){
            System.out.println(index+" > "+line.length()+" line=||"+line+"||");
            throw e;
        }
    }
    @Override
    public CheatChars subSequence(int start, int end) {
        return new CheatChars(line.subSequence(start,end).toString());
    }
    @Override
    public String toString(){ return line; }
    public String getOriginalLine(){return originalLine;}
    @Override
    public boolean equals(Object obj){ return originalLine.equals(obj); }
    @Override
    public int hashCode(){ return originalLine.hashCode(); }

}

