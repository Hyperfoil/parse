package io.hyperfoil.tools.parse.internal;

/**
 * A mutable wrapper for String that allows us to repeatedly match a pattern and remove the match substring
 * Created by wreicher
 */
public class CheatChars extends DropString implements CharSequence {

    private String currentLine;

    public CheatChars(String line){
        super(line);
        this.currentLine = line;
    }

    @Override
    public void drop(int start, int end){
        if(start>end || start < 0 || end > currentLine.length()){
            throw new IllegalArgumentException("Invalid drop range. [length="+ currentLine.length()+" start="+start+" end="+end+"] currentLine="+this.toString());
        }
        if( start==0 && end == currentLine.length()){ //shortcut for common case of dropping the entire currentLine
            currentLine = "";
        } else {
            currentLine = currentLine.substring(0, start) + currentLine.substring(end);
        }
        updateReferences(start,end);
    }

    public boolean isEmpty(){ return currentLine.isEmpty();}

    @Override
    public int length() { return currentLine.length(); }
    @Override
    public char charAt(int index) {
        try {
            return currentLine.charAt(index);
        }catch(StringIndexOutOfBoundsException e){
            throw e;
        }
    }
    @Override
    public CheatChars subSequence(int start, int end) {
        return new CheatChars(currentLine.subSequence(start,end).toString());
    }
    @Override
    public String toString(){ return currentLine; }
    @Override
    public boolean equals(Object obj){ return line.equals(obj); }
    @Override
    public int hashCode(){ return line.hashCode(); }

}

