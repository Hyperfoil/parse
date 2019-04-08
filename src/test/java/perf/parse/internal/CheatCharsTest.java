package perf.parse.internal;

import org.junit.Test;

import java.util.regex.Matcher;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 *
 */
public class CheatCharsTest {


    @Test
    public void regexTest(){
        CheatChars test = new CheatChars("foo=bar");
        Matcher m = java.util.regex.Pattern.compile("(?<key>\\w+)=(?<value>\\w+)").matcher(test);

        assertTrue(m.matches());
    }

    @Test
    public void dropStart(){
        CheatChars test = new CheatChars("0123456");
        test.drop(0,2);
        assertEquals("23456",test.toString());
    }

    @Test
    public void dropEnd(){
        CheatChars test = new CheatChars("0123456");
        test.drop(test.length()-2,test.length());
        assertEquals("01234",test.toString());
    }
    @Test
    public void dropMiddle(){
        CheatChars test = new CheatChars("0123456");
        test.drop(1,4);
        assertEquals("0456",test.toString());
        //System.out.println("0123456".substring(1,4));
    }
}
