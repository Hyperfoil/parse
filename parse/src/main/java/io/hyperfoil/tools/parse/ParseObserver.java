package io.hyperfoil.tools.parse;

import io.hyperfoil.tools.yaup.json.Json;

import java.util.List;

public interface ParseObserver {

    public static ParseObserver NO_OP = new ParseObserver(){

        @Override
        public void onLineStart(String line) {}

        @Override
        public void onExpStart(String name, String parentName) {}

        @Override
        public void onExpStop(String name, String parentName) {}

        @Override
        public void onMatch(String name) {}

        @Override
        public void onMiss(String name) {}
    };

    void onLineStart(String line);
    void onExpStart(String name,String parentName);
    void onExpStop(String name,String parentName);

    void onMatch(String name);
    void onMiss(String name);



}
