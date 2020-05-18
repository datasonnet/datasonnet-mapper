package com.datasonnet.commands;

import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import static org.junit.jupiter.api.Assertions.*;

public class RunCommandTest {

    @Test
    void handlesFilesProperly() {
        Run run = new Run();
        new CommandLine(run).parseArgs("-f", "name=data.xml", "map.ds", "payload.json");
        assertEquals("payload.json", run.input.getName());
        assertEquals("data.xml", run.argumentFiles.get("name").getName());
    }
}
