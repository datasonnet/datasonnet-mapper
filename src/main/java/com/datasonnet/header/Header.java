package com.datasonnet.header;

import com.fasterxml.jackson.dataformat.javaprop.JavaPropsMapper;
import com.fasterxml.jackson.dataformat.javaprop.JavaPropsSchema;
import com.fasterxml.jackson.dataformat.javaprop.util.JPropNode;
import com.fasterxml.jackson.dataformat.javaprop.util.JPropPathSplitter;

import java.io.IOException;
import java.io.StringReader;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class Header {
    public static String DATASONNET_HEADER = "/** DataSonnet";
    public static String DATASONNET_VERSION = "version";
    public static String DATASONNET_OUTPUT = "output";
    public static String DATASONNET_INPUT = "input";
    public static String DATASONNET_OUTPUT_PRESERVE_ORDER = "preserveOrder";

    public static String DATAFORMAT_PREFIX = "dataformat";
    public static String DATAFORMAT_DEFAULT = "*";

    private String version = "1.0";
    private Map dataFormatParameters = Collections.emptyMap();

    public static Header parseHeader(String dataSonnetDocument) throws HeaderParseException {
        Header header = new Header();

        if (dataSonnetDocument.trim().startsWith(DATASONNET_HEADER)) {
            String headerCommentSection = dataSonnetDocument.substring(0, dataSonnetDocument.indexOf("*/"));
            headerCommentSection = headerCommentSection.replace(DATASONNET_HEADER, "").replace("*/","");

            //Should not contain single backslash char at the end of the line
            headerCommentSection = headerCommentSection.replace("=\\\n", "=\\\\\n");

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
                props.load(new StringReader(headerCommentSection));
                Map propsMap = mapper.readPropertiesAs(props, schema, Map.class);
                if (propsMap.containsKey(DATASONNET_VERSION)) {
                    header.setVersion((String)propsMap.get(DATASONNET_VERSION));
                }
                header.dataFormatParameters = propsMap;
            } catch (IOException|IllegalArgumentException e) {
                throw new HeaderParseException("Error parsing DataSonnet Header: " + e.getMessage(), e);
            }
        }

        return header;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public boolean isPreserveOrder() {
        Map output = getOrEmpty(dataFormatParameters, Header.DATASONNET_OUTPUT);
        boolean preserveOrder = new Boolean(output.getOrDefault(Header.DATASONNET_OUTPUT_PRESERVE_ORDER, "true").toString());
        return preserveOrder;
    }

    public Map<String, Object> getDefaultParameters(String mimeType) {
        Map defaultParams = getOrEmpty(dataFormatParameters, Header.DATAFORMAT_DEFAULT);
        Map defMimeTypeParams = getOrEmpty(defaultParams, mimeType);
        return Collections.unmodifiableMap(defMimeTypeParams);
    }

    public Map<String, Object> getInputParameters(String name, String mimeType) {
        Map input = getOrEmpty(dataFormatParameters, Header.DATASONNET_INPUT);
        Map defaultInput = getOrEmpty(input, Header.DATAFORMAT_DEFAULT);
        Map defaultInputMimeTypeParams = getOrEmpty(defaultInput, mimeType);

        Map paramInput = getOrEmpty(input, name);
        Map mimeTypeParams = getOrEmpty(paramInput, mimeType);

        return overlay(overlay(getDefaultParameters(mimeType), defaultInputMimeTypeParams),
                       mimeTypeParams);
    }

    public Map<String, Object> getOutputParameters(String mimeType) {
        Map output = getOrEmpty(dataFormatParameters, Header.DATASONNET_OUTPUT);
        Map mimeTypeParams = getOrEmpty(output, mimeType);

        return overlay(getDefaultParameters(mimeType), mimeTypeParams);
    }

    private Map<String, Object> getOrEmpty(Map<String, Object> map, String key) {
        return (Map)map.getOrDefault(key, Collections.emptyMap());
    }
    private Map<String, Object> overlay(Map<String, Object> defaults, Map<String, Object> overlay) {
        Map<String, Object> output = new HashMap(defaults);
        output.putAll(overlay);
        return output;
    }
}
