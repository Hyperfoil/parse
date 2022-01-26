package io.hyperfoil.tools.parse;

import io.hyperfoil.tools.yaup.json.Json;
import org.junit.Test;

import java.util.Arrays;

public class JsFunctionTest {

    @Test
    public void async_fetch(){
        JsFunction jsFunction = new JsFunction("async (input)=>{ console.log(input); const bar = await fetch('https://www.redhat.com/en').then((x,y)=>console.log('found',x,y),x=>console.log('yikes',x)); console.log('bar',bar);}");
        jsFunction.execute("foo");
    }


    @Test
    public void async_token(){
        JsFunction jsFunction = new JsFunction(
                "async (input)=>{"+
                "    console.log('basic',btoa(\"perf:100yard-\"));\n" +
                "    const token = await fetch('https://fail.oauth-openshift.apps.test.perf-lab-myocp4.com/oauth/authorize?response_type=token&client_id=openshift-challenging-client',\n" +
                "      { \n" +
                "        tls : 'ignore', \n" +
                "        method: 'HEAD', \n" +
                "        redirect: 'ignore', \n" +
                "        headers: {\n" +
                "          'Authorization' : 'Basic '+btoa('perf:100yard-'),\n" +
                "          'Content-Type' : 'application/json'\n" +
                "        }\n" +
                "      }\n" +
                "    )\n" +
                "    .then(resp=>resp.headers.Location)\n" +
                "    .then(loc=>loc.match(/access_token=([^&]+)/)[1]);\n" +
                "    console.log('token',token);\n" +
                "    return token;\n" +
                "}");

        Json resp = jsFunction.execute("");
    }
}
