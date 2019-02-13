package perf.parse.factory;

import perf.parse.Parser;

public interface ParseFactory {

    void addToParser(Parser p);
    Parser newParser();
}
