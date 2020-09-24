package com.datasonnet;

/*-
 * Copyright 2019-2020 the original author or authors.
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

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class StringsTest {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final String lib = "ds" + ".";
    private final String pack = "strings";

    @Test
    void testStrings_appendIfMissing() {
        Mapper mapper = new Mapper(lib + pack + ".appendIfMissing(\"abc\", \"xyz\")", new ArrayList<>(), new HashMap<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("abcxyz", value);

        mapper = new Mapper(lib + pack + ".appendIfMissing(\"abcxyz\", \"xyz\")", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("abcxyz", value);

        mapper = new Mapper(lib + pack + ".appendIfMissing(null, \"\")", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("null", value);

        mapper = new Mapper(lib + pack + ".appendIfMissing(\"xyza\", \"xyz\")", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("xyzaxyz", value);

        mapper = new Mapper(lib + pack + ".appendIfMissing(\"\", \"xyz\")", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("xyz", value);
    }

    @Test
    void testStrings_camelize() {
        Mapper mapper = new Mapper(lib + pack + ".camelize(\"customer_first_name\")", new ArrayList<>(), new HashMap<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("customerFirstName", value);

        mapper = new Mapper(lib + pack + ".camelize(\"_customer_first_name\")", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("customerFirstName", value);

        mapper = new Mapper(lib + pack + ".camelize(\"_______customer_first_name\")", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("customerFirstName", value);

        mapper = new Mapper(lib + pack + ".camelize(null)", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("null", value);
    }

    @Test
    void testStrings_capitalize() {
        Mapper mapper = new Mapper(lib + pack + ".capitalize(\"customer\")", new ArrayList<>(), new HashMap<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("Customer", value);

        mapper = new Mapper(lib + pack + ".capitalize(\"customer_first_name\")", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("Customer First Name", value);

        mapper = new Mapper(lib + pack + ".capitalize(\"customer NAME\")", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("Customer Name", value);

        mapper = new Mapper(lib + pack + ".capitalize(\"customerName\")", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("Customer Name", value);

        mapper = new Mapper(lib + pack + ".capitalize(null)", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("null", value);
    }

    @Test
    void testStrings_charCode() {
        Mapper mapper = new Mapper(lib + pack + ".charCode(\"Master\")", new ArrayList<>(), new HashMap<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("77", value);

        mapper = new Mapper(lib + pack + ".charCode(\"M\")", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("77", value);
    }

    @Test
    void testStrings_charCodeAt() {
        Mapper mapper = new Mapper(lib + pack + ".charCodeAt(\"charCodeAt\", 4)", new ArrayList<>(), new HashMap<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("67", value);

        mapper = new Mapper(lib + pack + ".charCodeAt(\"charCodeAt\", 8)", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("65", value);
    }

    @Test
    void testStrings_dasherize() {
        Mapper mapper = new Mapper(lib + pack + ".dasherize(\"customer\")", new ArrayList<>(), new HashMap<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("customer", value);

        mapper = new Mapper(lib + pack + ".dasherize(\"customer_first_name\")", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("customer-first-name", value);

        mapper = new Mapper(lib + pack + ".dasherize(\"customer NAME\")", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("customer-name", value);

        mapper = new Mapper(lib + pack + ".dasherize(\"customerName\")", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("customer-name", value);

        mapper = new Mapper(lib + pack + ".dasherize(null)", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("null", value);
    }

    @Test
    void testStrings_fromCharCode() {
        Mapper mapper = new Mapper(lib + pack + ".fromCharCode(67)", new ArrayList<>(), new HashMap<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("C", value);

        mapper = new Mapper(lib + pack + ".fromCharCode(65)", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("A", value);
    }

    @Test
    void testStrings_isAlpha() {
        Mapper mapper = new Mapper(lib + pack + ".isAlpha(\"sdfvxer\")", new ArrayList<>(), new HashMap<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("true", value);

        mapper = new Mapper(lib + pack + ".isAlpha(\"ecvt4\")", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("false", value);

        mapper = new Mapper(lib + pack + ".isAlpha(true)", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("true", value);

        mapper = new Mapper(lib + pack + ".isAlpha(45)", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("false", value);
    }

    @Test
    void testStrings_isAlphanumeric() {
        Mapper mapper = new Mapper(lib + pack + ".isAlphanumeric(\"sdfvxer\")", new ArrayList<>(), new HashMap<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("true", value);

        mapper = new Mapper(lib + pack + ".isAlphanumeric(\"ecvt4\")", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("true", value);

        mapper = new Mapper(lib + pack + ".isAlphanumeric(true)", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("true", value);

        mapper = new Mapper(lib + pack + ".isAlphanumeric(45)", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("true", value);
    }

    @Test
    void testStrings_isLowerCase() {
        Mapper mapper = new Mapper(lib + pack + ".isLowerCase(\"sdfvxer\")", new ArrayList<>(), new HashMap<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("true", value);

        mapper = new Mapper(lib + pack + ".isLowerCase(\"ecvt4\")", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("false", value);

        mapper = new Mapper(lib + pack + ".isLowerCase(\"eCvt\")", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("false", value);

        mapper = new Mapper(lib + pack + ".isLowerCase(true)", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("true", value);

        mapper = new Mapper(lib + pack + ".isLowerCase(45)", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("false", value);
    }

    @Test
    void testStrings_isNumeric() {
        Mapper mapper = new Mapper(lib + pack + ".isNumeric(\"sdfvxer\")", new ArrayList<>(), new HashMap<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("false", value);

        mapper = new Mapper(lib + pack + ".isNumeric(\"5334\")", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("true", value);

        mapper = new Mapper(lib + pack + ".isNumeric(100)", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("true", value);
    }

    @Test
    void testStrings_isUpperCase() {
        Mapper mapper = new Mapper(lib + pack + ".isUpperCase(\"SDFVXER\")", new ArrayList<>(), new HashMap<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("true", value);

        mapper = new Mapper(lib + pack + ".isUpperCase(\"ECVT4\")", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("false", value);

        mapper = new Mapper(lib + pack + ".isUpperCase(\"EcVT\")", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("false", value);

        mapper = new Mapper(lib + pack + ".isUpperCase(true)", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("false", value);

        mapper = new Mapper(lib + pack + ".isUpperCase(45)", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("false", value);
    }

    @Test
    void testStrings_isWhitespace() {
        Mapper mapper = new Mapper(lib + pack + ".isWhitespace(null)", new ArrayList<>(), new HashMap<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("false", value);

        mapper = new Mapper(lib + pack + ".isWhitespace(\"\")", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("true", value);

        mapper = new Mapper(lib + pack + ".isWhitespace(\"       \")", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("true", value);

        mapper = new Mapper(lib + pack + ".isWhitespace(\"   abc    \")", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("false", value);

        mapper = new Mapper(lib + pack + ".isWhitespace(true)", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("false", value);

        mapper = new Mapper(lib + pack + ".isWhitespace(45)", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("false", value);
    }

    @Test
    void testStrings_leftPad() {
        Mapper mapper = new Mapper(lib + pack + ".leftPad(null,3)", new ArrayList<>(), new HashMap<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("null", value);

        mapper = new Mapper(lib + pack + ".leftPad(\"\",3)", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("   ", value);

        mapper = new Mapper(lib + pack + ".leftPad(\"bat\",5)", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("  bat", value);

        mapper = new Mapper(lib + pack + ".leftPad(\"bat\",3)", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("bat", value);

        mapper = new Mapper(lib + pack + ".leftPad(\"bat\",-1)", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("bat", value);

        mapper = new Mapper(lib + pack + ".leftPad(45,3)", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals(" 45", value);

        mapper = new Mapper(lib + pack + ".leftPad(true,10)", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("      true", value);
    }

    @Test
    void testStrings_ordinalize() {
        Mapper mapper = new Mapper(lib + pack + ".ordinalize(1)", new ArrayList<>(), new HashMap<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("1st", value);

        mapper = new Mapper(lib + pack + ".ordinalize(2)", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("2nd", value);

        mapper = new Mapper(lib + pack + ".ordinalize(3)", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("3rd", value);

        mapper = new Mapper(lib + pack + ".ordinalize(111)", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("111th", value);

        mapper = new Mapper(lib + pack + ".ordinalize(22)", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("22nd", value);

        mapper = new Mapper(lib + pack + ".ordinalize(null)", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("null", value);
    }

    @Test
    void testStrings_pluralize() {
        Mapper mapper = new Mapper(lib + pack + ".pluralize(null)", new ArrayList<>(), new HashMap<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("null", value);

        mapper = new Mapper(lib + pack + ".pluralize(\"help\")", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("helps", value);

        mapper = new Mapper(lib + pack + ".pluralize(\"box\")", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("boxes", value);

        mapper = new Mapper(lib + pack + ".pluralize(\"monday\")", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("mondays", value);

        mapper = new Mapper(lib + pack + ".pluralize(\"mondy\")", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("mondies", value);
    }


    @Test
    void testStrings_prependIfMissing() {
        Mapper mapper = new Mapper(lib + pack + ".prependIfMissing(\"abc\", \"xyz\")", new ArrayList<>(), new HashMap<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("xyzabc", value);

        mapper = new Mapper(lib + pack + ".prependIfMissing(\"xyzabc\", \"xyz\")", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("xyzabc", value);

        mapper = new Mapper(lib + pack + ".prependIfMissing(null, \"\")", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("null", value);

        mapper = new Mapper(lib + pack + ".prependIfMissing(\"axyz\", \"xyz\")", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("xyzaxyz", value);

        mapper = new Mapper(lib + pack + ".prependIfMissing(\"\", \"xyz\")", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("xyz", value);
    }

    @Test
    void testStrings_repeat() {
        Mapper mapper = new Mapper(lib + pack + ".repeat(\"e\", 0)", new ArrayList<>(), new HashMap<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("", value);

        mapper = new Mapper(lib + pack + ".repeat(\"e\", 3)", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("eee", value);

        mapper = new Mapper(lib + pack + ".repeat(\"e\", -2)", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("", value);
    }


    @Test
    void testStrings_rightPad() {
        Mapper mapper = new Mapper(lib + pack + ".rightPad(null,3)", new ArrayList<>(), new HashMap<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("null", value);

        mapper = new Mapper(lib + pack + ".rightPad(\"\",3)", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("   ", value);

        mapper = new Mapper(lib + pack + ".rightPad(\"bat\",5)", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("bat  ", value);

        mapper = new Mapper(lib + pack + ".rightPad(\"bat\",3)", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("bat", value);

        mapper = new Mapper(lib + pack + ".rightPad(\"bat\",-1)", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("bat", value);

        mapper = new Mapper(lib + pack + ".rightPad(45,3)", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("45 ", value);

        mapper = new Mapper(lib + pack + ".rightPad(true,10)", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("true      ", value);
    }


    @Test
    void testStrings_singularize() {
        Mapper mapper = new Mapper(lib + pack + ".singularize(null)", new ArrayList<>(), new HashMap<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("null", value);

        mapper = new Mapper(lib + pack + ".singularize(\"helps\")", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("help", value);

        mapper = new Mapper(lib + pack + ".singularize(\"boxes\")", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("box", value);

        mapper = new Mapper(lib + pack + ".singularize(\"mondays\")", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("monday", value);

        mapper = new Mapper(lib + pack + ".singularize(\"mondies\")", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("mondy", value);
    }

    @Test
    void testStrings_substringAfter() {
        Mapper mapper = new Mapper(lib + pack + ".substringAfter(null, \"'\")", new ArrayList<>(), new HashMap<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("null", value);

        mapper = new Mapper(lib + pack + ".substringAfter(\"\", \"-\")", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("", value);

        mapper = new Mapper(lib + pack + ".substringAfter(\"abc\", \"a\")", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("bc", value);

        mapper = new Mapper(lib + pack + ".substringAfter(\"abc\", \"b\")", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("c", value);

        mapper = new Mapper(lib + pack + ".substringAfter(\"abcba\", \"b\")", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("cba", value);

        mapper = new Mapper(lib + pack + ".substringAfter(\"abc\", \"d\")", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("", value);

        mapper = new Mapper(lib + pack + ".substringAfter(\"abc\", \"\")", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("abc", value);

    }

    @Test
    void testStrings_substringAfterLast() {
        Mapper mapper = new Mapper(lib + pack + ".substringAfterLast(null, \"'\")", new ArrayList<>(), new HashMap<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("null", value);

        mapper = new Mapper(lib + pack + ".substringAfterLast(\"\", \"-\")", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("", value);

        mapper = new Mapper(lib + pack + ".substringAfterLast(\"abcaxy\", \"a\")", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("xy", value);

        mapper = new Mapper(lib + pack + ".substringAfterLast(\"abc\", \"b\")", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("c", value);

        mapper = new Mapper(lib + pack + ".substringAfterLast(\"abcba\", \"b\")", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("a", value);

        mapper = new Mapper(lib + pack + ".substringAfterLast(\"abc\", \"d\")", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("", value);

        mapper = new Mapper(lib + pack + ".substringAfterLast(\"abc\", \"\")", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("", value);

    }

    @Test
    void testStrings_substringBefore() {
        Mapper mapper = new Mapper(lib + pack + ".substringBefore(null, \"'\")", new ArrayList<>(), new HashMap<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("null", value);

        mapper = new Mapper(lib + pack + ".substringBefore(\"\", \"-\")", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("", value);

        mapper = new Mapper(lib + pack + ".substringBefore(\"abc\", \"a\")", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("", value);

        mapper = new Mapper(lib + pack + ".substringBefore(\"abc\", \"b\")", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("a", value);

        mapper = new Mapper(lib + pack + ".substringBefore(\"abcba\", \"b\")", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("a", value);

        mapper = new Mapper(lib + pack + ".substringBefore(\"abc\", \"d\")", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("", value);

        mapper = new Mapper(lib + pack + ".substringBefore(\"abc\", \"\")", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("", value);
    }

    @Test
    void testStrings_substringBeforeLast() {
        Mapper mapper = new Mapper(lib + pack + ".substringBeforeLast(null, \"'\")", new ArrayList<>(), new HashMap<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("null", value);

        mapper = new Mapper(lib + pack + ".substringBeforeLast(\"\", \"-\")", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("", value);

        mapper = new Mapper(lib + pack + ".substringBeforeLast(\"abc\", \"a\")", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("", value);

        mapper = new Mapper(lib + pack + ".substringBeforeLast(\"abc\", \"b\")", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("a", value);

        mapper = new Mapper(lib + pack + ".substringBeforeLast(\"abcba\", \"b\")", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("abc", value);

        mapper = new Mapper(lib + pack + ".substringBeforeLast(\"abc\", \"d\")", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("", value);

        mapper = new Mapper(lib + pack + ".substringBeforeLast(\"abc\", \"\")", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("abc", value);
    }

    @Test
    void testStrings_underscore() {
        Mapper mapper = new Mapper(lib + pack + ".underscore(\"customer\")", new ArrayList<>(), new HashMap<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("customer", value);

        mapper = new Mapper(lib + pack + ".underscore(\"customer-first-name\")", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("customer_first_name", value);

        mapper = new Mapper(lib + pack + ".underscore(\"customer NAME\")", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("customer_name", value);

        mapper = new Mapper(lib + pack + ".underscore(\"customerName\")", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("customer_name", value);

        mapper = new Mapper(lib + pack + ".underscore(null)", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("null", value);
    }

    @Test
    void testStrings_unwrap() {
        Mapper mapper = new Mapper(lib + pack + ".unwrap(null, \"\")", new ArrayList<>(), new HashMap<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("null", value);

        mapper = new Mapper(lib + pack + ".unwrap(\"'abc'\", \"'\")", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("abc", value);

        mapper = new Mapper(lib + pack + ".unwrap(\"AABabcBAA\", \"A\")", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("ABabcBA", value);

        mapper = new Mapper(lib + pack + ".unwrap(\"A\", \"#\")", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("A", value);

        mapper = new Mapper(lib + pack + ".unwrap(\"A#\", \"#\")", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("#A", value);
    }

    @Test
    void testStrings_withMaxSize() {
        Mapper mapper = new Mapper(lib + pack + ".withMaxSize(null, 10)", new ArrayList<>(), new HashMap<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("null", value);

        mapper = new Mapper(lib + pack + ".withMaxSize(\"123\", 10)", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("123", value);

        mapper = new Mapper(lib + pack + ".withMaxSize(\"123\", 3)", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("123", value);

        mapper = new Mapper(lib + pack + ".withMaxSize(\"123\", 2)", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("12", value);

        mapper = new Mapper(lib + pack + ".withMaxSize(\"\", 0)", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("", value);
    }

    @Test
    void testStrings_wrapIfMissing() {
        Mapper mapper = new Mapper(lib + pack + ".wrapIfMissing(null, \"'\")", new ArrayList<>(), new HashMap<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("null", value);

        mapper = new Mapper(lib + pack + ".wrapIfMissing(\"abc\", \"'\")", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("'abc'", value);

        mapper = new Mapper(lib + pack + ".wrapIfMissing(\"'abc'\", \"'\")", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("'abc'", value);

        mapper = new Mapper(lib + pack + ".wrapIfMissing(\"'abc\", \"'\")", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("'abc'", value);
    }

    @Test
    void testStrings_wrapWith() {
        Mapper mapper = new Mapper(lib + pack + ".wrapWith(null, \"'\")", new ArrayList<>(), new HashMap<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("null", value);

        mapper = new Mapper(lib + pack + ".wrapWith(\"abc\", \"'\")", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("'abc'", value);

        mapper = new Mapper(lib + pack + ".wrapWith(\"'abc\", \"'\")", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("''abc'", value);
    }

}
