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

import picocli.CommandLine;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
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
