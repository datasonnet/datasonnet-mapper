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

import com.datasonnet.debugger.DataSonnetDebugger;
import com.datasonnet.debugger.StoppedProgramContext;
import com.datasonnet.debugger.da.DataSonnetDebugListener;
import com.datasonnet.document.DefaultDocument;
import com.datasonnet.document.MediaTypes;
import com.datasonnet.util.TestResourceReader;
import org.json.JSONException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

public class DebuggerTest {
    @Test
    void testDebugger() throws IOException, URISyntaxException, JSONException {
        final CountDownLatch latch = new CountDownLatch(1);

        final String dsScript = TestResourceReader.readFileAsString("debug.ds");

        final DataSonnetDebugger debugger = DataSonnetDebugger.getDebugger();
        debugger.attach();
        debugger.addBreakpoint(5);
        debugger.setLineCount(dsScript.split("\\R").length);
        debugger.setDebuggerAdapter(new DataSonnetDebugListener() {
            @Override
            public void stopped(StoppedProgramContext stoppedProgramContext) {
                DataSonnetDebugger.getDebugger().detach();
                latch.countDown();
            }
        });

        Runnable runMap = new Runnable() {
            @Override
            public void run() {
                String camelFunctions = "local cml = { exchangeProperty(str): exchangeProperty[str], header(str): header[str], properties(str): properties[str] };\n";
                String dataSonnetScript = camelFunctions + dsScript;
                Mapper mapper = new Mapper(dataSonnetScript);
                mapper.transform(new DefaultDocument<>("{}", MediaTypes.APPLICATION_JSON));
            }
        };
        new Thread(runMap).start();

        try {
            latch.await(5L, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            fail("The debugger did not stop at the breakpoint");
        }

        StoppedProgramContext spc = debugger.getStoppedProgramContext();

        assertTrue(latch.getCount() == 0);
        assertNotNull(spc);
        assertNotNull(spc.getSourcePos());
        assertEquals(5, spc.getSourcePos().getLine());
    }

    @AfterAll
    public static void cleanUp() {
        DataSonnetDebugger.getDebugger().detach();
    }

    @AfterEach
    public void cleanUpEach() {
        DataSonnetDebugger.getDebugger().detach();
    }
}
