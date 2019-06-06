package perf.parse.factory;

import perf.parse.*;
import perf.yaup.file.FileUtility;
import perf.yaup.json.Json;

public class CsvFactory implements ParseFactory {

   @Override
   public void addToParser(Parser p) {
      p.add(headers());
   }

   @Override
   public Parser newParser() {
      Parser p  = new Parser();
      addToParser(p);
      return p;
   }


   public Exp headers(){
      return new Exp("header","^(?<header>[^,$]+)(?:,|$)")
         .addRule(ExpRule.Repeat)
         .execute((line,match,pattern,parser)->{
            Json headers = match.getJson("header");
            StringBuilder sb = new StringBuilder();
            for(int i=0; i<headers.size(); i++){
               String name = headers.get(i).toString();
               sb.append("(?<");
               sb.append(name.replaceAll("\\.","\\\\."));
               sb.append(">[^,$]+)(?:,|$)");
            }
            Exp dataExp = new Exp("data",sb.toString())
               .nest("data")
               .setMerge(ExpMerge.AsEntry).eat(Eat.Line).addRule(ExpRule.RemoveOnClose);
            parser.addAhead(dataExp);

         });
   }
}
