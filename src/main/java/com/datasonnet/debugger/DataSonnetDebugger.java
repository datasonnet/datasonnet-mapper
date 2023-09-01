package com.datasonnet.debugger;

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
