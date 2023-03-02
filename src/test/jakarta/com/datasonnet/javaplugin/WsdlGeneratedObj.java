package com.datasonnet.javaplugin;

/*-
 * Copyright 2019-2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.annotation.*;

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
     * @return possible object is
     * {@link JAXBElement }{@code <}{@link Object }{@code >}
     */
    public JAXBElement<TestField> getTestField() {
        return testField;
    }

    /**
     * Sets the value of the testField property.
     *
     * @param value allowed object is
     *              {@link JAXBElement }{@code <}{@link Object }{@code >}
     */
    public void setTestField(JAXBElement<TestField> value) {
        this.testField = value;
    }
}
