package com.datasonnet.plugins.jackson;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

@JsonIgnoreProperties(value = {"globalScope", "typeSubstituted", "nil", "scope"})
public abstract class JAXBElementMixIn<T> extends JAXBElement<T> {
    @JsonCreator
    public JAXBElementMixIn(@JsonProperty("name") QName name,
                            @JsonProperty("declaredType") Class<T> declaredType,
                            @JsonProperty("value") T value) {
        super(name, declaredType, value);
    }
}
