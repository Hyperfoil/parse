package io.hyperfoil.tools.parse;

/**
 *
 */
public enum ExpMerge {



   /**
    * Each key is merged separately into the current json object. This uses the ValueMerge to determine
    * how multiple matches are merged together. This is the default merge for an Exp.
    */
   ByKey,
   /**
    * Creates a new Json object from all the values in the current Exp and merges them together into
    * an array under the Exp's nest or the parent array of the current current json target.
    */
   AsEntry,
   /**
    * Merges all the Exps values into the last instance of the Exp grouping or the last value from
    * the closest array in the current Json objects parent structure.
    */
   Extend;
}
