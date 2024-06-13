package io.hyperfoil.tools.parse;

import io.hyperfoil.tools.yaup.StringUtil;
import io.hyperfoil.tools.yaup.json.Json;
import io.hyperfoil.tools.yaup.json.graaljs.JsException;
import org.graalvm.polyglot.Source;
import org.jboss.logging.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.invoke.MethodHandles;
import java.util.Arrays;
import java.util.stream.Collectors;

public class JsFunction {

   final static Logger logger = Logger.getLogger(MethodHandles.lookup().lookupClass());

   private final String js;

   public JsFunction(String js){
      this.js = js;
   }

   public String getJs(){return js;}

   public Json execute(Object...arguments) {
      if(arguments==null ){
         return new Json(false);
      }
      String also = new BufferedReader(new InputStreamReader(getClass().getClassLoader().getResourceAsStream("jsonpath.js"))).lines()
                         .parallel().collect(Collectors.joining("\n"));
      Object fromJs = null;
      try {
         fromJs = StringUtil.jsEval(js, Arrays.asList(
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
                 "const Json = Java.type('io.hyperfoil.tools.yaup.json.Json');",
                 also
         ), arguments);
      }catch(JsException jse){
         logger.error("exception for js:\n"+js+"\n"+jse.getMessage());
      } finally {}
      if(fromJs == null){
         return new Json();
      }

      if(fromJs instanceof Json){
         return (Json)fromJs;
      }else{
         Json rtrn = new Json();
         rtrn.add(fromJs);
         return rtrn;
      }
   }
}
