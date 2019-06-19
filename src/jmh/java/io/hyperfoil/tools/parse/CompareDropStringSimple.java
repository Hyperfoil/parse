package io.hyperfoil.tools.parse;

import io.hyperfoil.tools.parse.Eat;
import io.hyperfoil.tools.parse.Exp;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import io.hyperfoil.tools.parse.internal.CheatChars;
import io.hyperfoil.tools.parse.internal.DropString;
import io.hyperfoil.tools.parse.internal.JsonBuilder;
import io.hyperfoil.tools.parse.internal.SharedString;
import io.hyperfoil.tools.yaup.json.Json;

@State(Scope.Benchmark)
public class CompareDropStringSimple {


   @Param({"a=apple b=ball c=cat"})
   public String input;

   public Exp exp = new Exp("foo","(?<foo.key>\\w+)=(?<foo.value>\\w+)").eat(Eat.Match)
         .add(new Exp("bar","(?<bar.key>\\w+)=(?<bar.value>=\\w+)").eat(Eat.Match))
         .add(new Exp("buz","(?<buz.key>\\w+)=(?<buz.value>=\\w+)").eat(Eat.Match));


   public Exp exp2 = new Exp("foo","(?<foo.key>\\w+)=(?<foo.value>\\w+)").eat(Eat.Match)
      .add(new Exp("bar","(?<bar.key>\\w+)=(?<bar.value>=\\w+)").eat(Eat.Match))
      .add(new Exp("buz","(?<buz.key>\\w+)=(?<buz.value>=\\w+)").eat(Eat.Match));


   @Benchmark
   public void exp_cheat(){
      DropString dropString = new CheatChars(input);
      JsonBuilder builder = new JsonBuilder();
      exp.apply(dropString,builder,null);
      Json root = builder.getRoot();
      assert root.size() > 0;
   }
   @Benchmark
   public void exp_shared(){
      DropString dropString = new SharedString(input);
      JsonBuilder builder = new JsonBuilder();
      exp.apply(dropString,builder,null);
      Json root = builder.getRoot();
      assert root.size() > 0;
   }

   @Benchmark
   public void exp2_cheat(){
      DropString dropString = new CheatChars(input);
      JsonBuilder builder = new JsonBuilder();
      exp2.apply(dropString,builder,null);
      Json root = builder.getRoot();
      assert root.size() > 0;
   }
   @Benchmark
   public void exp2_shared(){
      DropString dropString = new SharedString(input);
      JsonBuilder builder = new JsonBuilder();
      exp2.apply(dropString,builder,null);
      Json root = builder.getRoot();
      assert root.size() > 0;
   }
}
