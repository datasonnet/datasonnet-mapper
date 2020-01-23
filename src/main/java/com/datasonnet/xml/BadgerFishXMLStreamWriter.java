package com.datasonnet.xml;

import org.codehaus.jettison.AbstractXMLStreamWriter;
import org.codehaus.jettison.Node;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.codehaus.jettison.util.FastStack;

import javax.xml.namespace.NamespaceContext;
import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.io.Writer;
import java.util.Iterator;

public class BadgerFishXMLStreamWriter extends AbstractXMLStreamWriter {
    private JSONObject root;
    private JSONObject currentNode;
    private Writer writer;
    private FastStack nodes;
    private String currentKey;

    private BadgerFishConfiguration configuration;

    public BadgerFishXMLStreamWriter(Writer writer) {
        this(writer, new BadgerFishConfiguration());
    }

    public BadgerFishXMLStreamWriter(Writer writer, BadgerFishConfiguration configuration) {
        super();
        this.root = new JSONObject();
        this.currentNode = this.root;
        this.writer = writer;
        this.nodes = new FastStack(); //TODO - do we need it?
        this.configuration = configuration;
    }

    public void close() throws XMLStreamException {
    }

    public void flush() throws XMLStreamException {

    }

    public NamespaceContext getNamespaceContext() {
        return configuration.getNamespaceContext();
    }

    public String getPrefix(String ns) throws XMLStreamException {
        return getNamespaceContext().getPrefix(ns);
    }

    public Object getProperty(String arg0) throws IllegalArgumentException {
        return null;
    }

    public void setDefaultNamespace(String arg0) throws XMLStreamException {
    }

    public void setNamespaceContext(NamespaceContext context) throws XMLStreamException {
        this.configuration.setNamespaceContext(context);
    }

    public void setPrefix(String prefix, String ns) throws XMLStreamException {
        SimpleNamespaceContext ctx = (SimpleNamespaceContext)configuration.getNamespaceContext();
        String existingNS = ctx.getNamespaceURI(prefix);

        if (existingNS == null || "".equals(existingNS)) {
            ctx.bindNamespaceUri(prefix, ns);
        } else {
            int cnt = 1;
            while (ctx.getNamespaceURI(prefix + cnt) != "") {
                cnt++;
            }
            ctx.bindNamespaceUri(prefix + cnt, ns);
        }
    }

    public void writeAttribute(String p, String ns, String local, String value) throws XMLStreamException {
        String key = createAttributeKey(p, ns, local);
        try {
            getCurrentNode().put(key, value);
        } catch (JSONException e) {
            throw new XMLStreamException(e);
        }
    }

    private String createAttributeKey(String p, String ns, String local) {
        return configuration.getAttributeCharacter() + createKey(p, ns, local);
    }

    private String createKey(String p, String ns, String local) {
        if (p == null) {
            return local;
        }

        String pp = getNamespaceContext().getPrefix(ns);
        if (pp == null) {
            pp = p;
        }

        return (pp.equals("") ? pp : pp + configuration.getNamespaceSeparator()) + local;
    }

    public void writeAttribute(String ns, String local, String value) throws XMLStreamException {
        writeAttribute(null, ns, local, value);
    }

    public void writeAttribute(String local, String value) throws XMLStreamException {
        writeAttribute(null, local, value);
    }

    public void writeCData(String text) throws XMLStreamException {
        writeCharacters(text, true);
    }

    public void writeCharacters(String text) throws XMLStreamException {
        writeCharacters(text, false);
    }

    private void writeCharacters(String text, boolean isCDATA) throws XMLStreamException {
        text = text.trim();
        if (text.length() == 0) {
            return;
        }

        String keyPrefix = (isCDATA ? configuration.getCdataValueKey() : configuration.getTextValueKey());

        try {
            int counter = 0;
            Iterator keys = getCurrentNode().keys();
            while (keys.hasNext()) {
                String nextKey = (String)keys.next();
                if (!isCDATA && nextKey.equalsIgnoreCase(keyPrefix)) { //Adding second text attribute requires removing the old one and renaming it
                    Object oldText = getCurrentNode().remove(nextKey);
                    getCurrentNode().put(keyPrefix + "1", text);
                    return;
                }
                if (nextKey.startsWith(keyPrefix)) {
                    counter++;
                }
            }

            if (counter == 0) {
                if (isCDATA) { //CDATA fragments all have numbers
                    counter = 1;
                }
            } else {
                counter++;
            }
            getCurrentNode().put(keyPrefix + (counter == 0 ? "" : counter), text);
        } catch (JSONException e) {
            throw new XMLStreamException(e);
        }
    }

    public void writeDefaultNamespace(String ns) throws XMLStreamException {
        writeNamespace("", ns);
    }

    public void writeEndElement() throws XMLStreamException {
        if (getNodes().size() > 1) {
            getNodes().pop();
            currentNode = ((Node) getNodes().peek()).getObject();
        }
    }

    public void writeEntityRef(String arg0) throws XMLStreamException {
        // TODO Auto-generated method stub

    }

    public void writeNamespace(String prefix, String ns) throws XMLStreamException {
        SimpleNamespaceContext ctx = (SimpleNamespaceContext)configuration.getNamespaceContext();

        String _prefix = prefix;
        String configuredPrefix = ctx.getPrefix(ns);

        if (configuredPrefix != null) {
            _prefix = configuredPrefix;
        }

        ((Node) getNodes().peek()).setNamespace(_prefix, ns);

        JSONObject currentNode = getCurrentNode();

        try {
            JSONObject nsObj = currentNode.optJSONObject(configuration.getAttributeCharacter() + "xmlns");
            if (nsObj == null) {
                nsObj = new JSONObject();
                currentNode.put(configuration.getAttributeCharacter() + "xmlns", nsObj);
            }
            if (_prefix.equals("")) {
                _prefix = "$";
            }
            nsObj.put(_prefix, ns);
        } catch (JSONException e) {
            throw new XMLStreamException(e);
        }
    }

    public void writeProcessingInstruction(String arg0, String arg1) throws XMLStreamException {
        // TODO Auto-generated method stub

    }

    public void writeProcessingInstruction(String arg0) throws XMLStreamException {
        // TODO Auto-generated method stub

    }

    public void writeStartDocument() throws XMLStreamException {
    }


    public void writeEndDocument() throws XMLStreamException {
        try {
            root.write(writer);
            writer.flush();
        } catch (JSONException e) {
            throw new XMLStreamException(e);
        } catch (IOException e) {
            throw new XMLStreamException(e);
        }
    }

    public void writeStartElement(String prefix, String local, String ns) throws XMLStreamException {
        try {
            currentKey = createKey(prefix, ns, local);
            Object existing = getCurrentNode().opt(currentKey);
            if (existing instanceof JSONObject) {
                JSONArray array = new JSONArray();
                array.put(existing);

                JSONObject newCurrent = new JSONObject();
                array.put(newCurrent);

                getCurrentNode().put(currentKey, array);

                currentNode = newCurrent;
                Node node = new Node(currentNode);
                getNodes().push(node);
            } else {
                JSONObject newCurrent = new JSONObject();

                if (existing instanceof JSONArray) {
                    ((JSONArray) existing).put(newCurrent);
                } else {
                    getCurrentNode().put(currentKey, newCurrent);
                }

                currentNode = newCurrent;
                Node node = new Node(currentNode);
                getNodes().push(node);
            }
        } catch (JSONException e) {
            throw new XMLStreamException("Could not write start element!", e);
        }
    }

    protected JSONObject getCurrentNode() {
        return currentNode;
    }

    protected FastStack getNodes() {
        return nodes;
    }
}