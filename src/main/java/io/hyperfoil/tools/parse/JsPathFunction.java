package io.hyperfoil.tools.parse;

import io.hyperfoil.tools.yaup.json.Json;

import java.util.function.Function;

public class JsPathFunction extends JsFunction implements Function<String, Json> {

   public JsPathFunction(String js){
      super(js);
   }

   @Override
   public Json apply(String path) {
      return execute(path);
   }
}
