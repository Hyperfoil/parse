package io.hyperfoil.tools.parse;

import io.hyperfoil.tools.parse.internal.IMatcher;

public enum MatchRange {
   /**
    * Apply the Exp to the entire available input, ignoring where the previous Exp matched
    */
   EntireLine,
   /**
    * Apply the Exp starting form where the previous Exp ended.
    * This is the default behaviour
    */
   AfterParent,
   /**
    * Apply the Exp to the input section that is before the previous Exp match.
    * This can be thought of as a look behind from the parent or previous Exp.
    */
   BeforeParent;


   public int apply(IMatcher matcher,CharSequence line,int startIndex){
      int rtrn = startIndex;
      matcher.reset(line);
      switch (this){
         case EntireLine:
            matcher.region(0,line.length());
            rtrn = 0;
            break;
         case BeforeParent:
            matcher.region(0,startIndex);
            rtrn = 0;
            break;
         case AfterParent:
         default:
            matcher.region(startIndex,line.length());
      }
      return rtrn;
   }
}
