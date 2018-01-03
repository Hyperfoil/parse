package perf.parse;

import org.json.JSONObject;
/**
 *
 */
public interface MatchAction {

    void onMatch(JSONObject match, Exp pattern, Parser parser);
}
