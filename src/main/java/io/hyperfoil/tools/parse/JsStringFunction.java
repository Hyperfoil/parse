package io.hyperfoil.tools.parse;

import io.hyperfoil.tools.yaup.json.Json;

import java.util.function.Function;

public class JsStringFunction extends JsFunction implements Function<String, Json> {

   public JsStringFunction(String js) {
      super(js);
   }

   @Override
   public Json apply(String s) {
      return execute(s);
   }
}
