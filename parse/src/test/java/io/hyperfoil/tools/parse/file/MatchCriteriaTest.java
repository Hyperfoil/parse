package io.hyperfoil.tools.parse.file;

import io.hyperfoil.tools.parse.file.MatchCriteria;
import org.junit.Ignore;
import org.junit.Test;
import io.hyperfoil.tools.yaup.json.Json;

import static org.junit.Assert.assertFalse;

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
    public void not_match(){
        MatchCriteria matchCriteria = new MatchCriteria()
                .setPathPattern("foo.log");
        boolean matched = matchCriteria.match("parse.13423234234.json",new Json());
        assertFalse(matched);
    }
}
