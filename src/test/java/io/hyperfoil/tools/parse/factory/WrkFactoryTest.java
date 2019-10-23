package io.hyperfoil.tools.parse.factory;

/**
 *
 */


import io.hyperfoil.tools.parse.JsonConsumer;
import io.hyperfoil.tools.parse.Parser;
import io.hyperfoil.tools.yaup.json.Json;
import org.junit.BeforeClass;
import org.junit.Test;

public class WrkFactoryTest {

    private static WrkFactory f;

    @BeforeClass
    public static void staticInit() {
        f = new WrkFactory();
    }

    @Test
    public void exception_multiline_message() {
        String test[] = new String[]{
                "Running 1m test @ http://benchserver4G1:8080/fruits\n",
                "  25 threads and 25 connections\n",
                "  Thread Stats   Avg\t  Stdev     Max   +/- Stdev\n",
                "    Latency   472.18us  747.97us  19.98ms   93.33%\n",
                "    Req/Sec     2.97k   145.08     4.73k    74.41%\n",
                "  4434796 requests in 1.00m, 0.85GB read\n",
                "Requests/sec:  73790.28\n",
                "Transfer/sec:     14.57MB\n",
                "Running 1m test @ http://benchserver4G1:8080/fruits\n",
                "  25 threads and 25 connections\n",
                "  Thread Stats   Avg\t  Stdev     Max   +/- Stdev\n",
                "    Latency   471.32us  703.34us  16.86ms   92.85%\n",
                "    Req/Sec     2.95k   132.44     4.59k    74.09%\n",
                "  4406327 requests in 1.00m, 869.86MB read\n",
                "Requests/sec:  73318.30\n",
                "Transfer/sec:     14.47MB"
        };


        Parser p = f.newParser();
        JsonConsumer.List consumer = new JsonConsumer.List();
        p.add(consumer);
        for (String line : test) {
            p.onLine(line);
        }
        //p.close();
        Json root = p.getBuilder().getRoot();
    }
}
