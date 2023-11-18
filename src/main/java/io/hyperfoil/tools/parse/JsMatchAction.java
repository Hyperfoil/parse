package io.hyperfoil.tools.parse;

import io.hyperfoil.tools.yaup.json.Json;

/**
 * An action that runs javascript after an Exp matches the input line. The javascript function receives 4 parameters: line, match, pattern, parser
 * line - the input string that matched
 * match - The json that contains the matched values
 * pattern - the Exp that matched the line
 * parser - the Parser that ran the Exp.
 *
 * The CSV parser could use a JsMatchAction to create a custom Exp based on the name of the columns in the first row of the input:
 * (line,match,exp,parser)=>{
 *     parser.addAhead(new Exp('first',match.map(key=>`(?<${key}[^,]+)`).join(',')))
 * }
 * Using addAhead ensures the new Exp will run before the Exp that matched the header row.
 */
public class JsMatchAction extends JsFunction implements MatchAction {

   public JsMatchAction(String js){
      super(js);
   }

   @Override
   public void onMatch(String line, Json match, Exp pattern, Parser parser) {
      Json rtrn = execute(line,match,pattern,parser);
   }
}
