package com.datasonnet;

import com.datasonnet.spi.DataFormatPlugin;
import com.datasonnet.spi.DataFormatService;
import com.datasonnet.spi.Library;
import sjsonnet.Val;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

public class MapperBuilder {
    private final String script;
    private Iterable<String> inputNames = Collections.emptySet();
    private Map<String, String> imports = Collections.emptyMap();
    private List<Library> libs = Collections.emptyList();
    private DataFormatService service = DataFormatService.DEFAULT;
    private boolean asFunction = true;

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

    public MapperBuilder withImports(Map<String, String> imports) {
        Objects.requireNonNull(imports);

        this.imports = imports;
        return this;
    }

    public MapperBuilder addLibrary(Library lib) {
        Objects.requireNonNull(lib);
        if (libs.isEmpty()) {
            libs = new ArrayList<>(2);
        }
        libs.add(lib);

        return this;
    }

    public MapperBuilder shouldWrapAsFunction(boolean asFunction) {
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

    public Mapper build() {
        return new Mapper(script, inputNames, imports, asFunction, libs, service);
    }
}
