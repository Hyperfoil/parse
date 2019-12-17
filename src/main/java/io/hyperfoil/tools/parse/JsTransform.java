package io.hyperfoil.tools.parse;

import io.hyperfoil.tools.yaup.json.ValueConverter;
import io.hyperfoil.tools.yaup.json.graaljs.JsonProxyObject;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Value;
import io.hyperfoil.tools.yaup.json.Json;

import java.util.function.Function;

public class JsTransform implements Function<Json, Json> {

   private String js;

   public JsTransform(String js){
      this.js = js;

   }

   @Override
   public Json apply(Json json) {
      JsonProxyObject jsonProxyObject = new JsonProxyObject(json);
      Value matcher = null;
      try(Context context = Context.newBuilder("js").allowAllAccess(true).build()){
         context.enter();
         try {
            context.eval("js", "function milliseconds(v){ return Packages.perf.yaup.StringUtil.parseKMG(v)};");
            context.eval("js", "const StringUtil = Packages.perf.yaup.StringUtil;");
            context.eval("js", "const Exp = Java.type('io.hyperfoil.tools.parse.Exp');");
            context.eval("js", "const ExpMerge = Java.type('io.hyperfoil.tools.parse.ExpMerge');");
            context.eval("js", "const MatchRange = Java.type('io.hyperfoil.tools.parse.MatchRange');");
            context.eval("js", "const Eat = Java.type('io.hyperfoil.tools.parse.Eat');");
            context.eval("js", "const ValueType = Java.type('io.hyperfoil.tools.parse.ValueType')");
            context.eval("js", "const ValueMerge = Java.type('io.hyperfoil.tools.parse.ValueMerge');");
            context.eval("js", "const ExpRule = Java.type('io.hyperfoil.tools.parse.ExpRule')");

            //context.eval("js","const console = {log: print}");


            if (context.getBindings("js").hasMember("js" + js.hashCode())) {
               matcher = context.getBindings("js").getMember("js" + js.hashCode());
            } else {
               try {
                  matcher = context.eval("js", js);
               } catch (PolyglotException e) {
                  System.err.println(e.getMessage());
                  //TODO log that fialed to evaluate matchExpression
               }

            }
            if (matcher != null) {

               Value result = matcher.execute(jsonProxyObject);
               Object converted = ValueConverter.convert(result);
               if (converted instanceof JsonProxyObject) {
                  return ((JsonProxyObject) converted).getJson();
               } else if (converted instanceof Json) {
                  return (Json) converted;
               }
               if (result.isHostObject()) {
                  Object hostObj = result.asHostObject();
                  if (hostObj instanceof Json) {
                     return (Json) hostObj;
                  }
               } else if (result.hasMembers()) {
                  //TODO convert value to Json
               }
            }
         } finally {
            context.leave();
         }
      }



      return json;
   }
}
