package com.datasonnet.badgerfish;


import org.codehaus.jettison.AbstractXMLStreamReader;
import org.codehaus.jettison.Node;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.codehaus.jettison.util.FastStack;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class BadgerFishXMLStreamReader extends AbstractXMLStreamReader {
    private BadgerFishConvention CONVENTION;
    private FastStack nodes;
    private String currentText;

    private String outputVersion = "1.0";
    private String outputEncoding = "UTF-8";

    private Map<String, String> prefixReplacements = new HashMap<>();

    public BadgerFishXMLStreamReader(JSONObject obj) throws JSONException, XMLStreamException {
        this(obj, new BadgerFishConvention());
    }

    public BadgerFishXMLStreamReader(JSONObject obj, com.datasonnet.badgerfish.BadgerFishConvention convention) throws JSONException, XMLStreamException {
        CONVENTION = convention;
        String rootName = (String) obj.keys().next();
        this.node = new Node(null, rootName, obj.getJSONObject(rootName), CONVENTION);
        this.nodes = new FastStack();
        nodes.push(node);
        event = START_DOCUMENT;
    }

    public int next() throws XMLStreamException {
        if (event == START_DOCUMENT) {
            event = START_ELEMENT;
        } else {
            // should always be blanked on new events, but isn't in at least one case; this takes care of all cases
            currentText = null;
            if (event == END_ELEMENT && nodes.size() != 0) {
                node = (Node) nodes.peek();
            }
            
            if (node.getArray() != null 
                && node.getArray().length() > node.getArrayIndex()) {
                Node arrayNode = node;
                int idx = arrayNode.getArrayIndex();
                
                try {
                    Object o = arrayNode.getArray().get(idx);
                    processKey(node.getCurrentKey(), o);
                } catch (JSONException e) {
                    throw new XMLStreamException(e);
                }
                
                idx++;
                arrayNode.setArrayIndex(idx);
            } else if (node.getKeys() != null && node.getKeys().hasNext()) {
                processElement();
            } else {
                if (nodes.size() != 0) {
                    event = END_ELEMENT;
                    node = (Node)nodes.pop();
                } else {
                    event = END_DOCUMENT;
                }
            }
        }

        return event;
    }
    
    private void processElement() throws XMLStreamException {
        try {
            String nextKey = (String) node.getKeys().next();
            
            Object newObj = node.getObject().get(nextKey);
            
            processKey(nextKey, newObj);
        } catch (JSONException e) {
            throw new XMLStreamException(e);
        }
    }

    private void processKey(String nextKey, Object newObj) throws JSONException, XMLStreamException {
        if (nextKey.startsWith(CONVENTION.getConfiguration().getTextValueKey())) {
            event = CHARACTERS;
            if (newObj instanceof JSONArray) {
                JSONArray arr = (JSONArray)newObj;
                StringBuilder buf = new StringBuilder();
                for (int i = 0; i < arr.length(); i++) {
                    buf.append(arr.get(i));
                }
                currentText = buf.toString();
            } else {
                currentText = newObj == null ? null : newObj.toString();
            }
            return;
        } else if (nextKey.startsWith(CONVENTION.getConfiguration().getCdataValueKey())) {
            event = CDATA;
            if (newObj instanceof JSONArray) {
                JSONArray arr = (JSONArray)newObj;
                StringBuilder buf = new StringBuilder();
                for (int i = 0; i < arr.length(); i++) {
                    buf.append(arr.get(i));
                }
                currentText = buf.toString();
            } else {
                currentText = newObj == null ? null : newObj.toString();
            }
            return;

        } if (newObj instanceof JSONObject) {
            node = new Node((Node)nodes.peek(), nextKey, (JSONObject) newObj, CONVENTION);
            nodes.push(node);
            event = START_ELEMENT;
            return;
        } else if (newObj instanceof JSONArray) {
            JSONArray arr = (JSONArray) newObj;

            if (arr.length() == 0) {
                next();
                return;
            }
            
            // save some state information...
            node.setArray(arr);
            node.setArrayIndex(1);
            node.setCurrentKey(nextKey);
            
            processKey(nextKey, arr.get(0));
        } else if (newObj.equals(JSONObject.EXPLICIT_NULL) || newObj.equals(JSONObject.NULL)) { //Treat it as empty element or ignore
            if (CONVENTION.getConfiguration().isNullAsEmptyElement()) {
                node = new Node((Node)nodes.peek(), nextKey, new JSONObject("{}"), CONVENTION);
                nodes.push(node);
                event = START_ELEMENT;
            } else {
                event = SPACE;
            }
            return;
        } else if (newObj instanceof String || newObj instanceof Boolean || newObj instanceof Number) {
            String json = "{ \"" + CONVENTION.getConfiguration().getTextValueKey() + "\":\"" + newObj.toString() + "\"}";
            node = new Node((Node)nodes.peek(), nextKey, new JSONObject(json), CONVENTION);
            nodes.push(node);
            event = START_ELEMENT;
            return;
        } else {
            throw new JSONException("Element [" + nextKey + "] did not contain object, array or text content.");
        }        
    }

    public void close() throws XMLStreamException {
    }

    @Override public QName getAttributeName(int n) {
        QName internal = super.getAttributeName(n);
        return rewriteQName(internal);
    }

    @Override public String getAttributePrefix(int n) {
        return this.getAttributeName(n).getPrefix();
    }

    @Override public QName getName() {
        return rewriteQName(super.getName());
    }

    @Override public String getNamespaceURI() {
        return this.getName().getNamespaceURI();
    }

    @Override public String getNamespacePrefix(int n) {
        String prefix = super.getNamespacePrefix(n);
        String uri = this.getNamespaceURI(n);
        String combined = prefix + ":" + uri;

        NamespaceContext context = this.getNamespaceContext();
        String overridePrefix = context.getPrefix(uri);
        String overrideURI = context.getNamespaceURI(prefix);

        if(prefixReplacements.containsKey(combined)) {
            // previously overridden
            return prefixReplacements.get(combined);
        } else if(overridePrefix != null && !prefix.equals(overridePrefix)) {
            // we've got an override for that URI, make sure it has the right URI
            prefixReplacements.put(combined, overridePrefix);
            return overridePrefix;
        } else if((overrideURI != XMLConstants.NULL_NS_URI && !uri.equals(overrideURI)) || prefixReplacements.containsValue(prefix)) {
            // an override is using that prefix, or a previous combo
            String newPrefix = findPrefix(prefix);
            prefixReplacements.put(combined, newPrefix);
            return newPrefix;
        } else {
            return prefix;
        }
    }

    @Override public String getNamespaceURI(String prefix) {
        // supporting this properly would require complicated logic due to the stack-like nature of things
        throw new RuntimeException("Not supported");
    }

    @Override public String getPrefix() {
        return rewriteQName(super.getName()).getPrefix();
    }

    private QName rewriteQName(QName name) {
        String uri = name.getNamespaceURI();
        String prefix = name.getPrefix();
        String combined = prefix + ":" + uri;

        // this only works because any reasonable implementation will traverse the namespaces first
        if(prefixReplacements.containsKey(combined)) {
            prefix = prefixReplacements.get(combined);
        }

        return new QName(uri, name.getLocalPart(), prefix);
    }

    private String findPrefix(String prefix) {
        int suffix = 1;
        while(taken(prefix + suffix)) {
            suffix++;
        }
        return prefix + suffix;
    }

    private boolean taken(String prefix) {
        return this.getNamespaceContext().getNamespaceURI(prefix) != XMLConstants.NULL_NS_URI
                || prefixReplacements.containsValue(prefix)
                || node.getNamespaceURI(prefix) != null;
    }


    public String getAttributeType(int arg0) {
        return null;
    }

    public String getCharacterEncodingScheme() {
        return null;
    }

    public String getElementText() throws XMLStreamException {
        return currentText;
    }

    public NamespaceContext getNamespaceContext() {
        return CONVENTION.getNamespaceContext();
    }

    public String getText() {

        return currentText;
    }

    public String getVersion() {
        return CONVENTION.getVersion();
    }

    public String getEncoding() {
        return CONVENTION.getEncoding();
    }
}
