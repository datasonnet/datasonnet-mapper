package com.datasonnet.header;

import java.io.StringReader;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Pattern;

import com.fasterxml.jackson.dataformat.javaprop.*;
import com.fasterxml.jackson.dataformat.javaprop.util.JPropNode;
import com.fasterxml.jackson.dataformat.javaprop.util.JPropPathSplitter;

public class Header {
    public static String DATASONNET_HEADER = "/** DataSonnet";
    public static String DATASONNET_VERSION = "version";
    public static String DATASONNET_OUTPUT = "output";
    public static String DATASONNET_INPUT = "input";

    public static String DATAFORMAT_PREFIX = "dataformat";
    public static String DATAFORMAT_DEFAULT = "*";


    private String version = "1.0";
    private  Map dataFormatParameters = Collections.emptyMap();

    public static Header parseHeader(String dataSonnetDocument) throws HeaderParseException {
        Header header = new Header();

        if (dataSonnetDocument.trim().startsWith(DATASONNET_HEADER)) {
            String headerCommentSection = dataSonnetDocument.substring(0, dataSonnetDocument.indexOf("*/"));
            headerCommentSection = headerCommentSection.replace(DATASONNET_HEADER, "").replace("*/","");

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
                        String[] segments = key.split("(?<!" + Pattern.quote("\\") + ")" + Pattern.quote("" + _pathSeparatorChar));
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
                header.setDataFormatParameters(propsMap);
            } catch (Exception e) {
                throw new HeaderParseException("Error parsing DataSonnet Header: ", e);
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

    public Map getDataFormatParameters() {
        return dataFormatParameters;
    }

    public void setDataFormatParameters(Map dataFormatParameters) {
        this.dataFormatParameters = dataFormatParameters;
    }

    public Map getDataFormatParameters(String name, String mimeType, boolean isInput) {
        Map defaultParams = (Map)dataFormatParameters.getOrDefault(Header.DATAFORMAT_DEFAULT, Collections.emptyMap());
        Map mergedParams = (Map)defaultParams.getOrDefault(mimeType, new HashMap());

        if (isInput) {
            Map input = (Map)dataFormatParameters.getOrDefault(Header.DATASONNET_INPUT, Collections.emptyMap());
            Map defaultInput = (Map)input.getOrDefault(Header.DATAFORMAT_DEFAULT, Collections.emptyMap());
            Map mimeTypeParams = (Map)defaultInput.getOrDefault(mimeType, Collections.emptyMap());
            mergedParams.putAll(mimeTypeParams);

            Map paramInput = (Map)input.getOrDefault(name, Collections.emptyMap());
            mimeTypeParams = (Map)paramInput.getOrDefault(mimeType, Collections.emptyMap());
            mergedParams.putAll(mimeTypeParams);
        } else {
            Map output = (Map)dataFormatParameters.getOrDefault(Header.DATASONNET_OUTPUT, Collections.emptyMap());
            Map mimeTypeParams = (Map)output.getOrDefault(mimeType, Collections.emptyMap());
            mergedParams.putAll(mimeTypeParams);
        }

        return mergedParams;
    }
}
