package org.nyawka_engine.core;

public interface GLCommand {
    void execute(RegisteredThread thread);

    String code();
}
