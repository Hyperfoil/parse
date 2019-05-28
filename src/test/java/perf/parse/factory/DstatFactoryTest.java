package perf.parse.factory;

import org.junit.BeforeClass;
import org.junit.Test;
import perf.parse.Parser;
import perf.yaup.json.Json;

import java.util.LinkedList;
import java.util.List;

public class DstatFactoryTest {
   private static DstatFactory f;

   @BeforeClass
   public static void staticInit(){
      f = new DstatFactory();
   }

   @Test
   public void epoch_merge(){
      Parser p = f.newParser();
      final List<Json> found = new LinkedList<>();
      p.add(found::add);

      p.onLine("^[[7l--epoch--- ----total-cpu-usage---- -dsk/total- -net/total- ---paging-- ---system-- ------memory-usage-----");
      p.onLine("  epoch   |usr sys idl wai hiq siq| read  writ| recv  send|  in   out | int   csw | used  buff  cach  free");
      p.onLine("1552513095|  0   0 100   0   0   0|2679B   44k|   0     0 | 598B  742B|1409  1675 |8592M 3304k 77.6G  166G");
      p.close();
      found.forEach(j->{
         System.out.println(j.toString(2));
      });
   }
}
