package com.datasonnet;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DWStringsTest {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final String lib = "DW" +".";
    private final String pack = "Strings";

    @Test
    void testDW_appendIfMissing() {
        Mapper mapper = new Mapper(lib + pack + ".appendIfMissing(\"abc\", \"xyz\")", new ArrayList<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("abcxyz", value);

        mapper = new Mapper(lib +  pack + ".appendIfMissing(\"abcxyz\", \"xyz\")", new ArrayList<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("abcxyz", value);
    }

    @Test
    void testDW_camelize() {
        Mapper mapper = new Mapper(lib + pack + ".camelize(\"customer_first_name\")", new ArrayList<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("customerFirstName", value);

        mapper = new Mapper(lib + pack + ".camelize(\"_customer_first_name\")", new ArrayList<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("customerFirstName", value);

        mapper = new Mapper(lib + pack + ".camelize(\"_______customer_first_name\")", new ArrayList<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("customerFirstName", value);

        mapper = new Mapper(lib + pack + ".camelize(null)", new ArrayList<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("null", value);
    }

    @Test
    void testDW_capitalize() {
        Mapper mapper = new Mapper(lib + pack + ".capitalize(\"customer\")", new ArrayList<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("Customer", value);

        mapper = new Mapper(lib + pack + ".capitalize(\"customer_first_name\")", new ArrayList<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("Customer First Name", value);

        mapper = new Mapper(lib + pack + ".capitalize(\"customer NAME\")", new ArrayList<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("Customer Name", value);

        mapper = new Mapper(lib + pack + ".capitalize(\"customerName\")", new ArrayList<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("Customer Name", value);

        mapper = new Mapper(lib + pack + ".capitalize(null)", new ArrayList<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("null", value);
    }

    @Test
    void testDW_charCode() {
        Mapper mapper = new Mapper(lib + pack + ".charCode(\"Master\")", new ArrayList<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("77", value);

        mapper = new Mapper(lib + pack + ".charCode(\"M\")", new ArrayList<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("77", value);
    }

    @Test
    void testDW_charCodeAt() {
        Mapper mapper = new Mapper(lib + pack + ".charCodeAt(\"charCodeAt\", 4)", new ArrayList<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("67", value);

        mapper = new Mapper(lib + pack + ".charCodeAt(\"charCodeAt\", 8)", new ArrayList<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("65", value);
    }

    @Test
    void testDW_dasherize() {
        Mapper mapper = new Mapper(lib + pack + ".dasherize(\"customer\")", new ArrayList<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("customer", value);

        mapper = new Mapper(lib + pack + ".dasherize(\"customer_first_name\")", new ArrayList<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("customer-first-name", value);

        mapper = new Mapper(lib + pack + ".dasherize(\"customer NAME\")", new ArrayList<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("customer-name", value);

        mapper = new Mapper(lib + pack + ".dasherize(\"customerName\")", new ArrayList<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("customer-name", value);

        mapper = new Mapper(lib + pack + ".dasherize(null)", new ArrayList<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("null", value);
    }

    @Test
    void testDW_fromCharCode() {
        Mapper mapper = new Mapper(lib + pack + ".fromCharCode(67)", new ArrayList<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("C", value);

        mapper = new Mapper(lib + pack + ".fromCharCode(65)", new ArrayList<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("A", value);
    }

    @Test
    void testDW_isAlpha() {
        Mapper mapper = new Mapper(lib + pack + ".isAlpha(\"sdfvxer\")", new ArrayList<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("true", value);

        mapper = new Mapper(lib + pack + ".isAlpha(\"ecvt4\")", new ArrayList<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("false", value);

        mapper = new Mapper(lib + pack + ".isAlpha(true)", new ArrayList<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("true", value);

        mapper = new Mapper(lib + pack + ".isAlpha(45)", new ArrayList<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("false", value);
    }

    @Test
    void testDW_isAlphanumeric() {
        Mapper mapper = new Mapper(lib + pack + ".isAlphanumeric(\"sdfvxer\")", new ArrayList<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("true", value);

        mapper = new Mapper(lib + pack + ".isAlphanumeric(\"ecvt4\")", new ArrayList<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("true", value);

        mapper = new Mapper(lib + pack + ".isAlphanumeric(true)", new ArrayList<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("true", value);

        mapper = new Mapper(lib + pack + ".isAlphanumeric(45)", new ArrayList<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("true", value);
    }

    @Test
    void testDW_isLowerCase() {
        Mapper mapper = new Mapper(lib + pack + ".isLowerCase(\"sdfvxer\")", new ArrayList<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("true", value);

        mapper = new Mapper(lib + pack + ".isLowerCase(\"ecvt4\")", new ArrayList<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("false", value);

        mapper = new Mapper(lib + pack + ".isLowerCase(\"eCvt\")", new ArrayList<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("false", value);

        mapper = new Mapper(lib + pack + ".isLowerCase(true)", new ArrayList<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("true", value);

        mapper = new Mapper(lib + pack + ".isLowerCase(45)", new ArrayList<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("false", value);
    }

    @Test
    void testDW_isNumeric() {
        Mapper mapper = new Mapper(lib + pack + ".isNumeric(\"sdfvxer\")", new ArrayList<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("false", value);

        mapper = new Mapper(lib + pack + ".isNumeric(\"5334\")", new ArrayList<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("true", value);

        mapper = new Mapper(lib + pack + ".isNumeric(100)", new ArrayList<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("true", value);
    }

    @Test
    void testDW_isUpperCase() {
        Mapper mapper = new Mapper(lib + pack + ".isUpperCase(\"SDFVXER\")", new ArrayList<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("true", value);

        mapper = new Mapper(lib + pack + ".isUpperCase(\"ECVT4\")", new ArrayList<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("false", value);

        mapper = new Mapper(lib + pack + ".isUpperCase(\"EcVT\")", new ArrayList<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("false", value);

        mapper = new Mapper(lib + pack + ".isUpperCase(true)", new ArrayList<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("false", value);

        mapper = new Mapper(lib + pack + ".isUpperCase(45)", new ArrayList<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("false", value);
    }

    @Test
    void testDW_isWhitespace() {
        Mapper mapper = new Mapper(lib + pack + ".isWhitespace(null)", new ArrayList<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("false", value);

        mapper = new Mapper(lib + pack + ".isWhitespace(\"\")", new ArrayList<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("true", value);

        mapper = new Mapper(lib + pack + ".isWhitespace(\"       \")", new ArrayList<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("true", value);

        mapper = new Mapper(lib + pack + ".isWhitespace(\"   abc    \")", new ArrayList<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("false", value);

        mapper = new Mapper(lib + pack + ".isWhitespace(true)", new ArrayList<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("false", value);

        mapper = new Mapper(lib + pack + ".isWhitespace(45)", new ArrayList<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("false", value);
    }

    @Test
    void testDW_leftPad() {
        Mapper mapper = new Mapper(lib + pack + ".leftPad(null,3)", new ArrayList<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("null", value);

        mapper = new Mapper(lib + pack + ".leftPad(\"\",3)", new ArrayList<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("   ", value);

        mapper = new Mapper(lib + pack + ".leftPad(\"bat\",5)", new ArrayList<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("  bat", value);

        mapper = new Mapper(lib + pack + ".leftPad(\"bat\",3)", new ArrayList<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("bat", value);

        mapper = new Mapper(lib + pack + ".leftPad(\"bat\",-1)", new ArrayList<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("bat", value);

        mapper = new Mapper(lib + pack + ".leftPad(45,3)", new ArrayList<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals(" 45", value);

        mapper = new Mapper(lib + pack + ".leftPad(true,10)", new ArrayList<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("      true", value);
    }

}
