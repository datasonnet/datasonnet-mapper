package com.datasonnet;

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

import com.datasonnet.document.MediaType;
import com.datasonnet.document.MediaTypes;
import com.datasonnet.spi.DataFormatPlugin;
import com.datasonnet.spi.DataFormatService;
import com.datasonnet.spi.Library;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

public class MapperBuilder {
    private final String script;
    private Iterable<String> inputNames = Collections.emptySet();
    private Map<String, String> imports = Collections.emptyMap();
    private List<Library> libs = Collections.emptyList();
    private DataFormatService service = DataFormatService.DEFAULT;
    private boolean asFunction = true;
    private MediaType defaultOutput = MediaTypes.APPLICATION_JSON;

    public MapperBuilder(String script) {
        this.script = script;
    }

    // TODO: 8/11/20 defensively copy all collections and check for nulls?
    public MapperBuilder withInputNames(Iterable<String> inputNames) {
        Objects.requireNonNull(inputNames);

        this.inputNames = inputNames;
        return this;
    }

    public MapperBuilder withInputNames(String... inputNames) {
        this.inputNames = Arrays.asList(inputNames);
        return this;
    }

    public MapperBuilder withInputNamesFrom(Map<String, String> imports) {
        this.inputNames = imports.keySet();
        return this;
    }

    public MapperBuilder withImports(Map<String, String> imports) {
        Objects.requireNonNull(imports);

        this.imports = imports;
        return this;
    }

    public MapperBuilder withLibrary(Library lib) {
        Objects.requireNonNull(lib);
        if (libs.isEmpty()) {
            libs = new ArrayList<>(2);
        }
        libs.add(lib);

        return this;
    }

    public MapperBuilder wrapAsFunction(boolean asFunction) {
        this.asFunction = asFunction;
        return this;
    }

    public MapperBuilder configurePlugins(Consumer<List<DataFormatPlugin>> configurer) {
        List<DataFormatPlugin> plugins = new ArrayList<>(4);
        configurer.accept(plugins);
        this.service = new DataFormatService(plugins);
        return this;
    }

    public MapperBuilder extendPlugins(Consumer<List<DataFormatPlugin>> extender) {
        List<DataFormatPlugin> plugins = new ArrayList<>(DataFormatService.DEFAULT.getPlugins());
        extender.accept(plugins);
        this.service = new DataFormatService(plugins);
        return this;
    }

    public MapperBuilder withDefaultOutput(MediaType output) {
        this.defaultOutput = output;
        return this;
    }

    public Mapper build() {
        return new Mapper(script, inputNames, imports, asFunction, libs, service, defaultOutput);
    }
}
