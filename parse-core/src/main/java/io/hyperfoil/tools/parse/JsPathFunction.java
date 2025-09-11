package io.hyperfoil.tools.parse;

import io.hyperfoil.tools.yaup.json.Json;

import java.util.function.BiFunction;
import java.util.function.Function;

public class JsPathFunction extends JsFunction implements BiFunction<String, Json, Json> {

   public JsPathFunction(String js){
      super(js);
   }

   @Override
   public Json apply(String path,Json state) {
      return execute(new Json(),path);
   }
}
