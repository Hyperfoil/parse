package perf.parse;

/**
 *
 */
public enum Value {
    /**
     * Uses the length of the value to determine if it should be a child, sibling, or ancestor of the current context
     */
    NestLength("_nestLength_"),
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
    BooleanKey("_booleanKey_"),
    /**
     * Use the value as a key with a value of true if the Exp parameter matches
     */
    BooleanValue("_booleanValue_"),
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

    private String id;
    private Value(String id){this.id = id;};
    public String getId(){return id;}

    public static Value from(String value){
        switch(value){
            case "_nestLength_": return NestLength;
            case "_number_": return Number;
            case "_kmg_": return KMG;
            case "_count_": return Count;
            case "_sum_": return Sum;
            case "_booleanKey_": return BooleanKey;
            case "_booleanValue_": return BooleanValue;
            case "_position_": return Position;
            case "_string_" : return String;
            case "_list_" : return List;
            case "_key_":
            default:
                return Key;
        }
    }
}
