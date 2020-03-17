package com.datasonnet.xml;

import org.codehaus.jettison.Convention;
import org.codehaus.jettison.Node;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import java.util.Iterator;
import java.util.Map;

public class BadgerFishConvention implements Convention {
    private BadgerFishConfiguration configuration;

    private String version;
    private String encoding;
    private String standalone;

    public BadgerFishConvention() {
        this(new BadgerFishConfiguration());
    }

    public BadgerFishConvention(BadgerFishConfiguration config) {
        configuration = config;
    }

    public void processAttributesAndNamespaces(Node n, JSONObject object) 
        throws JSONException, XMLStreamException {
        // Read in the attributes, and stop when there are no more
        for (Iterator itr = object.keys(); itr.hasNext();) {
            String k = (String) itr.next();
            
            if (k.startsWith(getConfiguration().getAttributeCharacter())) {
                Object o = object.opt(k);
                k = k.substring(1);
                if (k.equals("xmlns")) {
                    // if its a string its a default namespace
                    if (o instanceof JSONObject) {
                        JSONObject jo = (JSONObject) o;
                        for (Iterator pitr = jo.keys(); pitr.hasNext(); ) {
                            String prefix = (String) pitr.next();
                            String uri = jo.getString(prefix);

                            if (prefix.equals("$")) {
                                prefix = "";
                            }

                            n.setNamespace(prefix, uri);
                        }                        
                    }
                } else {
                    String strValue = (String) o; 
                    QName name = null;
                    // note that a non-prefixed attribute name implies NO namespace,
                    // i.e. as opposed to the in-scope default namespace
                    if (k.contains(getConfiguration().getNamespaceSeparator())) {
                        name = createQName(k, n);
                    } else {
                        name = new QName(XMLConstants.DEFAULT_NS_PREFIX, k);
                    }                    
                    n.setAttribute(name, strValue);
                }
                itr.remove();
            }
        }
    }

    public QName createQName(String rootName, Node node) throws XMLStreamException {
        int idx = rootName.indexOf(getConfiguration().getNamespaceSeparator());
        if (idx != -1) {
            String prefix = rootName.substring(0, idx);
            String local = rootName.substring(idx+1);
            
            String uri = (String) node.getNamespaceURI(prefix);
            
            return new QName(uri, local, prefix);
        }
        
        String uri = (String) node.getNamespaceURI("");
        if (uri != null) {
            return new QName(uri, rootName);
        }
        
        return new QName(rootName);
    }

    public void setNamespaceBindings(Map<String, String> namespaces) {
        getConfiguration().setNamespaceBindings(namespaces);
    }

    public NamespaceContext getNamespaceContext() {
        return getConfiguration().getNamespaceContext();
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getEncoding() {
        return encoding;
    }

    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    public BadgerFishConfiguration getConfiguration() {
        return configuration;
    }

    public void setConfiguration(BadgerFishConfiguration configuration) {
        this.configuration = configuration;
    }
}
