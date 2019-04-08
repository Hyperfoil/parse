package perf.parse;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import perf.parse.internal.CheatChars;
import perf.parse.internal.DropString;
import perf.parse.internal.JsonBuilder;
import perf.yaup.json.Json;

@State(Scope.Benchmark)
public class CompareExpRepeat {


   @Param({"a=apple","a=apple b=ball","a=apple b=ball c=cat","a=apple b=ball c=cat d=dog"})
   public String input;

   public ExpOld exp = new ExpOld("foo","(?<foo.key>\\w+)=(?<foo.value>\\w+)").eat(Eat.Match).set(Rule.Repeat)
         ;


   public Exp exp2 = new Exp("foo","(?<foo.key>\\w+)=(?<foo.value>\\w+)").eat(Eat.Match).setRule(MatchRule.Repeat)
         ;

   @Benchmark
   public void exp(){
      DropString dropString = new CheatChars(input);
      JsonBuilder builder = new JsonBuilder();
      exp.apply(dropString,builder,null);
      Json root = builder.getRoot();
      assert root.size() > 0;
   }

   @Benchmark
   public void exp2(){
      DropString dropString = new CheatChars(input);
      JsonBuilder builder = new JsonBuilder();
      exp2.apply(dropString,builder,null);
      Json root = builder.getRoot();
      assert root.size() > 0;
   }
}
