package com.datasonnet;

import com.datasonnet.document.StringDocument;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Collections;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

public class RandomTest {

    @ParameterizedTest
    @MethodSource("numbersProvider")
    void testRandomNumbers(String jsonnet, Number min, Number max) throws Exception {
        Mapper mapper = new Mapper(jsonnet);
        Number result = NumberFormat.getInstance().parse(mapper.transform("{}"));
        assertTrue(result.doubleValue() >= min.doubleValue() && result.doubleValue() <= max.doubleValue());
    }

    static Stream<Object[]> numbersProvider() {
        return Stream.of(
                new Object[]{"DS.Random.randomInt(0,10)", new Integer(0), new Integer(10)},
                new Object[]{"DS.Random.randomInt()", new Integer(Integer.MIN_VALUE), new Integer(Integer.MAX_VALUE)},
                new Object[]{"DS.Random.randomDouble(0,10)", new Double(0d), new Double(10d)},
                new Object[]{"DS.Random.randomDouble()", new Double(-Double.MAX_VALUE), new Double(Double.MAX_VALUE)}
        );
    }

    @Test
    void testStrings() throws Exception {
        Pattern numeric = Pattern.compile("^[0-9]+$");
        Pattern alpha = Pattern.compile("^[a-zA-Z]+$");
        Pattern alphaNumeric = Pattern.compile("^[a-zA-Z0-9]+$");

        Mapper mapper = new Mapper("DS.Random.randomString(10)");
        String str = mapper.transform(new StringDocument("{}", "application/json"),
                Collections.emptyMap(),
                "text/plain").getContentsAsString();
        assertNotNull(str);
        assertTrue(str.length() == 10);

        mapper = new Mapper("DS.Random.randomString(10, true, false, false)");
        str = mapper.transform(new StringDocument("{}", "application/json"),
                Collections.emptyMap(),
                "text/plain").getContentsAsString();
        assertNotNull(str);
        assertTrue(str.length() == 10);
        assertTrue(alpha.matcher(str).matches());
        assertFalse(numeric.matcher(str).matches());

        mapper = new Mapper("DS.Random.randomString(10, false, true, false)");
        str = mapper.transform(new StringDocument("{}", "application/json"),
                Collections.emptyMap(),
                "text/plain").getContentsAsString();
        assertNotNull(str);
        assertTrue(str.length() == 10);
        assertTrue(numeric.matcher(str).matches());
        assertFalse(alpha.matcher(str).matches());

        mapper = new Mapper("DS.Random.randomString(10, true, true, false)");
        str = mapper.transform(new StringDocument("{}", "application/json"),
                Collections.emptyMap(),
                "text/plain").getContentsAsString();
        assertNotNull(str);
        assertTrue(str.length() == 10);
        assertTrue(alphaNumeric.matcher(str).matches());

        mapper = new Mapper("DS.Random.randomString(10, false, false, true)");
        str = mapper.transform(new StringDocument("{}", "application/json"),
                Collections.emptyMap(),
                "text/plain").getContentsAsString();
        assertNotNull(str);
        assertTrue(str.length() == 10);
        assertFalse(alphaNumeric.matcher(str).matches());
    }
}
