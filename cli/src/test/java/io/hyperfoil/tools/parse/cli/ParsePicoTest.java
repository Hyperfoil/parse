package io.hyperfoil.tools.parse.cli;

import io.quarkus.test.junit.main.LaunchResult;
import io.quarkus.test.junit.main.QuarkusMainLauncher;
import io.quarkus.test.junit.main.QuarkusMainTest;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusMainTest
public class ParsePicoTest {

    /**
     * No arg execution should be an error that displays the usage hint
     * @param launcher
     */
    @Test
    public void no_arg_help(QuarkusMainLauncher launcher) {
        LaunchResult result = launcher.launch();
        assertNotEquals(0,result.exitCode());
        assertTrue(result.getErrorOutput().contains("Usage:"),result.getErrorOutput());
    }

    @Test
    public void asContent_path(QuarkusMainLauncher launcher) throws IOException {
        Path sourcePath = Files.writeString(File.createTempFile("parse",".txt").toPath(),
                """
                found
                """);
        sourcePath.toFile().deleteOnExit();
        Path configPath = Files.writeString(File.createTempFile("parse",".yaml").toPath(),
                """
                ---
                name: jsonpath
                path: parse.*.txt
                asContent: nest
                """);
        configPath.toFile().deleteOnExit();
        File destination = File.createTempFile("parse",".json");
        destination.deleteOnExit();
        LaunchResult result = launcher.launch("--disableDefaults","-s",sourcePath.toString(),"-r",configPath.toString(),"-d",destination.getPath());
        assertEquals(0,result.exitCode());
        assertTrue(destination.exists(),"output file should be created");
        String created = Files.readString(destination.toPath());
        assertTrue(created.contains("nest"),"result should contain the first nest key\n"+created);
        assertTrue(created.contains("found"),"result should contain the first nest value\n"+created);
    }

    @Test
    public void asXml_path(QuarkusMainLauncher launcher) throws IOException {
        Path sourcePath = Files.writeString(File.createTempFile("parse",".xml").toPath(),
                """
                <?xml version="1.0" encoding="UTF-8"?>
                <foo>
                    <bar>uno</bar>
                    <biz>dos</biz>
                </foo>
                """);
        sourcePath.toFile().deleteOnExit();
        Path configPath = Files.writeString(File.createTempFile("parse",".yaml").toPath(),
                """
                ---
                name: jsonpath
                path: parse.*.xml
                asXml:
                - path: $.foo.bar['text()']
                  nest: one
                - path: $.foo.biz['text()']
                  nest: two
                """);
        configPath.toFile().deleteOnExit();
        File destination = File.createTempFile("parse",".json");
        destination.deleteOnExit();
        LaunchResult result = launcher.launch("--disableDefaults","-s",sourcePath.toString(),"-r",configPath.toString(),"-d",destination.getPath());
        assertEquals(0,result.exitCode());
        assertTrue(destination.exists(),"output file should be created");
        String created = Files.readString(destination.toPath());
        assertTrue(created.contains("one"),"result should contain the first nest key\n"+created);
        assertTrue(created.contains("uno"),"result should contain the first nest value\n"+created);
        assertTrue(created.contains("two"),"result should contain the second nest key\n"+created);
        assertTrue(created.contains("dos"),"result should contain the second nest value\n"+created);
    }
    @Test
    public void asJson_function(QuarkusMainLauncher launcher) throws IOException {
        Path sourcePath = Files.writeString(File.createTempFile("parse",".json").toPath(),
                """
                {
                    "foo": "uno",
                    "bar": "dos"
                }
                """);
        sourcePath.toFile().deleteOnExit();
        Path configPath = Files.writeString(File.createTempFile("parse",".yaml").toPath(),
                """
                ---
                name: jsonpath
                path: parse.*.json
                asJson: |
                  (blob)=>{
                    const {foo, bar } = blob;
                    return {merged: foo+bar}
                  }
                """);
        configPath.toFile().deleteOnExit();
        File destination = File.createTempFile("parse",".json");
        destination.deleteOnExit();
        LaunchResult result = launcher.launch("--disableDefaults","-s",sourcePath.toString(),"-r",configPath.toString(),"-d",destination.getPath());
        assertEquals(0,result.exitCode());
        assertTrue(destination.exists(),"output file should be created");
        String created = Files.readString(destination.toPath());
        assertTrue(created.contains("merged"),"resulting json should contain 'merged' key\n"+created);
        assertTrue(created.contains("unodos"),"resulting json should contain 'unodos'\n"+created);
    }

    /***
     * Command line state parameters should be available to applicable state references
     * @param launcher
     * @throws IOException
     */
    @Test
    public void multiple_state_parameters_in_with(QuarkusMainLauncher launcher) throws IOException {

        Path sourcePath = Files.writeString(File.createTempFile("foo",".txt").toPath(),
                """
                the content should not matter
                """);
        sourcePath.toFile().deleteOnExit();
        Path configPath = Files.writeString(File.createTempFile("parse",".yaml").toPath(),
                """
                ---
                name: showState
                path: foo.*.txt
                asText:
                - name: withState
                  pattern: .*
                  with:
                    foo: ${{foo}}
                    biz: ${{biz}}
                """);
        configPath.toFile().deleteOnExit();
        File destination = File.createTempFile("parse",".json");
        destination.deleteOnExit();
        LaunchResult result = launcher.launch("-S","foo=bar","-S","biz=buz","-s",sourcePath.toString(),"-r",configPath.toString(),"-d",destination.getPath());
        assertEquals(0,result.exitCode());
        assertTrue(destination.exists(),"output file should be created");
        String created = Files.readString(destination.toPath());
        assertTrue(created.contains("bar"),"output file should contain state from with reference:\n"+created);
        assertTrue(created.contains("buz"),"output file should contain state from with reference:\n"+created);
    }
    @Test
    public void not_match(QuarkusMainLauncher launcher) throws IOException {
        Path sourcePath = Files.writeString(File.createTempFile("foo",".txt").toPath(),
                """
                the content should not matter
                """);
        sourcePath.toFile().deleteOnExit();
        Path configPath = Files.writeString(File.createTempFile("parse",".yaml").toPath(),
                """
                ---
                name: showState
                path: miss.log
                asText:
                - name: withState
                  pattern: .*
                  with:
                    foo: WTF
                """);
        configPath.toFile().deleteOnExit();
        File destination = File.createTempFile("parse",".json");
        destination.deleteOnExit();
        LaunchResult result = launcher.launch("--disableDefaults","-s",sourcePath.toString(),"-r",configPath.toString(),"-d",destination.getPath());
        assertEquals(0,result.exitCode());
        assertTrue(destination.exists(),"output file should be created");
        String created = Files.readString(destination.toPath());
        assertFalse(created.contains("WTF"),"rule should not populate\n"+created);
    }


}
