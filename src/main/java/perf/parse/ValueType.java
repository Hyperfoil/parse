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
   Auto {
      @Override
      public Object apply(String a) {
         if (IntegerPattern.matcher(a).matches()) {
            return Long.parseLong(a);
         } else if (DoublePattern.matcher(a).matches()) {
            return Double.parseDouble(a);
         } else if ((a.startsWith("[") && a.endsWith("]")) || (a.startsWith("{") && a.endsWith("}"))) {
            return perf.yaup.json.Json.fromString(a);
         } else if (KmgPattern.matcher(a).matches()) {
            return StringUtil.parseKMG(a);
         } else {
            return a;
         }
      }
   },
   /**
    * Leave the value as a String even if the value would normally be converted to another type.
    */
   String {
      @Override
      public Object apply(String input) {
         return input;
      }
   },
   /**
    * Convert the input value into the number of bytes (Long).
    * Supports the usual K, Kb, or KB type suffixes. Assumes no suffix is already bytes
    */
   KMG {
      @Override
      public Object apply(String input) {
         return StringUtil.parseKMG(input);
      }
   },
   /**
    * Convert the value to a Long
    */
   Integer {
      @Override
      public Object apply(String input) {
         if (Pattern.matches("-?\\d+(?:.0+)?", input)) {
            return Long.parseLong(input);
         } else {
            return input;
         }
      }
   },
   /**
    * Convert the value to a Double
    */
   Decimal {
      @Override
      public Object apply(String input) {
         if (Pattern.matches("-?\\d+(?:.\\d+)?", input)) {
            return Double.parseDouble(input);
         } else {
            return input;
         }
      }
   },

   /**
    * Convert the value to a Json object. This will allow it to merge differently if it is an array type
    */
   Json {
      @Override
      public Object apply(String input) {
         return perf.yaup.json.Json.fromString(input);
      }
   };

   private static Pattern IntegerPattern = Pattern.compile("-?\\d{1,16}+");
   private static Pattern DoublePattern = Pattern.compile("-?\\d+(?:\\.\\d+)?");
   private static Pattern KmgPattern = Pattern.compile("\\d+[bBkKmMgG]");

   //private final Function<String,Object> transform;
   ValueType() {
   }

   public Object apply(String input) {
      return input;
   }
}
