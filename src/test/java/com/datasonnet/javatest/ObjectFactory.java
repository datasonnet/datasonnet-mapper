
package com.datasonnet.javatest;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.namespace.QName;


/**
 * This object contains factory methods for each 
 * Java content interface and Java element interface 
 * generated in the com.clearxchange.release4_0 package. 
 * <p>An ObjectFactory allows you to programatically 
 * construct new instances of the Java representation 
 * for XML content. The Java representation of XML 
 * content can consist of schema derived interfaces 
 * and classes representing the binding of schema 
 * type definitions, element declarations and model 
 * groups.  Factory methods for each of these are 
 * provided in this class.
 * 
 */
@XmlRegistry
public class ObjectFactory {

    private final static QName _TestField_QNAME = new QName("http://com.datasonnet.test", "testField");

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: com.clearxchange.release4_0
     * 
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link TestField }
     *
     */
    public TestField createTestField() {
        return new TestField();
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link Object }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link Object }{@code >}
     */
    @XmlElementDecl(namespace = "http://com.datasonnet.test", name = "testField")
    public JAXBElement<TestField> createTestField(TestField value) {
        return new JAXBElement<TestField>(_TestField_QNAME, TestField.class, value);
    }

}
