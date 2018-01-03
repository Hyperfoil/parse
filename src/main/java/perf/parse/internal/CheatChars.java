package perf.parse.internal;

/**
 * A mutable wrapper for String that allows us to repeatedly match a pattern and remove the match substring
 * Created by wreicher
 */
public class CheatChars implements CharSequence {

    private String line;

    public CheatChars(String line){
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
    public char charAt(int index) { return line.charAt(index); }
    @Override
    public CharSequence subSequence(int start, int end) {
        return line.subSequence(start,end);
    }
    @Override
    public String toString(){ return line; }
    @Override
    public boolean equals(Object obj){ return line.equals(obj); }
    @Override
    public int hashCode(){ return line.hashCode(); }

}

