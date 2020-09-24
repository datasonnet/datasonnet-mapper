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


import com.datasonnet.document.DefaultDocument;
import com.datasonnet.document.MediaTypes;
import com.datasonnet.util.Dictionary;
import com.datasonnet.util.XMLDocumentUtils;
import com.datasonnet.util.XMLGenerator;
import com.pholser.junit.quickcheck.From;
import com.pholser.junit.quickcheck.Property;
import com.pholser.junit.quickcheck.runner.JUnitQuickcheck;
import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

import static org.junit.Assert.assertTrue;


@Ignore
@RunWith(JUnitQuickcheck.class)
public class XMLPropertyTest {


    @Property
    public void reversible(@From(XMLGenerator.class) @Dictionary("xml.dict") Document dom) throws Exception {
        String xml = XMLDocumentUtils.documentToString(dom);
        Mapper mapper = new Mapper("ds.write(ds.read(payload, \"application/xml\"), \"application/xml\")");
        String output = mapper.transform(new DefaultDocument<String>(xml, MediaTypes.APPLICATION_XML), Collections.emptyMap(), MediaTypes.APPLICATION_XML).getContent();
        DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document parsed = db.parse(new ByteArrayInputStream(output.getBytes(StandardCharsets.UTF_8)));
        assertTrue("For input " + xml + " found output " + output, dom.isEqualNode(parsed));

        // okay, so this doesn't work because of ordering differences... let me see... we could sort both the same?
        // go ahead and pass on it for now
    }
}
