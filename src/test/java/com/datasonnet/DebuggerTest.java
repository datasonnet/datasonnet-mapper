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
import com.datasonnet.debugger.DataSonnetDebugger;
import com.datasonnet.document.DefaultDocument;
import com.datasonnet.document.Document;
import com.datasonnet.document.MediaTypes;
import com.datasonnet.util.TestResourceReader;
import org.json.JSONException;
<<<<<<< HEAD
=======
import org.junit.jupiter.api.Disabled;
>>>>>>> debugger
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URISyntaxException;

import static org.junit.jupiter.api.Assertions.*;

public class DebuggerTest {

    @Test
    @Disabled
    void testDebugger() throws IOException, URISyntaxException, JSONException {
        final DataSonnetDebugger debugger = DataSonnetDebugger.getDebugger();
        debugger.attach();
        //debugger.addBreakpoint(4);
        final Mapper mapper = new Mapper(TestResourceReader.readFileAsString("debug.ds"));
        Runnable resume = new Runnable() {
            @Override
            public void run() {
                while (true) {
                    System.out.println("Resume?");
                    try {
                        System.in.read();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    debugger.resume();
                }
            }
        };
        new Thread(resume).start();
        Document<String> response = mapper.transform(new DefaultDocument<>("{}", MediaTypes.APPLICATION_JSON));
    }

}
