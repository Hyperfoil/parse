package perf.parse.internal;

import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SharedStringTest {

   @Test
   public void subSequence_start(){
      SharedString test = new SharedString("foo=bar");
      assertEquals("foo",test.subSequence(0,3).toString());
   }
   @Test
   public void subSequence_end(){
      SharedString test = new SharedString("foo=bar");
      assertEquals("bar",test.subSequence(4,7).toString());
   }

   @Test
   public void use_regex(){
      SharedString test = new SharedString("foo=bar");
      Matcher m = java.util.regex.Pattern.compile("(?<key>\\w+)=(?<value>\\w+)").matcher(test);
      assertTrue(m.matches());
      assertEquals("foo",m.group("key"));
      assertEquals("bar",m.group("value"));
   }
   @Test
   public void regex_word_space(){
      SharedString test = new SharedString("foo=bar biz=buz");
   }
   @Test
   public void drop_start(){
      SharedString test = new SharedString("0123456");
      test.drop(0,2);
      assertEquals("23456",test.toString());
   }
   @Test
   public void drop_end(){
      SharedString test = new SharedString("0123456");
      test.drop(test.length()-2,test.length());
      assertEquals("01234",test.toString());
   }

   @Test
   public void drop_middle(){
      SharedString test = new SharedString("0123456");
      test.drop(1,4);

      assertEquals("0456",test.toString());
   }

   @Test
   public void drop_parent_does_not_change_child(){
      String input = "abcdefghij";
      SharedString parent = new SharedString(input);
      SharedString child = new SharedString(input,0,5,parent);

      parent.drop(0,1);

      assertEquals("abcde",child.toString());
      assertEquals("bcdefghij",parent.toString());

   }

   @Test
   public void drop_parent_then_child_not_change_parent(){
      String input = "abcdefghij";
      SharedString parent = new SharedString(input);
      SharedString child = new SharedString(input,0,5,parent);

      parent.drop(0,1);
      child.drop(0,1);
      assertEquals("bcde",child.toString());
      assertEquals("bcdefghij",parent.toString());
   }

   @Test
   public void drop_child_removes_parent(){
      String input = "abcdefghij";
      SharedString parent = new SharedString(input);
      SharedString child = new SharedString(input,0,5,parent);

      child.drop(1,2);
      assertEquals("acde",child.toString());
      assertEquals("acdefghij",parent.toString());
   }
   @Test
   public void drop_child_removes_from_parent(){
      String input = "abcdefghij";
      SharedString parent = new SharedString(input);
      SharedString child = new SharedString(input,0,5,parent);
      parent.drop(1,2);
      child.drop(0,3);
      assertEquals("de",child.toString());
      assertEquals("defghij",parent.toString());
   }

   @Test
   public void reference_before_drop_start(){
      String input = "1234567890";
      SharedString test = new SharedString(input);
      DropString.Ref ref = test.reference(0);
      test.drop(1,2);
      assertEquals("134567890",test.toString());
      assertEquals(0,ref.get());
   }
   @Test
   public void reference_after_drop_end(){
      String input = "1234567890";
      SharedString test = new SharedString(input);
      DropString.Ref ref = test.reference(4);
      test.drop(1,3);
      assertEquals("14567890",test.toString());
      assertEquals(2,ref.get());
   }
   @Test
   public void refernce_in_drop_range(){
      String input = "1234567890";
      SharedString test = new SharedString(input);
      DropString.Ref ref = test.reference(4);
      test.drop(1,6);
      assertEquals("17890",test.toString());
      assertEquals(1,ref.get());
   }
   @Test
   public void reference_updated_by_child_drop(){
      String input = "abcdefghij";
      SharedString parent = new SharedString(input);
      SharedString child = new SharedString(input,0,5,parent);
      DropString.Ref ref = parent.reference(5);
      child.drop(0,5);
      assertEquals(0,ref.get());
   }
}
