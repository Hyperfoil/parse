package perf.parse;

import perf.yaup.json.Json;

/**
 *
 */
public interface MatchAction {

    void onMatch(String line,Json match, Exp pattern, Parser parser);
}
