package com.datasonnet.commands;

import com.datasonnet.Document;
import com.datasonnet.Mapper;
import com.datasonnet.StringDocument;
import com.datasonnet.spi.DataFormatPlugin;
import com.datasonnet.spi.DataFormatService;
import picocli.CommandLine;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

@CommandLine.Command(
        name = "run",
        description = "Transform data using DataSonnet",
        footer = {"Available input and output formats are JSON, XML, and CSV"}
)
public class Run implements Callable<Void> {

    @CommandLine.Parameters(
            index = "0",
            description = "Map file (mime-type is autodetected by suffix, defaulting to JSON)"
    )
    File datasonnet;

    @CommandLine.Parameters(
            index = "1",
            arity = "0..1",
            description = "Input data file (if omitted reads from standard input)"
    )
    File input;

    @CommandLine.Option(names = {"-a", "--argument"}, split = ",", description = "argument name and value (as JSON)")
    Map<String, String> arguments = new HashMap<>();

    @CommandLine.Option(names = {"-f", "--argument-file"}, split = ",", description = "argument name and file containing the value (mime-type is autodetected by suffix, defaulting to JSON)")
    Map<String, File> argumentFiles = new HashMap<>();

    @CommandLine.Option(names = {"-i", "--import-file"}, description = "file to make available for imports")
    List<File> importFiles = new ArrayList<>();

    @CommandLine.Option(names = {"-n", "--no-wrap"}, description = "Do not wrap in a function call. Only use this if your transformation is already a top-level function.")
    boolean alreadyWrapped;

    @CommandLine.Option(names = {"-o", "--output-type"}, description = "Handle the output as this format. Defaults to JSON.")
    String outputType = "application/json";

    @Override
    public Void call() throws Exception {
        Mapper mapper = new Mapper(Main.readFile(datasonnet), combinedArguments().keySet(), imports(), !alreadyWrapped);
        Document result = mapper.transform(new StringDocument(payload(), suffix(datasonnet)), combinedArguments(), outputType);
        System.out.println(result.contents());
        return null;
    }

    private String suffix(File file) {
        String[] parts = file.getName().split(".");
        if(parts.length > 1) {
            return parts[parts.length - 1];
        } else {
            return "";  // no suffix
        }
    }

    private String payload() throws IOException {
        if(input == null) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
            StringBuilder contents = new StringBuilder();
            int current;
            while((current = reader.read()) != -1) {
                contents.appendCodePoint(current);
            }
            return contents.toString();
        } else {
            return Main.readFile(input);
        }
    }

    private Map<String, Document> combinedArguments() throws IOException {
        return new HashMap<String, Document>() {{
            for(Map.Entry<String, String> entry : arguments.entrySet()) {
                put(entry.getKey(), new StringDocument(entry.getValue(), "application/json"));
            }
            for(Map.Entry<String, File> entry : argumentFiles.entrySet()) {
                File file = entry.getValue();
                String contents = Main.readFile(file);
                put(entry.getKey(), new StringDocument(contents, suffix(file)));
            }
        }};
    }


    private Map<String, String> imports() throws IOException {
        Map imports = new HashMap<>();
        for(File importFile : importFiles) {
            String name = importFile.getPath();
            String contents = Main.readFile(importFile);
            imports.put(name, contents);
        }
        return imports;
    }

}
