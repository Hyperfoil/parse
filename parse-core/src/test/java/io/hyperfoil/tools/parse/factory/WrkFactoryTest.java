package io.hyperfoil.tools.parse.factory;

/**
 *
 */


import io.hyperfoil.tools.parse.JsonConsumer;
import io.hyperfoil.tools.parse.Parser;
import io.hyperfoil.tools.yaup.json.Json;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class WrkFactoryTest {

    private static WrkFactory f;

    @BeforeClass
    public static void staticInit() {
        f = new WrkFactory();
    }

    @Test
    public void wrk_summary() {


        String test[] = new String[]{
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

        Json root = p.getBuilder().getRoot();

        assertTrue("has reqSec", root.has("reqSec"));
        assertEquals("73318.3", root.getString("reqSec"));
        assertEquals("471.32", root.getString("meanLatency"));
        assertEquals("http://benchserver4G1:8080/fruits", root.getString("url"));
        assertEquals("25", root.getString("connections"));

        p.close();
    }

    @Test
    public void wrk2_summary() {
        String test[] = new String[]{
                "Running 15s test @ http://localhost:8092/db\n",
                        "  24 threads and 1024 connections\n",
                        "  Thread calibration: mean lat.: 290.152ms, rate sampling interval: 1169ms\n",
                        "  Thread calibration: mean lat.: 303.501ms, rate sampling interval: 1215ms\n",
                        "  Thread calibration: mean lat.: 287.823ms, rate sampling interval: 1150ms\n",
                        "  Thread calibration: mean lat.: 298.585ms, rate sampling interval: 1191ms\n",
                        "  Thread calibration: mean lat.: 298.793ms, rate sampling interval: 1199ms\n",
                        "  Thread calibration: mean lat.: 312.523ms, rate sampling interval: 1159ms\n",
                        "  Thread calibration: mean lat.: 280.193ms, rate sampling interval: 1126ms\n",
                        "  Thread calibration: mean lat.: 284.830ms, rate sampling interval: 1149ms\n",
                        "  Thread calibration: mean lat.: 295.905ms, rate sampling interval: 1180ms\n",
                        "  Thread calibration: mean lat.: 289.936ms, rate sampling interval: 1160ms\n",
                        "  Thread calibration: mean lat.: 291.302ms, rate sampling interval: 1163ms\n",
                        "  Thread calibration: mean lat.: 298.033ms, rate sampling interval: 1192ms\n",
                        "  Thread calibration: mean lat.: 299.220ms, rate sampling interval: 1212ms\n",
                        "  Thread calibration: mean lat.: 284.450ms, rate sampling interval: 1132ms\n",
                        "  Thread calibration: mean lat.: 281.599ms, rate sampling interval: 1129ms\n",
                        "  Thread calibration: mean lat.: 283.727ms, rate sampling interval: 1137ms\n",
                        "  Thread calibration: mean lat.: 286.417ms, rate sampling interval: 1147ms\n",
                        "  Thread calibration: mean lat.: 281.016ms, rate sampling interval: 1133ms\n",
                        "  Thread calibration: mean lat.: 293.708ms, rate sampling interval: 1174ms\n",
                        "  Thread calibration: mean lat.: 335.480ms, rate sampling interval: 1259ms\n",
                        "  Thread calibration: mean lat.: 284.184ms, rate sampling interval: 1137ms\n",
                        "  Thread calibration: mean lat.: 291.517ms, rate sampling interval: 1166ms\n",
                        "  Thread calibration: mean lat.: 287.073ms, rate sampling interval: 1150ms\n",
                        "  Thread calibration: mean lat.: 286.525ms, rate sampling interval: 1150ms\n",
                        "  Thread Stats   Avg      Stdev     Max   +/- Stdev\n",
                        "    Latency   827.21ms  105.45ms   1.04s    66.22%\n",
                        "    Req/Sec     1.25k    68.04     1.35k    78.26%\n",
                        "  Latency Distribution (HdrHistogram - Recorded Latency)\n",
                        " 50.000%  823.81ms\n",
                        " 75.000%  908.29ms\n",
                        " 90.000%  967.68ms\n",
                        " 99.000%    1.02s \n",
                        " 99.900%    1.03s \n",
                        " 99.990%    1.04s \n",
                        " 99.999%    1.04s \n",
                        "100.000%    1.04s \n",
                        "\n",
                        "  Detailed Percentile spectrum:\n",
                        "       Value   Percentile   TotalCount 1/(1-Percentile)\n",
                        "\n",
                        "     570.367     0.000000            6         1.00\n",
                        "     675.327     0.100000        14438         1.11\n",
                        "     739.327     0.200000        28875         1.25\n",
                        "     780.287     0.300000        43375         1.43\n",
                        "     801.791     0.400000        57601         1.67\n",
                        "     823.807     0.500000        72107         2.00\n",
                        "     844.799     0.550000        79212         2.22\n",
                        "     859.135     0.600000        86353         2.50\n",
                        "     870.911     0.650000        93786         2.86\n",
                        "     884.223     0.700000       100895         3.33\n",
                        "     908.287     0.750000       107917         4.00\n",
                        "     918.015     0.775000       111473         4.44\n",
                        "     925.183     0.800000       115184         5.00\n",
                        "     932.351     0.825000       118803         5.71\n",
                        "     940.031     0.850000       122306         6.67\n",
                        "     948.735     0.875000       125940         8.00\n",
                        "     955.391     0.887500       127710         8.89\n",
                        "     967.679     0.900000       129524        10.00\n",
                        "     978.943     0.912500       131298        11.43\n",
                        "     993.279     0.925000       133097        13.33\n",
                        "     998.911     0.937500       134916        16.00\n",
                        "    1001.471     0.943750       135816        17.78\n",
                        "    1004.543     0.950000       136718        20.00\n",
                        "    1007.103     0.956250       137557        22.86\n",
                        "    1010.175     0.962500       138520        26.67\n",
                        "    1012.735     0.968750       139384        32.00\n",
                        "    1014.271     0.971875       139826        35.56\n",
                        "    1016.319     0.975000       140327        40.00\n",
                        "    1017.855     0.978125       140728        45.71\n",
                        "    1019.391     0.981250       141200        53.33\n",
                        "    1020.927     0.984375       141694        64.00\n",
                        "    1021.439     0.985938       141860        71.11\n",
                        "    1021.951     0.987500       142059        80.00\n",
                        "    1022.975     0.989062       142356        91.43\n",
                        "    1023.487     0.990625       142485       106.67\n",
                        "    1024.511     0.992188       142708       128.00\n",
                        "    1025.535     0.992969       142931       142.22\n",
                        "    1026.047     0.993750       143036       160.00\n",
                        "    1026.559     0.994531       143121       182.86\n",
                        "    1027.071     0.995313       143189       213.33\n",
                        "    1028.095     0.996094       143342       256.00\n",
                        "    1028.095     0.996484       143342       284.44\n",
                        "    1028.607     0.996875       143428       320.00\n",
                        "    1029.119     0.997266       143488       365.71\n",
                        "    1029.631     0.997656       143544       426.67\n",
                        "    1030.143     0.998047       143569       512.00\n",
                        "    1030.655     0.998242       143591       568.89\n",
                        "    1031.167     0.998437       143617       640.00\n",
                        "    1031.679     0.998633       143646       731.43\n",
                        "    1032.191     0.998828       143688       853.33\n",
                        "    1032.703     0.999023       143720      1024.00\n",
                        "    1032.703     0.999121       143720      1137.78\n",
                        "    1032.703     0.999219       143720      1280.00\n",
                        "    1033.215     0.999316       143758      1462.86\n",
                        "    1033.215     0.999414       143758      1706.67\n",
                        "    1033.727     0.999512       143775      2048.00\n",
                        "    1033.727     0.999561       143775      2275.56\n",
                        "    1033.727     0.999609       143775      2560.00\n",
                        "    1034.239     0.999658       143785      2925.71\n",
                        "    1034.751     0.999707       143791      3413.33\n",
                        "    1037.311     0.999756       143797      4096.00\n",
                        "    1037.823     0.999780       143802      4551.11\n",
                        "    1037.823     0.999805       143802      5120.00\n",
                        "    1038.847     0.999829       143816      5851.43\n",
                        "    1038.847     0.999854       143816      6826.67\n",
                        "    1038.847     0.999878       143816      8192.00\n",
                        "    1038.847     0.999890       143816      9102.22\n",
                        "    1038.847     0.999902       143816     10240.00\n",
                        "    1039.359     0.999915       143824     11702.86\n",
                        "    1039.359     0.999927       143824     13653.33\n",
                        "    1039.359     0.999939       143824     16384.00\n",
                        "    1039.359     0.999945       143824     18204.44\n",
                        "    1039.359     0.999951       143824     20480.00\n",
                        "    1039.359     0.999957       143824     23405.71\n",
                        "    1039.871     0.999963       143828     27306.67\n",
                        "    1039.871     0.999969       143828     32768.00\n",
                        "    1039.871     0.999973       143828     36408.89\n",
                        "    1039.871     0.999976       143828     40960.00\n",
                        "    1039.871     0.999979       143828     46811.43\n",
                        "    1039.871     0.999982       143828     54613.33\n",
                        "    1039.871     0.999985       143828     65536.00\n",
                        "    1040.383     0.999986       143829     72817.78\n",
                        "    1040.383     0.999988       143829     81920.00\n",
                        "    1040.383     0.999989       143829     93622.86\n",
                        "    1040.383     0.999991       143829    109226.67\n",
                        "    1040.383     0.999992       143829    131072.00\n",
                        "    1040.895     0.999993       143830    145635.56\n",
                        "    1040.895     1.000000       143830          inf\n",
                        "#[Mean    =      827.212, StdDeviation   =      105.454]\n",
                        "#[Max     =     1040.384, Total count    =       143830]\n",
                        "#[Buckets =           27, SubBuckets     =         2048]\n",
                        "----------------------------------------------------------\n",
                        "  441147 requests in 15.00s, 65.96MB read\n",
                        "  Socket errors: connect 11, read 0, write 0, timeout 77\n",
                        "Requests/sec:  29402.22\n",
                        "Transfer/sec:      4.40MB\n"
        };


        Parser p = f.newParser();
        JsonConsumer.List consumer = new JsonConsumer.List();
        p.add(consumer);
        for (String line : test) {
            p.onLine(line);
        }

        Json root = p.getBuilder().getRoot();

        assertTrue("has reqSec", root.has("reqSec"));
        assertEquals("29402.22", root.getString("reqSec"));
        assertEquals("827.21", root.getString("meanLatency"));
        assertEquals("http://localhost:8092/db", root.getString("url"));
        assertEquals("1024", root.getString("connections"));
        assertTrue(root.getJson("latencyDistribution").isArray());

        p.close();
    }

}
