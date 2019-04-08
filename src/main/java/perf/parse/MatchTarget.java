package perf.parse;

import perf.parse.internal.IMatcher;

public enum MatchTarget {
   EntireLine,
   AfterParent,
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
