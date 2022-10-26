package io.hyperfoil.tools.parse;

import io.hyperfoil.tools.yaup.json.Json;

public class JsMatchAction extends JsFunction implements MatchAction {

   public JsMatchAction(String js){
      super(js);
   }

   @Override
   public void onMatch(String line, Json match, Exp pattern, Parser parser) {
      Json rtrn = execute(line,match,pattern,parser);
   }
}
