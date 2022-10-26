package io.hyperfoil.tools.parse;

import io.hyperfoil.tools.yaup.json.Json;

import java.util.function.Function;

public class JsJsonFunction extends JsFunction implements Function<Json, Json> {

   private String js;

   public JsJsonFunction(String js){
      super(js);
   }

   @Override
   public Json apply(Json json) {
      return execute(json);
   }
}
