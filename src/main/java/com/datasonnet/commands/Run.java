package com.datasonnet.commands;

/*-
 * Copyright 2019-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import com.datasonnet.Mapper;
import com.datasonnet.document.DefaultDocument;
import com.datasonnet.document.Document;
import com.datasonnet.document.MediaType;
import com.datasonnet.document.MediaTypes;
import picocli.CommandLine;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
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
    boolean alreadyWrapped = false;

    @CommandLine.Option(names = {"-o", "--output-type"}, description = "Handle the output as this format. Defaults to JSON.")
    String outputType = "application/json";

    @Override
    public Void call() throws Exception {
        Mapper mapper = new Mapper(Main.readFile(datasonnet), combinedArguments().keySet(), imports(), !alreadyWrapped);
        Document<String> result = mapper
                .transform(new DefaultDocument<>(payload(), MediaTypes.forExtension(suffix(datasonnet)).get()),
                        combinedArguments(), MediaType.valueOf(outputType));
        String contents = result.getContent();
        System.out.println(contents);

        return null;
    }

    private String suffix(File file) {
        String[] parts = file.getName().split(".");
        if (parts.length > 1) {
            return parts[parts.length - 1];
        } else {
            return "";  // no suffix
        }
    }

    private String payload() throws IOException {
        if (input == null) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
            StringBuilder contents = new StringBuilder();
            int current;
            while ((current = reader.read()) != -1) {
                contents.appendCodePoint(current);
            }
            return contents.toString();
        } else {
            return Main.readFile(input);
        }
    }

    private Map<String, Document<?>> combinedArguments() throws IOException {
        return Collections.unmodifiableMap(new HashMap<String, Document<String>>() {{
            for (Map.Entry<String, String> entry : arguments.entrySet()) {
                put(entry.getKey(), new DefaultDocument<>(entry.getValue(), MediaTypes.APPLICATION_JSON));
            }

            for (Map.Entry<String, File> entry : argumentFiles.entrySet()) {
                File file = entry.getValue();
                String contents = Main.readFile(file);
                put(entry.getKey(), new DefaultDocument<>(contents, MediaTypes.forExtension(suffix(file)).get()));
            }
        }});
    }

    private Map<String, String> imports() throws IOException {
        Map<String, String> imports = new HashMap<>();
        for (File importFile : importFiles) {
            String name = importFile.getPath();
            String contents = Main.readFile(importFile);
            imports.put(name, contents);
        }
        return Collections.unmodifiableMap(imports);
    }

}
