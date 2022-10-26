package io.hyperfoil.tools.parse.file;

import io.hyperfoil.tools.yaup.json.Json;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.function.Function;

public class ContentConverter implements Function<String, Json> {


   private String key = "content";

   public ContentConverter(){
   }

   public void setKey(String key){
      this.key = key;
   }
   public String getKey(){return key;}


   @Override
   public Json apply(String s) {
      Json rtrn = new Json();
      try {
         String content = new String(Files.readAllBytes(Paths.get(s)));
         rtrn.set(getKey(),content);
      } catch (IOException e) {
         e.printStackTrace();
      }
      return rtrn;
   }
}
