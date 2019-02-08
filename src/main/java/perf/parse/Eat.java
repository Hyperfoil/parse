package perf.parse;

import perf.yaup.StringUtil;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by wreicher
 */
public enum Eat {
    /**
     * Eat a fixed with of the input each time the perf.parse.Exp matches
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
     * If the perf.parse.Exp matches any part of the line then consume the entire line
     * preventing other perf.parse.Exp from matching
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
        if(input.matches("\\d+")){
            return Eat.from(Integer.parseInt(input));
        }else{
            return StringUtil.getEnum(input,Eat.class,Eat.Match);
        }
    }
}
