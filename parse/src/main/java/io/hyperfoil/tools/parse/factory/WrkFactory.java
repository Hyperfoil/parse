package io.hyperfoil.tools.parse.factory;

import io.hyperfoil.tools.parse.*;

/**
 * Created by johara
 * WrkFactory - creates a Parser for wrk/wrk2 logs
 */
public class WrkFactory implements ParseFactory {

    public Exp connections() { return new Exp("connections", "\\s+(?<threads>\\d*)\\sthreads and\\s*(?<connections>\\d*)\\s+connections"); }

    public Exp testConfig() { return new Exp("config", "Running (?<duration>\\d*)(?<durationUnit>\\w*)\\stest @ (?<url>.*)"); }

    public Exp reqSec() {
        return new Exp("reqSec", "Requests/sec:\\s+(?<reqSec>\\d*\\.\\d*)");
    }

    public Exp latency() { return new Exp("latency", "\\s*Latency\\s*(?<meanLatency>\\d*\\.\\d*)(?<meanLatencyUnit>\\w*)\\s*(?<stdDevLatency>\\d*\\.\\d*)(?<stdDevLatencyUnit>\\w*)\\s*(?<maxLatency>\\d*\\.\\d*)(?<maxLatencyUnit>\\w*)\\s*(?<stdDevPercent>\\d*\\.\\d*)"); }

    public Exp latencyDistribution() { return new Exp("latencyDistribution", "^\\s*(?<centile>\\d*\\.\\d*)%\\s*(?<latencyVal>\\d*\\.\\d*)(?<latencyUnit>\\w*)\\s*.*"); }

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
        p.add(latencyDistribution().nest("latencyDistribution").setMerge(ExpMerge.AsEntry));
    }
}
