package com.datasonnet.jackson;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

@JsonIgnoreProperties(value = {"globalScope", "typeSubstituted", "nil"})
public abstract class JAXBElementMixIn<T> extends JAXBElement<T> {

/*
    @JsonCreator
    public JAXBElementMixIn(@JsonProperty("name") QName name,
                            @JsonProperty("declaredType") Class<T> declaredType,
                            @JsonProperty("scope") Class scope,
                            @JsonProperty("value") T value) {
        super(name, declaredType, scope, value);
    }
*/

    @JsonCreator
    public JAXBElementMixIn(@JsonProperty("name") QName name,
                            @JsonProperty("declaredType") Class<T> declaredType,
                            @JsonProperty("value") T value) {
        super(name, declaredType, value);
    }
}
