package io.hyperfoil.tools.parse;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Value;
import io.hyperfoil.tools.yaup.json.Json;

import java.util.function.Function;

public class JsTransform implements Function<Json, Json> {

   private Context context;
   private String js;

   public JsTransform(String js){
      this.js = js;
      this.context = Context.newBuilder("js").allowAllAccess(true).allowHostAccess(true).build();
      context.enter();
      context.eval("js","function milliseconds(v){ return Packages.perf.yaup.StringUtil.parseKMG(v)};");
      context.eval("js","const StringUtil = Packages.perf.yaup.StringUtil;");
      context.eval("js","const Exp = Java.type('io.hyperfoil.tools.parse.Exp');");
      context.eval("js","const ExpMerge = Java.type('io.hyperfoil.tools.parse.ExpMerge');");
      context.eval("js","const MatchRange = Java.type('io.hyperfoil.tools.parse.MatchRange');");
      context.eval("js","const Eat = Java.type('io.hyperfoil.tools.parse.Eat');");
      context.eval("js","const ValueType = Java.type('io.hyperfoil.tools.parse.ValueType')");
      context.eval("js","const ValueMerge = Java.type('io.hyperfoil.tools.parse.ValueMerge');");
      context.eval("js","const ExpRule = Java.type('io.hyperfoil.tools.parse.ExpRule')");
      context.eval("js","");

      context.eval("js","const console = {log: print}");

      context.leave();
   }

   @Override
   public Json apply(Json json) {
      Value matcher = null;
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
         Value result = matcher.execute(json);
         if(result.isHostObject()){
            Object hostObj = result.asHostObject();
            if(hostObj instanceof Json){
               return (Json)hostObj;
            }
         }else if (result.hasMembers()){
            //TODO convert value to Json
         }
      }
      return json;
   }
}
