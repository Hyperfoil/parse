package io.hyperfoil.tools.parse;

import io.hyperfoil.tools.parse.internal.DropString;
import io.hyperfoil.tools.yaup.StringUtil;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by wreicher
 */
public enum Eat {
    /**
     * Eat a fixed with of the input each time the io.hyperfoil.tools.parse.Exp matches
     */
    Width(1),
    /**
     * Do not consume any of the input line (Default behavior)
     */
    None(0),
    /**
     * Consume the matched part of the line (including non-captured sections)
     */
    Match(-1),
    /**
     * Eat the line up to and including the match
     */
    ToMatch(-2),
    /**
     * If the io.hyperfoil.tools.parse.Exp matches any part of the line then consume the entire line
     * preventing other io.hyperfoil.tools.parse.Exp from matching
     */
    Line(-3);


    private static final Map<Integer,Eat> idMap = new HashMap<>();
    static {
        Eat values[] = Eat.values();
        for(int i=0; i<values.length; i++){
            idMap.put(values[i].getId(),values[i]);
        }
    }

    private int id;

    Eat(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public static Eat from(int value) {
        return idMap.containsKey(value) ? idMap.get(value) : Width;
    }
    public static Eat from(String input){
        if(input.matches("-?\\d+")){
            return Eat.from(Integer.parseInt(input));
        }else{
            return StringUtil.getEnum(input,Eat.class,Eat.Match);
        }
    }


    public static boolean preEat(int eat, DropString line, int start, int end){
        Eat toEat = Eat.from(eat);
        boolean changed = false;
        switch (toEat){
            case Match:
                line.drop(start,end);
                changed = true;
                break;
            case ToMatch:
                line.drop(0,end);
                changed = true;
                break;
            case Width:
                line.drop(start,start+eat);
                changed = true;
                break;
        }
        return changed;
    }
    public static boolean postEat(int eat,DropString line,int start,int end){
        Eat toEat = Eat.from(eat);
        boolean changed = false;
        switch (toEat){
            case Line:
                line.drop(0,line.length());
                changed = true;
        }
        return changed;
    }
}
