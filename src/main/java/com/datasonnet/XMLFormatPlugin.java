package com.datasonnet;


import com.datasonnet.badgerfish.*;
import com.datasonnet.spi.DataFormatPlugin;
import com.datasonnet.spi.UjsonUtil;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.codehaus.jettison.json.JSONException;
import ujson.Value;

import com.ctc.wstx.api.WstxOutputProperties;
import com.ctc.wstx.api.WstxInputProperties;
import org.codehaus.stax2.XMLInputFactory2;
import org.codehaus.stax2.XMLOutputFactory2;
import org.codehaus.stax2.XMLStreamReader2;
import org.codehaus.stax2.XMLStreamWriter2;
import org.codehaus.stax2.ri.Stax2ReaderAdapter;
import org.codehaus.stax2.ri.Stax2WriterAdapter;

import javax.xml.stream.*;

import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class XMLFormatPlugin implements DataFormatPlugin {
    public static String NAMESPACE_DECLARATIONS = "NamespaceDeclarations";
    public static String NAMESPACE_SEPARATOR = "NamespaceSeparator";
    public static String TEXT_VALUE_KEY = "TextValueKey";
    public static String CDATA_VALUE_KEY = "CdataValueKey";
    public static String ATTRIBUTE_CHARACTER = "AttributeCharacter";

    public static String OMIT_XML_DECLARATION = "OmitXmlDeclaration";
    public static String XML_VERSION = "XmlVersion";
    public static String AUTO_EMPTY_ELEMENTS = "AutoEmptyElements";
    public static String NULL_AS_EMPTY_ELEMENT = "NullAsEmptyElement";
    public static String ENCODING = "Encoding";
    public static String ROOT_ELEMENT = "RootElement";


    public XMLFormatPlugin() {
        System.setProperty("javax.xml.stream.XMLInputFactory", "com.ctc.wstx.stax.WstxInputFactory");
        System.setProperty("javax.xml.stream.XMLOutputFactory", "com.ctc.wstx.stax.WstxOutputFactory");
    }

    public Value read(String inputXML, Map<String, Object> params) throws Exception {
        if(params == null) {
            params = Collections.emptyMap();
        }

        try(Reader input = new StringReader(inputXML);
            StringWriter output = new StringWriter();
            ) {
            XMLStreamReader2 reader = createInputStream(input);
            XMLStreamWriter2 writer = createJSONOutputStream(params, output);

            XMLEventPipe pipe = new XMLEventPipe(reader, writer);
            pipe.pipe();

            return UjsonUtil.jsonObjectValueOf(output.toString());
        }
    }

    public String write(Value inputXML, Map<String, Object> params) throws Exception {
        JSONObject input = new JSONObject(UjsonUtil.jsonObjectValueTo(inputXML));

        if (params.containsKey(ROOT_ELEMENT)) {
            JSONObject root = new JSONObject();
            root.put(params.get(ROOT_ELEMENT).toString(), input);
            input = root;
        } else {
            //Check that input has only one root element
            JSONArray names = input.names();
            if (names.length() != 1) {
                throw new IllegalArgumentException("Object must have only one root element; has " + names.toString());
            }
            Object o = input.get(names.get(0).toString());
            if (!(o instanceof JSONObject)) {
                throw new IllegalArgumentException("Value of \"" + names.get(0) + "\" must be an object");
            }
        }

        try (StringWriter output = new StringWriter()){
            XMLStreamWriter2 writer = createXMLOutputStream(params, output);
            XMLStreamReader2 reader = createInputStream(params, input);

            XMLEventPipe pipe = new XMLEventPipe(reader, writer);
            pipe.pipe();

            return output.toString();
        }
    }

    private XMLStreamWriter2 createJSONOutputStream(Map<String, Object> params, StringWriter output) throws XMLStreamException {
        return Stax2WriterAdapter.wrapIfNecessary(new BadgerFishXMLStreamWriter(output, config(params)));
    }

    private XMLStreamReader2 createInputStream(Reader input) throws XMLStreamException {
        XMLInputFactory2 inputFactory = (XMLInputFactory2) XMLInputFactory.newFactory();
        inputFactory.setProperty(WstxInputProperties.P_ALLOW_XML11_ESCAPED_CHARS_IN_XML10, true);
        inputFactory.setProperty(XMLInputFactory2.P_REPORT_CDATA, Boolean.TRUE);

        return (XMLStreamReader2) inputFactory.createXMLStreamReader(input);
    }

    private XMLStreamReader2 createInputStream(Map<String, Object> params, JSONObject input) throws JSONException, XMLStreamException {
        BadgerFishConvention convention = new BadgerFishConvention(config(params));
        convention.setEncoding((String) params.getOrDefault(ENCODING, WstxOutputProperties.DEFAULT_OUTPUT_ENCODING));
        convention.setVersion((String) params.getOrDefault(XML_VERSION, WstxOutputProperties.DEFAULT_XML_VERSION));

        XMLStreamReader2 reader =  Stax2ReaderAdapter.wrapIfNecessary(new BadgerFishXMLStreamReader(input, convention));

        return filterReader(params, reader);
    }

    private XMLStreamWriter2 createXMLOutputStream(Map<String, Object> params, StringWriter output) throws XMLStreamException {
        XMLOutputFactory2 outputFactory = (XMLOutputFactory2) XMLOutputFactory.newInstance();

        if (params.containsKey(AUTO_EMPTY_ELEMENTS)) {
            outputFactory.setProperty(XMLOutputFactory2.P_AUTOMATIC_EMPTY_ELEMENTS, new Boolean(params.get(AUTO_EMPTY_ELEMENTS).toString()));
        }

        return (XMLStreamWriter2) outputFactory.createXMLStreamWriter(output, "UTF-8");
    }

    private BadgerFishConfiguration config(Map<String, Object> params) throws XMLStreamException {
        BadgerFishConfiguration config = new BadgerFishConfiguration();

        if (params.containsKey(NAMESPACE_DECLARATIONS)) {
            config.setNamespaceBindings((Map)params.get(NAMESPACE_DECLARATIONS));
        }
        if (params.containsKey(NULL_AS_EMPTY_ELEMENT)) {
            config.setNullAsEmptyElement(new Boolean(params.get(NULL_AS_EMPTY_ELEMENT).toString()));
        }
        if (params.containsKey(NAMESPACE_SEPARATOR)) {
            config.setNamespaceSeparator((String)params.get(NAMESPACE_SEPARATOR));
        }
        if (params.containsKey(TEXT_VALUE_KEY)) {
            config.setTextValueKey((String)params.get(TEXT_VALUE_KEY));
        }
        if (params.containsKey(CDATA_VALUE_KEY)) {
            config.setCdataValueKey((String)params.get(CDATA_VALUE_KEY));
        }
        if (params.containsKey(ATTRIBUTE_CHARACTER)) {
            config.setAttributeCharacter((String)params.get(ATTRIBUTE_CHARACTER));
        }

        return config;
    }

    private XMLStreamReader2 filterReader(Map<String, Object> params, XMLStreamReader2 reader) throws XMLStreamException {
        if (new Boolean(params.getOrDefault(OMIT_XML_DECLARATION, "false").toString())) {
            XMLInputFactory2 inputFactory = (XMLInputFactory2) XMLInputFactory.newFactory();
            reader = (XMLStreamReader2) inputFactory.createFilteredReader(reader, new StreamFilter() {
                @Override
                public boolean accept(XMLStreamReader reader) {
                    return reader.getEventType() != XMLStreamConstants.START_DOCUMENT;
                }
            });
        }
        return reader;
    }

    public String[] getSupportedIdentifiers() {
        return new String[] { "application/xml", "xml" };
    }

    @Override
    public Map<String, String> getReadParameters() {
        Map<String, String> readParams = new HashMap<>();
        readParams.put(NAMESPACE_DECLARATIONS, "Map of <prefix, namespace>");
        readParams.put(NAMESPACE_SEPARATOR, "Character which separates prefix and tag");
        readParams.put(TEXT_VALUE_KEY, "Json object key for the text value");
        readParams.put(CDATA_VALUE_KEY, "Json object key for the CDATA value");
        readParams.put(ATTRIBUTE_CHARACTER, "A prefix for attribute keys");
        return Collections.unmodifiableMap(readParams);
    }

    @Override
    public Map<String, String> getWriteParameters() {
        Map<String, String> writeParams = new HashMap<>();
        writeParams.put(NAMESPACE_DECLARATIONS, "Map of <prefix, namespace>");
        writeParams.put(NAMESPACE_SEPARATOR, "Character which separates prefix and tag");
        writeParams.put(TEXT_VALUE_KEY, "Json object key for the text value");
        writeParams.put(CDATA_VALUE_KEY, "Json object key for the CDATA value");
        writeParams.put(ATTRIBUTE_CHARACTER, "A prefix for attribute keys");
        writeParams.put(ENCODING, "XML encoding");
        writeParams.put(XML_VERSION, "XML version");
        writeParams.put(OMIT_XML_DECLARATION, "Omits <?xml ... ?> declaration from the output");
        writeParams.put(AUTO_EMPTY_ELEMENTS, "Automatically output empty elements, when a start element is immediately followed by matching end element.");
        writeParams.put(NULL_AS_EMPTY_ELEMENT, "Creates empty elements for null values");
        writeParams.put(ROOT_ELEMENT, "Root element wrapper");
        return Collections.unmodifiableMap(writeParams);
    }

    public String getPluginId() {
        return "XML";
    }

}
