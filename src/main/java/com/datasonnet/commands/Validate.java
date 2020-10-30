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
import com.datasonnet.MapperBuilder;
import picocli.CommandLine;

import java.io.File;
import java.util.concurrent.Callable;

@CommandLine.Command(
        name = "validate",
        description = "Validate a DataSonnet map"
)
public class Validate implements Callable<Void> {

    @CommandLine.Parameters(
            index = "0",
            description = "Map file"
    )
    private File datasonnet;

    @CommandLine.Option(names = {"-i", "--includes-function"})
    boolean includesFunction;


    @Override
    public Void call() throws Exception {
        Mapper mapper = new MapperBuilder(Main.readFile(datasonnet))
                .wrapAsFunction(!includesFunction)
                .build();
        System.out.println("Validates!");
        return null;
    }
}
