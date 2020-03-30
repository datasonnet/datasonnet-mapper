package com.datasonnet.javatest;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.*;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
        "testField"
})
@XmlRootElement(name = "WsdlGeneratedObj")
public class WsdlGeneratedObj {
    @XmlElementRef(name = "testField", namespace = "http://com.datasonnet.test", type = JAXBElement.class, required = true)
    protected JAXBElement<TestField> testField;

    /**
     * Gets the value of the testField property.
     *
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link Object }{@code >}
     *
     */
    public JAXBElement<TestField> getTestField() {
        return testField;
    }

    /**
     * Sets the value of the testField property.
     *
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link Object }{@code >}
     *
     */
    public void setTestField(JAXBElement<TestField> value) {
        this.testField = value;
    }
}
