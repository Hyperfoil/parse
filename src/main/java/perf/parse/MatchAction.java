package perf.parse;

import perf.yaup.json.Json;

/**
 *
 */
public interface MatchAction {

    void onMatch(Json match, Exp pattern, Parser parser);
}
