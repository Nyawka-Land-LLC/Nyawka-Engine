package org.nyawka_engine.core;

/**
 * RegisteredThreadCellState - is a class that represents the state of a registered thread in the GLThreadRegistry. 
 * It keeps track of whether the thread has a GL context and whether it is currently taken.
 */
public final class RegisteredThreadCellState {
    private volatile boolean hasGLContext = false;
    private volatile boolean isTaken = false;

    public RegisteredThreadCellState() {
    }

    @Getter
    public boolean hasGLContext() {
        return hasGLContext;
    }

    @Getter
    public boolean isTaken() {
        return isTaken;
    }

    @Setter
    public void setHasGLContext(boolean hasGLContext) {
        this.hasGLContext = hasGLContext;
    }

    @Setter
    public void setTaken(boolean taken) {
        isTaken = taken;
    }
}