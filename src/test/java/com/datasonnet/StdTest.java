package com.datasonnet;

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

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.datasonnet.document.DefaultDocument;
import com.datasonnet.document.Document;
import com.datasonnet.document.MediaTypes;
import com.datasonnet.util.TestResourceReader;
import org.json.JSONException;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URISyntaxException;

import static org.junit.jupiter.api.Assertions.*;

public class StdTest {

    @Test
    void testStdGet() throws IOException, URISyntaxException, JSONException {
        Mapper mapper = new Mapper(TestResourceReader.readFileAsString("stdGet.ds"));
        Document<String> response = mapper.transform(new DefaultDocument<>("{}", MediaTypes.APPLICATION_JSON));
        JSONAssert.assertEquals("{\"hidden\":\"HiddenMessage\",\"noHidden\":\"NONE\",\"obj\":{\"Hello\":\"World\"},\"nonExistent\":null,\"nonExistentD\":\"DefaultNonExistent\",\"a\":[1,2,3],\"b\":[9,8,7]}", response.getContent(), true);
    }

    @Test
    void testStdObjectValues() throws IOException, URISyntaxException, JSONException {
        Mapper mapper = new Mapper("std.objectValues(" + TestResourceReader.readFileAsString("stdObjectValues.ds") + ")");
        Document<String> response = mapper.transform(new DefaultDocument<>("{}", MediaTypes.APPLICATION_JSON));
        JSONAssert.assertEquals("[\"Hello\",{\"Hello\":\"World\"},[1,2,3]]", response.getContent(), true);
    }

    @Test
    void testStdObjectValuesAll() throws IOException, URISyntaxException, JSONException {
        Mapper mapper = new Mapper("std.objectValuesAll(" + TestResourceReader.readFileAsString("stdObjectValues.ds") + ")");
        Document<String> response = mapper.transform(new DefaultDocument<>("{}", MediaTypes.APPLICATION_JSON));
        JSONAssert.assertEquals("[\"Hello\",{\"Hello\":\"World\"},[1,2,3],\"HiddenMessage\"]", response.getContent(), true);
    }

    @Test
    void testStdReverse() throws IOException, URISyntaxException, JSONException {
        Mapper mapper = new Mapper("std.reverse([1,2,3])");
        Document<String> response = mapper.transform(new DefaultDocument<>("{}", MediaTypes.APPLICATION_JSON));
        JSONAssert.assertEquals("[3,2,1]", response.getContent(), true);
    }

    @Test
    void testStdSplitLimitR() throws IOException, URISyntaxException, JSONException {
        Mapper mapper = new Mapper("std.splitLimitR(\"testX1YsplitX1YrightX1Ytest2\", \"X1Y\", 2)");
        Document<String> response = mapper.transform(new DefaultDocument<>("{}", MediaTypes.APPLICATION_JSON));
        JSONAssert.assertEquals("[\"testX1Ysplit\",\"right\",\"test2\"]", response.getContent(), true);
    }

    @Test
    void testStdSlice() throws IOException, URISyntaxException, JSONException {
        Mapper mapper = new Mapper("std.slice([1, 2, 3, 4, 5, 6], 0, 4, 1)");
        Document<String> response = mapper.transform(new DefaultDocument<>("{}", MediaTypes.APPLICATION_JSON));
        JSONAssert.assertEquals("[ 1, 2, 3, 4 ]", response.getContent(), true);

        mapper = new Mapper("std.slice([1, 2, 3, 4, 5, 6], 1, 6, 2)");
        response = mapper.transform(new DefaultDocument<>("{}", MediaTypes.APPLICATION_JSON));
        JSONAssert.assertEquals("[ 2, 4, 6 ]", response.getContent(), true);

        mapper = new Mapper("std.slice(\"jsonnet\", 0, 4, 1)");
        response = mapper.transform(new DefaultDocument<>("{}", MediaTypes.APPLICATION_JSON));
        JSONAssert.assertEquals("\"json\"", response.getContent(), true);
    }

    @Test
    void testStdAny() throws IOException, URISyntaxException, JSONException {
        Mapper mapper = new Mapper("std.any([false, true, false])");
        Document<String> response = mapper.transform(new DefaultDocument<>("{}", MediaTypes.APPLICATION_JSON));
        assertEquals("true", response.getContent());

        mapper = new Mapper("std.any([false, false, false])");
        response = mapper.transform(new DefaultDocument<>("{}", MediaTypes.APPLICATION_JSON));
        assertEquals("false", response.getContent());

        mapper = new Mapper("std.any([])");
        response = mapper.transform(new DefaultDocument<>("{}", MediaTypes.APPLICATION_JSON));
        assertEquals("false", response.getContent());

        try {
            mapper = new Mapper("std.any([false, \"HELLO\", false])");
            response = mapper.transform(new DefaultDocument<>("{}", MediaTypes.APPLICATION_JSON));
            fail("This should fail with java.lang.IllegalArgumentException:");
        } catch (Exception e) {
            String msg = e.getMessage();
            assertTrue(msg != null && msg.contains("Array must contain only boolean values"));
        }
    }

    @Test
    void testStdAll() throws IOException, URISyntaxException, JSONException {
        Mapper mapper = new Mapper("std.all([false, true, false])");
        Document<String> response = mapper.transform(new DefaultDocument<>("{}", MediaTypes.APPLICATION_JSON));
        assertEquals("false", response.getContent());

        mapper = new Mapper("std.all([false, false, false])");
        response = mapper.transform(new DefaultDocument<>("{}", MediaTypes.APPLICATION_JSON));
        assertEquals("false", response.getContent());

        mapper = new Mapper("std.all([true, true, true])");
        response = mapper.transform(new DefaultDocument<>("{}", MediaTypes.APPLICATION_JSON));
        assertEquals("true", response.getContent());

        mapper = new Mapper("std.all([])");
        response = mapper.transform(new DefaultDocument<>("{}", MediaTypes.APPLICATION_JSON));
        assertEquals("true", response.getContent());

        try {
            mapper = new Mapper("std.all([false, \"HELLO\", false])");
            response = mapper.transform(new DefaultDocument<>("{}", MediaTypes.APPLICATION_JSON));
            fail("This should fail with java.lang.IllegalArgumentException:");
        } catch (Exception e) {
            String msg = e.getMessage();
            assertTrue(msg != null && msg.contains("Array must contain only boolean values"));
        }
    }

    @Test
    void testTrace() throws IOException, URISyntaxException, JSONException {
        ListAppender<ILoggingEvent> appender;
        Logger mapperLogger = (Logger) LoggerFactory.getLogger("DS_TRACE");
        appender = new ListAppender<>();
        appender.start();
        mapperLogger.setLevel(Level.ALL);
        mapperLogger.addAppender(appender);

        Mapper mapper = new Mapper("local myCond = false; { condition: std.trace(\"Condition is \" + myCond, myCond) }");
        Document<String> response = mapper.transform(new DefaultDocument<>("{}", MediaTypes.APPLICATION_JSON));
        JSONAssert.assertEquals("{\"condition\":false}", response.getContent(), false);

        assertNotNull(appender.list);
        assertTrue(appender.list.size() == 1);
        String message = appender.list.get(0).getFormattedMessage();
        assertEquals("Condition is false", message);
    }
}
