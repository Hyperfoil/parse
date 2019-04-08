package perf.parse.file;

import org.junit.Ignore;
import org.junit.Test;
import perf.yaup.json.Json;

public class MatchCriteriaTest {


    @Test @Ignore
    public void path_only(){
        MatchCriteria matchCriteria = new MatchCriteria()
            .setPathPattern(".*?/run/(?<serverName>[^\\./]+).*?\\.gclog?");

        boolean matched = false;
        Json state = new Json();
        matched = matchCriteria.match("/tmp/867.zip#archive/run/benchserver4.perf.lab.eng.rdu.redhat.com/log/server_20190213_171157.gclog",state);

    }
}
