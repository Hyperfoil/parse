package perf.parse;

public enum ExpMerge {
   /**
    * Each key is merged separately into the current json object. This uses the ValueMerge to determine
    * how multiple matches are merged togehter.
    */
   ByKey,
   /**
    * creates a new Json object from all the values in the current ExpOld and merges them together into
    * an array under the ExpOld nesting or just the first array in the current json's parent path.
    */
   AsEntry,
   /**
    * Merges all the Exps values into the last instance of the ExpOld grouping or the last value from
    * the closest array in the current Json objects parent structure.
    */
   Extend;
}
