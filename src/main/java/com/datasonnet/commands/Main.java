package com.datasonnet.commands;

import picocli.CommandLine;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

@CommandLine.Command(
        subcommands = {
                Run.class,
                Validate.class
        },
        mixinStandardHelpOptions = true
)
public class Main implements Runnable {
    public static void main(String[] args) {
        int code = new CommandLine(new Main()).execute(args);
        System.exit(code);
    }

    public void run() {
        System.err.println("To execute a map, use the `run` subcommand. To see all available subcommands, pass `-h`");
    }

    static String readFile(File file) throws IOException {
        return Files.lines(file.toPath()).collect(Collectors.joining());
    }
}
