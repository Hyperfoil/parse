package perf.parse;

import java.util.HashMap;
import java.util.Map;

/**
 *
 */
public enum Value {

    /**
     * If the value does not match the previous value then close the current rot context and start a new one
     */
    TargetId("_targetid_"),
    /**
     * Uses the length of the value to determine if it should be a child, sibling, or elder of the current target
     */
    NestLength("_nestlength_"),
    /**
     * Uses the length of the value to determien if it should be a child, sibling, or elder. Siblings must have the same
     * prefix string
     */
    NestPeerless("_nestpeerless_"),
    /**
     * Convert the value to a number rather than leaving it as a String
     */
    Number("_number_"),
    /**
     * Parse the suffix and convert the value into bytes
     * k = 1024 bytes
     * m = 1024^2 bytes
     * g = 1024^3 bytes
     * t = 1024^4 bytes
     * p = 1024^5 bytes
     * e = 1024^6 bytes
     * z = 1024^7 bytes
     * y = 1024^8 bytes
     */
    KMG("_kmg_"),
    /**
     * Count the number of occurrences of the value and store them with the value as key
     */
    Count("_count_"),
    /**
     * Count the number of times value is seen
     */
    Sum("_sum_"),
    /**
     * Treat the value as the key for the value of another named Exp parameter
     */
    Key("_key_"),//will value of key will be the key for another key's value
    /**
     * Store a true of for the value if the Exp parameter matches
     */
    BooleanKey("_booleankey_"),
    /**
     * Use the value as a key with a value of true if the Exp parameter matches
     */
    BooleanValue("_booleanvalue_"),
    /**
     * Use the position in the input string as the value (0 indexed)
     */
    Position("_position_"),
    /**
     * Treat the value as a String using concatenation for multiple occurrences
     */
    String("_string_"),
    /**
     * Treat the value as a list of all matched values
     */
    List("_list_");

    private static final Map<String,Value> idMap = new HashMap<>();
    static {
        Value values[] = Value.values();
        for(int i=0; i<values.length; i++){
            idMap.put(values[i].getId().toLowerCase(),values[i]);
        }
    }

    private String id;

    Value(String id){this.id = id;}

    public String getId(){return id;}

    public static Value from(String value){
        value = value.toLowerCase();
        if(!value.startsWith("_")){
            value = "_"+value;
        }
        if(!value.endsWith("_")){
            value = value+"_";
        }
        if(idMap.containsKey(value)){
            return idMap.get(value);
        }else{
            return Key;
        }
    }
}
