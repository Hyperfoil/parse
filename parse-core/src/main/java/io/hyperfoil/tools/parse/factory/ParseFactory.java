package io.hyperfoil.tools.parse.factory;

import io.hyperfoil.tools.parse.Parser;

public interface ParseFactory {

    void addToParser(Parser p);
    Parser newParser();
}
