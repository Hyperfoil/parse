package perf.parse;

import perf.yaup.json.Json;

/**
 *
 */
public interface MatchAction {

    void onMatch(String line, Json match, ExpOld pattern, Parser parser);
}
