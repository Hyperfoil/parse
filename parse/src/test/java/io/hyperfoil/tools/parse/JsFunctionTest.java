package io.hyperfoil.tools.parse;

import io.hyperfoil.tools.yaup.json.Json;
import org.junit.Ignore;
import org.junit.Test;

import java.util.Arrays;

public class JsFunctionTest {

    @Test
    public void async_fetch(){
        JsFunction jsFunction = new JsFunction("async (input)=>{ console.log(input); const bar = await fetch('https://www.redhat.com/en').then((x,y)=>console.log('found',x,y),x=>console.log('yikes',x)); console.log('bar',bar);}");
        jsFunction.execute("foo");
    }


}
