package com.datasonnet;

/*-
 * Copyright 2019-2021 the original author or authors.
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
import com.datasonnet.document.MediaTypes;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;

import java.util.HashMap;

public class YamlReaderTest {

    @Test
    void testYamlReader() throws Exception {
        String data =   "message: \"Hello World\"\n" +
                        "object: \n" +
                        "  num: 3.14159\n" +
                        "  bool: true\n" +
                        "  array: \n" +
                        "    - 1\n" +
                        "    - 2";
        DefaultDocument<?> doc = new DefaultDocument<>(data, MediaTypes.APPLICATION_YAML);

        String mapping = "/** DataSonnet\n" +
                "version=2.0\n" +
                "output application/json\n" +
                "input payload application/x-yaml\n" +
                "*/\n" +
                "payload";

        Mapper mapper = new Mapper(mapping);
        String mapped = mapper.transform(doc, new HashMap<>(), MediaTypes.APPLICATION_JSON).getContent();

        String expectedJson = "{\"message\":\"Hello World\",\"object\":{\"num\":3.14159,\"bool\":true,\"array\":[1,2]}}";
        JSONAssert.assertEquals(expectedJson, mapped, true);
    }

    @Test
    void testYamlReaderWithLine() throws Exception {
        String data =   "---\nmessage: \"Hello World\"\n" +
                "object: \n" +
                "  num: 3.14159\n" +
                "  bool: true\n" +
                "  array: \n" +
                "    - 1\n" +
                "    - 2";
        DefaultDocument<?> doc = new DefaultDocument<>(data, MediaTypes.APPLICATION_YAML);

        String mapping = "/** DataSonnet\n" +
                "version=2.0\n" +
                "output application/json\n" +
                "input payload application/x-yaml\n" +
                "*/\n" +
                "payload";

        Mapper mapper = new Mapper(mapping);
        String mapped = mapper.transform(doc, new HashMap<>(), MediaTypes.APPLICATION_JSON).getContent();

        String expectedJson = "{\"message\":\"Hello World\",\"object\":{\"num\":3.14159,\"bool\":true,\"array\":[1,2]}}";
        JSONAssert.assertEquals(expectedJson, mapped, true);
    }

    @Test
    void testYamlReaderMultiple() throws Exception {
        String data =   "---\n" +
                "message: \"Hello World\"\n" +
                "---\n" +
                "test: \"Value\"\n";
        DefaultDocument<?> doc = new DefaultDocument<>(data, MediaTypes.APPLICATION_YAML);

        String mapping = "/** DataSonnet\n" +
                "version=2.0\n" +
                "output application/json\n" +
                "input payload application/x-yaml\n" +
                "*/\n" +
                "payload";

        Mapper mapper = new Mapper(mapping);
        String mapped = mapper.transform(doc, new HashMap<>(), MediaTypes.APPLICATION_JSON).getContent();

        String expectedJson = "[{\"message\":\"Hello World\"},{\"test\":\"Value\"} ]";
        JSONAssert.assertEquals(expectedJson, mapped, true);
    }
}
