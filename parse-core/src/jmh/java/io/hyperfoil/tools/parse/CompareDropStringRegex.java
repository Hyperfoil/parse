package io.hyperfoil.tools.parse;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import io.hyperfoil.tools.parse.internal.CheatChars;
import io.hyperfoil.tools.parse.internal.SharedString;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@State(Scope.Benchmark)
public class CompareDropStringRegex {


   @Param({"Average GC time (845.14 ms) is above the time for allocation rate (7.57 MB/s) to deplete free headroom (0M)"})
   public String input;


   Pattern pattern = Pattern.compile("Average GC time \\((?<milliseconds>\\d+\\.\\d{2}) ms\\) is above the time for allocation rate \\((?<rate>\\d+\\.\\d{2}) MB/s\\) to deplete free headroom \\((?<free>\\d+[bBkKmMgG])\\)");

   @Benchmark
   public void cheatChars(){
      CheatChars cheatChars = new CheatChars(input);
      Matcher m = pattern.matcher(cheatChars);
      assert m.matches();
   }

   @Benchmark
   public void sharedString(){
      SharedString sharedString = new SharedString(input);
      Matcher m = pattern.matcher(sharedString);
      assert m.matches();
   }
}
