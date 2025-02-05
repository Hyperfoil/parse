package io.hyperfoil.tools.parse.file;

import io.hyperfoil.tools.parse.JsJsonFunction;
import io.hyperfoil.tools.parse.JsPathFunction;
import io.hyperfoil.tools.yaup.json.Json;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;

public class FileRuleTest {

   public static String join(String...args){
      return Arrays.asList(args).stream().collect(Collectors.joining("\n"));
   }
   public static String writeFile(String...lines){
      String content = join(lines);
      try {
         File f = File.createTempFile(FileRule.class.getSimpleName(),null);
         f.deleteOnExit();
         Files.write(f.toPath(),content.getBytes());
         return f.getPath();
      } catch (IOException e) {
         e.printStackTrace();
      }
      return null;
   }

   @Test
   public void asJson_return_javascript_object(){
      String path = writeFile("{\"foo\":\"bar\",\"biz\":\"buz\"}");
      FileRule rule = new FileRule("asJson").setCriteria(
         new MatchCriteria("")
      ).setConverter(new JsonConverter().andThen( new JsJsonFunction(
         """
         (data)=>{
           const { foo, biz } = data;
           return { data: [foo, biz] }
         }
         """
      )));
      List<Json> found = new ArrayList<>();
      rule.apply(path,(nest,json)->{
         found.add(json);
      });
      assertEquals("should only have 1 entry",1,found.size());
      Json entry = found.get(0);
      assertEquals("expected json data",Json.fromJs("{data:['bar','buz']}"),entry);
   }

   @Test
   public void asPath_return_javascript_object(){
         String path = writeFile("1234","{\"foo\":\"bar\"}");
         FileRule rule = new FileRule("timestampJson").setCriteria(
            new MatchCriteria("")
         ).setConverter(new JsPathFunction(join("  (path)=>{" ,
            "    const rtrn = []" ,
            "    FileUtility.stream(path).forEach(line => {" ,
            "      if(line.trim() === \"\"){" ,
            "" ,
            "      } else if (line.match( /^\\d+$/ ) ) {" ,
            "        rtrn.push({});" ,
            "        rtrn[rtrn.length-1].timestamp = parseInt(line);" ,
            "      } else {" ,
            "        rtrn[rtrn.length-1].data = JSON.parse(line);" ,
            "      }" ,
            "    });" ,
            "    return rtrn;" ,
            "  }")));

         List<Json> found = new ArrayList<>();
         rule.apply(path,(nest,json)->{
            found.add(json);
         });
         assertEquals("should only have 1 entry",1,found.size());
         Json entry = found.get(0);
         assertEquals("expected json data",Json.fromJs("[{timestamp:1234,data:{foo:'bar'}}]"),entry);


   }
}
