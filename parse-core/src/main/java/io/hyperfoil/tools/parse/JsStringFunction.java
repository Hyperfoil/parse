package io.hyperfoil.tools.parse;

import io.hyperfoil.tools.yaup.json.Json;

import java.util.function.BiFunction;
import java.util.function.Function;

public class JsStringFunction extends JsFunction implements BiFunction<String, Json, Json> {

   public JsStringFunction(String js) {
      super(js);
   }

   @Override
   public Json apply(String s,Json state) {
      return execute(new Json(),s);
   }
}
