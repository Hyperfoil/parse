package io.hyperfoil.tools.parse.factory;

import io.hyperfoil.tools.parse.Eat;
import io.hyperfoil.tools.parse.Exp;
import io.hyperfoil.tools.parse.ExpRule;
import io.hyperfoil.tools.parse.Parser;

/**
 * Created by johara
 * WrkFactory - creates a Parser for wrk logs
 */
public class WrkFactory implements ParseFactory {

    public Exp connections() { return new Exp("\\s+(?<threads>\\d*)\\sthreads and\\s*(?<connections>\\d*)\\s+connections"); }

    public Exp testConfig() { return new Exp("Running (?<duration>\\d*)(?<durationUnit>\\w*)\\stest @ (?<url>.*)"); }

    public Exp reqSec() {
        return new Exp("reqSec", "Requests/sec:\\s+(?<reqSec>\\d*\\.\\d*)");
    }

    public Exp latency() { return new Exp("latency", "\\s*Latency\\s*(?<meanLatency>\\d*\\.\\d*)(?<meanLatencyUnit>\\w*)\\s*(?<stdDevLatency>\\d*\\.\\d*)(?<stdDevLatencyUnit>\\w*)\\s*(?<maxLatency>\\d*\\.\\d*)(?<maxLatencyUnit>\\w*)\\s*(?<stdDevPercent>\\d*\\.\\d*)"); }


//                        " 50.000%  823.81ms\n" +
//                                " 75.000%  908.29ms\n" +
//                                " 90.000%  967.68ms\n" +
//                                " 99.000%    1.02s \n" +
//                                " 99.900%    1.03s \n" +
//                                " 99.990%    1.04s \n" +
//                                " 99.999%    1.04s \n" +
//                                "100.000%    1.04s \n" +

    public Exp LatencyDistributionHeader(){ return new Exp("latencyDistributionHeader", "\\s*Latency Distribution.*");
    }
    public Exp latencyDistribution() { return new Exp("latencyDistribution", "^\\s*(?<centile>\\d*\\.\\d*)%\\s*(?<latencyVal>\\d*\\.\\d*)(?<latencyUnit>\\w*)\\s*$"); }

    @Override
    public Parser newParser() {
        Parser p = new Parser();
        addToParser(p);
        return p;
    }

    @Override
    public void addToParser(Parser p) {
        p.add(testConfig().addRule(ExpRule.PreClose));
        p.add(connections());
        p.add(latency());
        p.add(reqSec());
        p.add(
                LatencyDistributionHeader()
                        .eat(Eat.Line)
                        .addRule(ExpRule.Repeat)
                        .add(latencyDistribution())
        );
    }
}
