package io.hyperfoil.tools.parse;

import io.hyperfoil.tools.yaup.StringUtil;
import io.hyperfoil.tools.yaup.json.Json;
import io.hyperfoil.tools.yaup.json.ValueConverter;
import io.hyperfoil.tools.yaup.json.graaljs.JsonProxy;
import io.hyperfoil.tools.yaup.json.graaljs.JsonProxyObject;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Value;

import java.util.Arrays;

public class JsFunction {

   private final String js;

   public JsFunction(String js){
      this.js = js;
   }

   public String getJs(){return js;}

   public Json execute(Object...arguments) {
      if(arguments==null ){
         return new Json(false);
      }
      for(int i=0; i<arguments.length; i++){
         if (arguments[i] != null && arguments[i] instanceof Json){
            arguments[i] = JsonProxy.create((Json)arguments[i]);
         }
      }
      Object fromJs = StringUtil.jsEval(js, Arrays.asList(
         "function milliseconds(v){ return Packages.io.hyperfoil.tool.yaup.StringUtil.parseKMG(v)};",
         "const StringUtil = Packages.io.hyperfoil.tool.yaup.StringUtil;",
         "const Exp = Java.type('io.hyperfoil.tools.parse.Exp');",
         "const ExpMerge = Java.type('io.hyperfoil.tools.parse.ExpMerge');",
         "const MatchRange = Java.type('io.hyperfoil.tools.parse.MatchRange');",
         "const Eat = Java.type('io.hyperfoil.tools.parse.Eat');",
         "const ValueType = Java.type('io.hyperfoil.tools.parse.ValueType')",
         "const ValueMerge = Java.type('io.hyperfoil.tools.parse.ValueMerge');",
         "const ExpRule = Java.type('io.hyperfoil.tools.parse.ExpRule')",
         "const FileUtility = Packages.io.hyperfoil.tools.yaup.file.FileUtility;",
         "const Xml = Java.type('io.hyperfoil.tools.yaup.xml.pojo.Xml');",
         "const Json = Java.type('io.hyperfoil.tools.yaup.json.Json');"
      ),arguments);

      if(fromJs instanceof Json){
         return (Json)fromJs;
      }else{
         Json rtrn = new Json();
         rtrn.add(fromJs);
         return rtrn;
      }

//      Value matcher = null;
//      try(Context context = Context.newBuilder("js").allowAllAccess(true).build()){
//         context.enter();
//         try {
//            context.eval("js", "function milliseconds(v){ return Packages.io.hyperfoil.tool.yaup.StringUtil.parseKMG(v)};");
//            context.eval("js", "const StringUtil = Packages.io.hyperfoil.tool.yaup.StringUtil;");
//            context.eval("js", "const Exp = Java.type('io.hyperfoil.tools.parse.Exp');");
//            context.eval("js", "const ExpMerge = Java.type('io.hyperfoil.tools.parse.ExpMerge');");
//            context.eval("js", "const MatchRange = Java.type('io.hyperfoil.tools.parse.MatchRange');");
//            context.eval("js", "const Eat = Java.type('io.hyperfoil.tools.parse.Eat');");
//            context.eval("js", "const ValueType = Java.type('io.hyperfoil.tools.parse.ValueType')");
//            context.eval("js", "const ValueMerge = Java.type('io.hyperfoil.tools.parse.ValueMerge');");
//            context.eval("js", "const ExpRule = Java.type('io.hyperfoil.tools.parse.ExpRule')");
//
//            context.eval("js", "const FileUtility = Packages.io.hyperfoil.tools.yaup.file.FileUtility;");
//            context.eval("js", "const Xml = Java.type('io.hyperfoil.tools.yaup.xml.pojo.Xml');");
//            context.eval("js", "const Json = Java.type('io.hyperfoil.tools.yaup.json.Json');");
//
//            if (context.getBindings("js").hasMember("js" + js.hashCode())) {
//               matcher = context.getBindings("js").getMember("js" + js.hashCode());
//            } else {
//               try {
//                  matcher = context.eval("js", js);
//               } catch (PolyglotException e) {
//                  System.err.println(e.getMessage());
//                  //TODO log that failed to evaluate matchExpression
//               }
//            }
//            if (matcher != null) {
//               Value result = null;
//               result = matcher.execute(arguments);
//               Object converted = ValueConverter.convert(result);
//               if (converted instanceof JsonProxyObject) {
//                  return ((JsonProxyObject) converted).getJson();
//               } else if (converted instanceof Json) {
//                  return (Json) converted;
//               }
//               if (result.isHostObject()) {
//                  Object hostObj = result.asHostObject();
//                  if (hostObj instanceof Json) {
//                     return (Json) hostObj;
//                  }
//               } else if (result.hasMembers()) {
//                  //TODO convert value to Json
//               }
//            }
//         } finally {
//            context.leave();
//         }
//      }
//      return new Json(false);
   }
}
