package com.datasonnet.debugger;

public class Breakpoint {

    private boolean enabled;
    private int line;

    public Breakpoint(int line) {
        this.line = line;
        enabled = true;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
