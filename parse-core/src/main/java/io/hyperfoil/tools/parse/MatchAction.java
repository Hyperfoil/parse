package io.hyperfoil.tools.parse;

import io.hyperfoil.tools.yaup.json.Json;

/**
 * An action that runs after an Exp matches part of a line. Implementations are either Java or make use of JsMatchAction to define actions in yaml.
 *
 */
public interface MatchAction {

    void onMatch(String line, Json match, Exp pattern, Parser parser);
}
