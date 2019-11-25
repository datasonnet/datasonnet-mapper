package com.datasonnet;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.dataformat.javaprop.*;

public class Header {
    public static String DATASONNET_HEADER = "/** DataSonnet";
    public static String DATASONNET_VERSION = "version";
    public static String DATASONNET_OUTPUT = "output";

    public static String DATAFORMAT_PREFIX = "dataformat";
    public static String DATAFORMAT_DEFAULT = "*";


    private String version = "1.0";
    private Map<String, Map<String, Map<String, Object>>> dataFormatParameters = new HashMap<>();

    public static Header parseHeader(String dataSonnetDocument) {
        Header header = new Header();

        if (dataSonnetDocument.trim().startsWith(DATASONNET_HEADER)) {
            String headerCommentSection = dataSonnetDocument.substring(0, dataSonnetDocument.indexOf("*/"));
            headerCommentSection = headerCommentSection.replace(DATASONNET_HEADER, "").replace("*/","");

            JavaPropsMapper mapper = new JavaPropsMapper();
            try {
                Map propsMap = mapper.readValue(headerCommentSection, Map.class);
                if (propsMap.containsKey(DATASONNET_VERSION)) {
                    header.setVersion((String)propsMap.get(DATASONNET_VERSION));
                }
                if (propsMap.containsKey(DATAFORMAT_PREFIX)) {
                    header.setDataFormatParameters((Map)propsMap.get(DATAFORMAT_PREFIX));
                }
            } catch (Exception e) {
                e.printStackTrace();
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

    public Map<String, Map<String, Map<String, Object>>> getDataFormatParameters() {
        return dataFormatParameters;
    }

    public void setDataFormatParameters(Map<String, Map<String, Map<String, Object>>> dataFormatParameters) {
        this.dataFormatParameters = dataFormatParameters;
    }
}
