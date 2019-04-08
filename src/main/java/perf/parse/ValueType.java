package perf.parse;

import perf.yaup.StringUtil;

import java.util.function.Function;
import java.util.regex.Pattern;

public enum ValueType {
    /**
     * Default behaviour
     * Automatically transform the input unto Long, Double, Json, KMG, if the value matches the pattern.
     * Otherwise leave the value as a string
     */
    Auto(a->{
        if(Pattern.matches("-?\\d+",a)){
            return Long.parseLong(a);
        }else if(Pattern.matches("-?\\d+(?:\\.\\d+)?",a)){
            return Double.parseDouble(a);
        }else if ( (a.startsWith("[") && a.endsWith("]")) || (a.startsWith("{") && a.endsWith("}"))){
            return perf.yaup.json.Json.fromString(a);
        }else if ( Pattern.matches("\\d+[bBkKmMgG]",a)){
            return StringUtil.parseKMG(a);
        }else{
            return a;
        }
    }),
    /**
     * Leave the value as a String even if the value would normally be converted to another type.
     */
    String(a->a),
    /**
     * Convert the input value into the number of bytes (Long).
     * Supports the usual K, Kb, or KB type suffixes. Assumes no suffix is already bytes
     */
    KMG(a-> StringUtil.parseKMG(a)),
    /**
     * Convert the value to a Long
     */
    Integer(a->{
        if(Pattern.matches("-?\\d+(?:.0+)?",a)){
            return Long.parseLong(a);
        }else{
            return a;
        }
    }),
    /**
     * Convert the value to a Double
     */
    Decimal(a->{
        if(Pattern.matches("-?\\d+(?:.\\d+)?",a)){
            return Double.parseDouble(a);
        }else{
            return a;
        }
    }),
    /**
     * Convert the value to a Json object. This will allow it to merge differently if it is an array type
     */
    Json(a->perf.yaup.json.Json.fromString(a));

    private final Function<String,Object> transform;
    ValueType(Function<String,Object> transform){
        this.transform = transform;
    }

    public Object apply(String input){
        return transform.apply(input);
    }
}
