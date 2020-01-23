package com.datasonnet.xml;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import javax.xml.namespace.NamespaceContext;

public class SimpleNamespaceContext implements NamespaceContext {
    private final Map<String, String> prefixToNamespaceUri = new HashMap();
    private final Map<String, Set<String>> namespaceUriToPrefixes = new HashMap();
    private String defaultNamespaceUri = "";

    public SimpleNamespaceContext() {
    }

    public String getNamespaceURI(String prefix) {
        //Assert.notNull(prefix, "No prefix given");
        if ("xml".equals(prefix)) {
            return "http://www.w3.org/XML/1998/namespace";
        } else if ("xmlns".equals(prefix)) {
            return "http://www.w3.org/2000/xmlns/";
        } else if ("".equals(prefix)) {
            return this.defaultNamespaceUri;
        } else {
            return this.prefixToNamespaceUri.getOrDefault(prefix, "");
        }
    }

    //@Nullable
    public String getPrefix(String namespaceUri) {
        Set<String> prefixes = this.getPrefixesSet(namespaceUri);
        return !prefixes.isEmpty() ? (String)prefixes.iterator().next() : null;
    }

    public Iterator<String> getPrefixes(String namespaceUri) {
        return this.getPrefixesSet(namespaceUri).iterator();
    }

    private Set<String> getPrefixesSet(String namespaceUri) {
        //Assert.notNull(namespaceUri, "No namespaceUri given");
        if (this.defaultNamespaceUri.equals(namespaceUri)) {
            return Collections.singleton("");
        } else if ("http://www.w3.org/XML/1998/namespace".equals(namespaceUri)) {
            return Collections.singleton("xml");
        } else if ("http://www.w3.org/2000/xmlns/".equals(namespaceUri)) {
            return Collections.singleton("xmlns");
        } else {
            Set<String> prefixes = (Set)this.namespaceUriToPrefixes.get(namespaceUri);
            return prefixes != null ? Collections.unmodifiableSet(prefixes) : Collections.emptySet();
        }
    }

    public void setBindings(Map<String, String> bindings) {
        bindings.forEach(this::bindNamespaceUri);
    }

    public void bindNamespaceUri(String prefix, String namespaceUri) {
        if ("".equals(prefix)) {
            this.defaultNamespaceUri = namespaceUri;
        } else {
            this.prefixToNamespaceUri.put(prefix, namespaceUri);
            Set<String> prefixes = (Set)this.namespaceUriToPrefixes.computeIfAbsent(namespaceUri, (k) -> {
                return new LinkedHashSet();
            });
            prefixes.add(prefix);
        }

    }
}
