package io.hyperfoil.tools.parse.file;

import io.hyperfoil.tools.yaup.file.FileUtility;
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
      //String content = new String(Files.readAllBytes(Paths.get(s))); //this didn't work with our ARCHIVE_KEY separated paths
      String content = FileUtility.readFile(s);
      rtrn.set(getKey(),content);
      return rtrn;
   }
}
