package org.nyawka_engine.core;

public class OneInitReference<V> {
    private V value;
    private boolean initialized = false;

    public OneInitReference() {
    }

    @Getter
    public V getValue() {
        if (!initialized) {
            throw new IllegalStateException("Value has not been initialized yet.");
        }
        return value;
    }

    @Setter
    public void setValue(V value) {
        if (initialized) {
            throw new IllegalStateException("Value has already been initialized.");
        }
        this.value = value;
        this.initialized = true;
    }
}
