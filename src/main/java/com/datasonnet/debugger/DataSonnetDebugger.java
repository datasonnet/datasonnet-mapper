package com.datasonnet.debugger;

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

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;

public class DataSonnetDebugger {

    private static DataSonnetDebugger DEBUGGER;

    private final ConcurrentMap<String, Breakpoint> breakpoints = new ConcurrentHashMap<>();

    private CountDownLatch latch;

    private boolean attached;

    public static DataSonnetDebugger getDebugger() {
        if (DEBUGGER == null) {
            DEBUGGER = new DataSonnetDebugger();
        }
        return DEBUGGER;
    }

    public void addBreakpoint(int line) {
        breakpoints.put(String.valueOf(line), new Breakpoint(line));
    }
    public Breakpoint getBreakpoint(int line) {
        return breakpoints.get(String.valueOf(line));
    }

    public void removeBreakpoint(int line) {
        breakpoints.remove(String.valueOf(line));
    }

    public void probeExpr(int line) {
        Breakpoint breakpoint = breakpoints.get(String.valueOf(line));
        if (breakpoint != null && breakpoint.isEnabled()) {
            latch = new CountDownLatch(1);
            try {
                latch.await();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            System.out.println("Resuming after await");
        }
    }

    public void resume() {
        if (latch != null && latch.getCount() > 0) {
            latch.countDown();
        }
    }

    public void attach() {
        attached = true;
    }
    public void detach() {
        attached = false;
        if (latch != null && latch.getCount() > 0) {
            latch.countDown();
        }
    }

    public boolean isAttached() {
        return attached;
    }
}
