package io.hyperfoil.tools.parse;

import org.junit.Assert;
import org.junit.Test;

public class JsMatchActionTest {

   @Test
   public void create_Exp_simple(){
      Parser p = new Parser();
      Exp e = new Exp(".*");
      e.execute(new JsMatchAction("(line,match,exp,parser)=>{" +
         "parser.addAhead(new Exp('first','foo'));" +
         "}"));
      p.add(e);
      p.onLine("test");
      p.close();
      Assert.assertEquals("expect parser to have 2 exp",2,p.exps().size());
      Assert.assertEquals("first exp should be named first","first",p.exps().get(0).getName());
   }
   @Test
   public void create_Exp_with_rule(){
      Parser p = new Parser();
      Exp e = new Exp(".*");
      e.execute(new JsMatchAction("(line,match,exp,parser)=>{" +
         "parser.addAhead(new Exp('first','foo').addRule(ExpRule.Repeat));" +
         "}"));
      p.add(e);
      p.onLine("test");
      p.close();
      Assert.assertEquals("expect parser to have 2 exp",2,p.exps().size());
      Assert.assertEquals("first exp should be named first","first",p.exps().get(0).getName());
      Assert.assertTrue("first should repeat",p.exps().get(0).hasRule(ExpRule.Repeat));
   }
}
