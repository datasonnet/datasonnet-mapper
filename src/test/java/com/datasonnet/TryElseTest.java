package com.datasonnet;

/*-
 * Copyright 2019-2022 the original author or authors.
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
import com.datasonnet.document.DefaultDocument;
import com.datasonnet.document.Document;
import com.datasonnet.document.MediaTypes;
import com.datasonnet.util.TestResourceReader;
import org.json.JSONException;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class TryElseTest {

    @Test
    void testTryElse() throws IOException, URISyntaxException, JSONException {
        Mapper mapper = new Mapper(TestResourceReader.readFileAsString("tryElse.ds"));
        String response = mapper.transform("{}");
        JSONAssert.assertEquals("{\"tryNonexistent\":\"OK\",\"tryChain\":\"OK\",\"tryNaN\":-1}", response, true);
    }

    @Disabled
    @Test
    void testTryElseObj() throws IOException, URISyntaxException, JSONException {
        Mapper mapper = new Mapper(TestResourceReader.readFileAsString("tryElseObj.ds"));
        String response = mapper.transform("{}");
        System.out.println("**** RESPONSE IS " + response);
        //JSONAssert.assertEquals("{\"tryNonexistent\":\"OK\",\"tryChain\":\"OK\",\"tryNaN\":-1}", response, true);
    }

    @Test
    void testDefault() throws IOException, URISyntaxException, JSONException {
        Mapper mapper = new Mapper(TestResourceReader.readFileAsString("default.ds"));
        String response = mapper.transform("{}");
        JSONAssert.assertEquals("{\"tryNonexistent\":\"OK\",\"tryChain\":\"OK\",\"tryNaN\":-1}", response, true);
    }

    @Test
    void testDefaultHeader() throws IOException, URISyntaxException, JSONException {
        Mapper mapper = new Mapper(TestResourceReader.readFileAsString("defaultHeader.ds"));
        String response = mapper.transform("{}");
        JSONAssert.assertEquals("{\"tryNonexistent\":\"OK\",\"tryObj\":{\"x\":\"OK\"},\"tryOverride\":\"OverrideOK\",\"tryElseOverride\":\"OverrideOK\"}", response, true);
    }

    @Test
    void testDefaultHeaderPayloadAsDocument() throws IOException, URISyntaxException, JSONException {
        Mapper mapper = new Mapper(TestResourceReader.readFileAsString("defaultHeader.ds"));
        
        Document<String>  response = mapper.transform(new DefaultDocument<>("{}", MediaTypes.APPLICATION_JSON));
        JSONAssert.assertEquals("{\"tryNonexistent\":\"OK\",\"tryObj\":{\"x\":\"OK\"},\"tryOverride\":\"OverrideOK\",\"tryElseOverride\":\"OverrideOK\"}", response.getContent(), true);
    }

}
