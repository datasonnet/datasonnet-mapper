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
import static org.junit.Assert.*;

import java.util.HashMap;

public class YamlWriterTest {

    @Test
    void testYamlWriter() throws Exception {
        String data ="{\"message\":\"Hello World\",\"object\":{\"num\":3.14159,\"bool\":true,\"array\":[1,2]}}";
        DefaultDocument<?> doc = new DefaultDocument<>(data, MediaTypes.APPLICATION_JSON);

        String mapping = "/** DataSonnet\n" +
                "version=2.0\n" +
                "output application/x-yaml\n" +
                "input payload application/json\n" +
                "*/\n" +
                "payload";

        Mapper mapper = new Mapper(mapping);
        String mapped = mapper.transform(doc, new HashMap<>(), MediaTypes.APPLICATION_YAML).getContent();

        String expectedYaml ="---\n" +
                "message: \"Hello World\"\n" +
                "object:\n" +
                "  num: 3.14159\n" +
                "  bool: true\n" +
                "  array:\n" +
                "  - 1.0\n" +
                "  - 2.0\n";
        assertEquals(expectedYaml,mapped);
    }


    @Test
    void testYamlWriterRemoveHead() throws Exception {
        String data ="{\"message\":\"Hello World\",\"object\":{\"num\":3.14159,\"bool\":true,\"array\":[1,2]}}";
        DefaultDocument<?> doc = new DefaultDocument<>(data, MediaTypes.APPLICATION_JSON);

        String mapping = "/** DataSonnet\n" +
                "version=2.0\n" +
                "output application/x-yaml; RemoveHead=true\n" +
                "input payload application/json\n" +
                "*/\n" +
                "payload";

        Mapper mapper = new Mapper(mapping);
        String mapped = mapper.transform(doc, new HashMap<>(), MediaTypes.APPLICATION_YAML).getContent();

        String expectedYaml ="message: \"Hello World\"\n" +
                "object:\n" +
                "  num: 3.14159\n" +
                "  bool: true\n" +
                "  array:\n" +
                "  - 1.0\n" +
                "  - 2.0\n";
        assertEquals(expectedYaml,mapped);

        mapping = "/** DataSonnet\n" +
                "version=2.0\n" +
                "output application/x-yaml; RemoveHead=true\n" +
                "input payload application/json\n" +
                "*/\n" +
                "payload.message";

        mapper = new Mapper(mapping);
        mapped = mapper.transform(doc, new HashMap<>(), MediaTypes.APPLICATION_YAML).getContent();

        expectedYaml ="\"Hello World\"\n";
        assertEquals(expectedYaml,mapped);
    }

    @Test
    void testYamlWriterDisableQuotes() throws Exception {
        String data ="{\"message\":\"Hello World\",\"object\":{\"num\":3.14159,\"bool\":true,\"array\":[1,2]}}";
        DefaultDocument<?> doc = new DefaultDocument<>(data, MediaTypes.APPLICATION_JSON);

        String mapping = "/** DataSonnet\n" +
                "version=2.0\n" +
                "output application/x-yaml; DisableQuotes=true\n" +
                "input payload application/json\n" +
                "*/\n" +
                "payload";

        Mapper mapper = new Mapper(mapping);
        String mapped = mapper.transform(doc, new HashMap<>(), MediaTypes.APPLICATION_YAML).getContent();

        String expectedYaml ="---\nmessage: Hello World\n" +
                "object:\n" +
                "  num: 3.14159\n" +
                "  bool: true\n" +
                "  array:\n" +
                "  - 1.0\n" +
                "  - 2.0\n";
        assertEquals(expectedYaml,mapped);
    }

    @Test
    void testYamlToYaml() throws Exception {
        String data ="message: \"Hello World\"\n" +
                "object:\n" +
                "  num: 3.14159\n" +
                "  bool: true\n" +
                "  array:\n" +
                "  - 1.0\n" +
                "  - 2.0\n";
        DefaultDocument<?> doc = new DefaultDocument<>(data, MediaTypes.APPLICATION_YAML);

        String mapping = "/** DataSonnet\n" +
                "version=2.0\n" +
                "output application/x-yaml; RemoveHead=true\n" +
                "input payload application/yaml\n" +
                "*/\n" +
                "payload";

        Mapper mapper = new Mapper(mapping);
        String mapped = mapper.transform(doc, new HashMap<>(), MediaTypes.APPLICATION_YAML).getContent();

        String expectedYaml ="message: \"Hello World\"\n" +
                "object:\n" +
                "  num: 3.14159\n" +
                "  bool: true\n" +
                "  array:\n" +
                "  - 1.0\n" +
                "  - 2.0\n";
        assertEquals(expectedYaml,mapped);
    }

    @Test
    void testYamlWriterMultiple() throws Exception {
        String data =   "---\n" +
                "message: \"Hello World\"\n" +
                "---\n" +
                "test: \"Value\"\n";
        DefaultDocument<?> doc = new DefaultDocument<>(data, MediaTypes.APPLICATION_YAML);

        String mapping = "/** DataSonnet\n" +
                "version=2.0\n" +
                "output application/x-yaml\n" +
                "input payload application/x-yaml\n" +
                "*/\n" +
                "payload";

        Mapper mapper = new Mapper(mapping);
        String mapped = mapper.transform(doc, new HashMap<>(), MediaTypes.APPLICATION_YAML).getContent();

        String expectedYaml =  "---\n" +
                "message: \"Hello World\"\n" +
                "---\n" +
                "test: \"Value\"\n";
        assertEquals(expectedYaml, mapped);
    }

}
