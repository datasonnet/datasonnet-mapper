package com.datasonnet.badgerfish;

import javax.xml.namespace.NamespaceContext;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class BadgerFishConfiguration {

    private String namespaceSeparator;
    private String attributeCharacter;
    private String textValueKey;
    private String cdataValueKey;
    private boolean nullAsEmptyElement;
    private boolean omitXmlDeclaration;

    private NamespaceContext namespaceContext;

    public static String DEFAULT_NAMESPACE_SEPARATOR = ":";
    public static String DEFAULT_ATTRIBUTE_CHARACTER = "@";
    public static String DEFAULT_TEXT_VALUE_KEY = "$";
    public static String DEFAULT_CDATA_VALUE_KEY = "#";

    public BadgerFishConfiguration() {
        this(DEFAULT_NAMESPACE_SEPARATOR, DEFAULT_ATTRIBUTE_CHARACTER, DEFAULT_TEXT_VALUE_KEY, DEFAULT_CDATA_VALUE_KEY, true, false, Collections.emptyMap());
    }

    public BadgerFishConfiguration(String namespaceSeparator, String attributeCharacter, String textValueKey, String cdataValueKey, boolean nullAsEmptyElement, boolean omitXmlDeclaration, Map<String, String> namespaces) {
        this.namespaceSeparator = namespaceSeparator;
        this.attributeCharacter = attributeCharacter;
        this.textValueKey = textValueKey;
        this.cdataValueKey = cdataValueKey;

        this.nullAsEmptyElement = nullAsEmptyElement;
        this.omitXmlDeclaration = omitXmlDeclaration;

        SimpleNamespaceContext namespaceContext = new SimpleNamespaceContext();
        namespaceContext.setBindings(namespaces);

        this.namespaceContext = namespaceContext;
    }

    public String getNamespaceSeparator() {
        return namespaceSeparator;
    }

    public void setNamespaceSeparator(String namespaceSeparator) {
        this.namespaceSeparator = namespaceSeparator;
    }

    public String getAttributeCharacter() {
        return attributeCharacter;
    }

    public void setAttributeCharacter(String attributeCharacter) {
        this.attributeCharacter = attributeCharacter;
    }

    public NamespaceContext getNamespaceContext() {
        return namespaceContext;
    }

    public void setNamespaceContext(NamespaceContext context) {
        this.namespaceContext = context;
    }

    public void setNamespaceBindings(Map<String, String> namespaces) {
        ((SimpleNamespaceContext)getNamespaceContext()).setBindings(namespaces);
    }

    public String getTextValueKey() {
        return textValueKey;
    }

    public void setTextValueKey(String textValueKey) {
        this.textValueKey = textValueKey;
    }

    public String getCdataValueKey() {
        return cdataValueKey;
    }

    public void setCdataValueKey(String cdataValueKey) {
        this.cdataValueKey = cdataValueKey;
    }

    public boolean isNullAsEmptyElement() {
        return nullAsEmptyElement;
    }

    public void setNullAsEmptyElement(boolean nullAsEmptyElement) {
        this.nullAsEmptyElement = nullAsEmptyElement;
    }

    public boolean isOmitXmlDeclaration() {
        return omitXmlDeclaration;
    }

    public void setOmitXmlDeclaration(boolean omitXmlDeclaration) {
        this.omitXmlDeclaration = omitXmlDeclaration;
    }
}
