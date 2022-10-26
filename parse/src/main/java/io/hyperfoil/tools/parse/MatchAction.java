package io.hyperfoil.tools.parse;

import io.hyperfoil.tools.yaup.json.Json;

/**
 *
 */
public interface MatchAction {

    void onMatch(String line, Json match, Exp pattern, Parser parser);
}
