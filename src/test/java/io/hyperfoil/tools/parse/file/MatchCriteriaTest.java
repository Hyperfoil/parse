package io.hyperfoil.tools.parse.file;

import io.hyperfoil.tools.parse.file.MatchCriteria;
import org.junit.Ignore;
import org.junit.Test;
import io.hyperfoil.tools.yaup.json.Json;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MatchCriteriaTest {


    @Test @Ignore
    public void path_only(){
        MatchCriteria matchCriteria = new MatchCriteria()
            .setPathPattern(".*?/run/(?<serverName>[^\\./]+).*?\\.gclog?");

        boolean matched = false;
        Json state = new Json();
        matched = matchCriteria.match("/tmp/867.zip#archive/run/benchserver4.perf.lab.eng.rdu.redhat.com/log/server_20190213_171157.gclog",state);
    }

    @Test
    public void csv(){
        MatchCriteria matchCriteria = new MatchCriteria()
                .setPathPattern(".csv");
        boolean matched;
        Json state = new Json();
        matched = matchCriteria.match("test.csv",state);

        assertTrue("test.csv should match",matched);

        matched = matchCriteria.match("csv.yaml",state);
        assertFalse("csv.yaml should not match",matched);
    }
}
