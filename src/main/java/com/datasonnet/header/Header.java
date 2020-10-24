package com.datasonnet.header;

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

import com.datasonnet.document.Document;
import com.datasonnet.document.InvalidMediaTypeException;
import com.datasonnet.document.MediaType;
import com.datasonnet.document.MediaTypes;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.dataformat.javaprop.JavaPropsMapper;
import com.fasterxml.jackson.dataformat.javaprop.JavaPropsSchema;
import com.fasterxml.jackson.dataformat.javaprop.util.JPropNode;
import com.fasterxml.jackson.dataformat.javaprop.util.JPropPathSplitter;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.StringReader;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class Header {
    public static final String DATASONNET_HEADER = "/** DataSonnet";
    public static final Pattern VERSION_LINE = Pattern.compile("^version *= *(?<version>[a-zA-Z0-9.+-]+) *(\\r?\\n|$)");
    public static final String DATASONNET_INPUT = "input";
    public static final String DATASONNET_OUTPUT = "output";
    public static final String DATASONNET_PRESERVE_ORDER = "preserveOrder";
    public static final String DATAFORMAT_PREFIX = "dataformat";
    public static final String DATAFORMAT_DEFAULT = "*";
    public static final String VERSION_2_0 = "2.0";
    public static final String VERSION_1_0 = "1.0";
    private final String version;
    private final boolean preserveOrder;
    private final Map<String, Map<Integer, MediaType>> namedInputs;
    private final Map<Integer, MediaType> output;
    // using maps to facilitate only one per super/sub type
    private final Map<Integer, MediaType> allInputs;
    private final Map<Integer, MediaType> dataFormats;

    public Header(String version,
                  boolean preserveOrder,
                  Map<String, Iterable<MediaType>> namedInputs,
                  Iterable<MediaType> output,
                  Iterable<MediaType> allInputs,
                  Iterable<MediaType> dataFormats) {
        this.version = version;
        this.preserveOrder = preserveOrder;
        this.namedInputs = new HashMap<>();
        for(Map.Entry<String, Iterable<MediaType>> entry : namedInputs.entrySet()) {
            this.namedInputs.put(entry.getKey(), indexMediaTypes(entry.getValue()));
        }
        this.output = indexMediaTypes(output);
        this.allInputs = indexMediaTypes(allInputs);
        this.dataFormats = indexMediaTypes(dataFormats);
    }

    private Map<Integer, MediaType> indexMediaTypes(Iterable<MediaType> mediaTypes) {
        Map<Integer, MediaType> indexed = new HashMap<>();
        for(MediaType mediaType : mediaTypes) {
            indexed.put(calculateIndex(mediaType), mediaType);
        }
        return indexed;
    }

    private Integer calculateIndex(MediaType mediaType) {
        return mediaType.getType().hashCode() + mediaType.getSubtype().hashCode();
    }


    private static final Header EMPTY =
            new Header(VERSION_2_0, true, Collections.emptyMap(), Collections.emptyList(), Collections.emptyList(), Collections.emptyList());

    public static Header parseHeader(String script) throws HeaderParseException {
        if (!script.trim().startsWith(DATASONNET_HEADER)) {
            return EMPTY;
        }

        String headerSection = extractHeader(script);

        Matcher versionMatcher = VERSION_LINE.matcher(headerSection);
        if(!versionMatcher.find()) {
            throw new HeaderParseException("The first line of the header must be a version line, but is not");
        }

        String version = versionMatcher.group("version");
        String headerWithoutVersion = headerSection.substring(versionMatcher.end());

        if(VERSION_1_0.equals(version)) {
            return parseHeader10(headerWithoutVersion);
        } else if(VERSION_2_0.equals(version)) {
            return parseHeader20(headerWithoutVersion);
        } else {
            throw new HeaderParseException("Version must be one of 1.0 or 2.0 but is " + version);
        }
    }

    @NotNull
    private static String extractHeader(String script) throws HeaderParseException {
        int terminus = script.indexOf("*/");
        if(terminus == -1) {
            throw new HeaderParseException("Unterminated header. Headers must end with */");
        }

        String headerSection = script
                .substring(0, terminus)
                .replace(DATASONNET_HEADER, "")
                .trim();
        return headerSection;
    }

    @NotNull
    private static Header parseHeader10(String headerSection) throws HeaderParseException {
        JavaPropsMapper mapper = new JavaPropsMapper();
        JavaPropsSchema schema = new JavaPropsSchema() {
            class HeaderSplitter extends JPropPathSplitter
            {
                protected final char _pathSeparatorChar = '.';

                public HeaderSplitter()
                {
                    super(true);
                }

                @Override
                public JPropNode splitAndAdd(JPropNode parent,
                                             String key, String value)
                {
                    JPropNode curr = parent;
                    // split on the path separator character not preceded by a backslash
                    String[] segments = key.split("(?<!\\\\)" + Pattern.quote("" + _pathSeparatorChar));
                    for (String segment : segments) {
                        curr = _addSegment(curr, segment.replaceAll("\\\\", ""));
                    }
                    return curr.setValue(value);
                }
            }
            @Override
            public JPropPathSplitter pathSplitter() {
                return new HeaderSplitter();
            };
        };
        try {
            Properties props = new Properties();
            props.load(new StringReader(headerSection));
            Map propsMap = mapper.readPropertiesAs(props, schema, Map.class);


            Map<String, Map<String, Map<String, String>>> originalInputs = getOrEmpty(propsMap, "input");
            Map<String, Iterable<MediaType>> inputs = new HashMap<>();
            for(Map.Entry<String, Map<String, Map<String, String>>> entry : originalInputs.entrySet()) {
                if(!entry.getKey().equals("*")) {
                    List<MediaType> mediaTypes = extractMediaTypes(entry.getValue());
                    inputs.put(entry.getKey(), mediaTypes);
                }
            }
            Iterable<MediaType> allInputs = extractMediaTypes(getOrEmpty(originalInputs, "*"));
            Iterable<MediaType> output = extractMediaTypes(getOrEmpty(propsMap, "output"));
            Iterable<MediaType> dataFormat = extractMediaTypes(getOrEmpty(propsMap, "dataformat"));


            return new Header(VERSION_1_0,
                    getBoolean(propsMap,DATASONNET_PRESERVE_ORDER, true),
                    inputs,
                    output,
                    allInputs,
                    dataFormat);
        } catch (IOException|IllegalArgumentException exc) {
            throw new HeaderParseException("Error parsing DataSonnet Header: " + exc.getMessage(), exc);
        } catch (ClassCastException exc) {
            throw new HeaderParseException("Error parsing DataSonnet Header, make sure type parameters are nested properly");
        }
    }

    private static MediaType extractMediaType(String type, Map<String, String> params) {
        return new MediaType(MediaType.valueOf(type), params);
    }

    private static List<MediaType> extractMediaTypes(Map<String, Map<String, String>> originals) {
        List<MediaType> types = new ArrayList<>();
        for(Map.Entry<String, Map<String, String>> entry : originals.entrySet()) {
            types.add(extractMediaType(entry.getKey(), entry.getValue()));
        }
        return types;
    }

    private static <T> Map<String, T> getOrEmpty(Map map, String key) {
        return (Map)map.getOrDefault(key, Collections.emptyMap());
    }

    private static boolean getBoolean(Map propsMap, String key, boolean defaultTo) {
        if(propsMap.containsKey(key)) {
            return Boolean.parseBoolean(propsMap.get(key).toString());
        } else {
            return defaultTo;
        }
    }

    @NotNull
    private static Header parseHeader20(String headerSection) throws HeaderParseException {
        boolean preserve = true;
        List<MediaType> output = new ArrayList<>(4);
        Map<String, List<MediaType>> inputs = new HashMap<>(4);
        List<MediaType> allInputs = new ArrayList<>(4);
        List<MediaType> dataformat = new ArrayList<>(4);

        for (String line : headerSection.split("\\r?\\n")) {
            try {
                if (line.startsWith(DATASONNET_PRESERVE_ORDER)) {
                    String[] tokens = line.split("=", 2);
                    preserve = Boolean.parseBoolean(tokens[1]);
                } else if (line.startsWith(DATASONNET_INPUT)) {
                    String[] tokens = line.split(" ", 3);
                    if (DATAFORMAT_DEFAULT.equals(tokens[1])) {
                        MediaType toAdd = MediaType.valueOf(tokens[2]);
                        allInputs.add(toAdd);
                    } else {
                        if(!inputs.containsKey(tokens[1])) {
                            inputs.put(tokens[1], new ArrayList<>());
                        }
                        inputs.get(tokens[1]).add(MediaType.valueOf(tokens[2]));
                    }
                } else if (line.startsWith(DATASONNET_OUTPUT)) {
                    String[] tokens = line.split(" ", 2);
                    output.add(MediaType.valueOf(tokens[1]));
                } else if (line.startsWith(DATAFORMAT_PREFIX)) {
                    String[] tokens = line.split(" ", 2);
                    MediaType toAdd = MediaType.valueOf(tokens[1]);
                    dataformat.add(toAdd);
                } else if (line.trim().isEmpty()) {
                    // this is allowed, and we pass
                } else {
                    throw new HeaderParseException("Unable to parse header line: " + line);
                }
            } catch (InvalidMediaTypeException exc) {
                throw new HeaderParseException("Could not parse media type from header in line " + line, exc);
            } catch(ArrayIndexOutOfBoundsException exc) {
                throw new HeaderParseException("Problem with header formatting in line " + line);
            }
        }

        return new Header(VERSION_2_0, preserve, Collections.unmodifiableMap(inputs), output, allInputs, dataformat);
    }

    public String getVersion() {
        return version;
    }

    public Map<String, Iterable<MediaType>> getNamedInputs() {
        Map<String, Iterable<MediaType>> namedInputs = new HashMap<>(this.namedInputs.size());
        for(Map.Entry<String, Map<Integer, MediaType>> entry : this.namedInputs.entrySet()) {
            namedInputs.put(entry.getKey(), Collections.unmodifiableCollection(entry.getValue().values()));
        }
        return Collections.unmodifiableMap(namedInputs);
    }

    public Collection<MediaType> getOutput() {
        return Collections.unmodifiableCollection(output.values());
    }

    public Collection<MediaType> getPayload() {
        return Collections.unmodifiableCollection(namedInputs.getOrDefault("payload", Collections.emptyMap()).values());
    }

    public Collection<MediaType> getAllInputs() {
        return Collections.unmodifiableCollection(allInputs.values());
    }

    public Collection<MediaType> getDataFormats() {
        return Collections.unmodifiableCollection(dataFormats.values());
    }

    public boolean isPreserveOrder() {
        return preserveOrder;
    }

    public <T> Document<T> combineInputParams(String inputName, Document<T> doc) {
        Map<String, String> params = baseParams();
        MediaType mediaType = doc.getMediaType();
        Integer key = calculateIndex(mediaType);

        if (dataFormats.containsKey(key)) {
            params.putAll(dataFormats.get(key).getParameters());
        }

        if (allInputs.containsKey(key)) {
            params.putAll(allInputs.get(key).getParameters());
        }

        if (namedInputs.containsKey(inputName)) {
            Map<Integer, MediaType> inputTypes = namedInputs.getOrDefault(inputName, Collections.emptyMap());
            if(inputTypes.containsKey(key)) {
                params.putAll(inputTypes.get(key).getParameters());
            }
        }

        // hmmm, I see why this is here, but it feels tricky. Some parameters control
        // what inputs look like after parsing; overriding those with things from the input is bad.
        // other parameters control how the content is parsed; overriding those with things from the input is good.
        // since the former are (deservedly) rarer, leaving this here is probably best, but we may want
        // something more sophisticated later.
        params.putAll(mediaType.getParameters());

        return doc.withMediaType(new MediaType(mediaType, params));
    }

    private Map<String, String> baseParams() {
        Map<String, String> params = new HashMap<>(4);
        params.put(MediaTypeParameters.VERSION, getVersion());
        return params;
    }

    public MediaType combineOutputParams(MediaType mediaType) {
        Map<String, String> params = baseParams();
        Integer key = calculateIndex(mediaType);

        if (dataFormats.containsKey(key)) {
            params.putAll(dataFormats.get(key).getParameters());
        }

        if (output.containsKey(key)) {
            params.putAll(output.get(key).getParameters());
        }

        params.putAll(mediaType.getParameters());

        return new MediaType(mediaType, params);
    }

    public static class MediaTypeParameters {
        public static final String PREFIX = "ds_";
        public static final String VERSION = "ds_version";
    }
}
