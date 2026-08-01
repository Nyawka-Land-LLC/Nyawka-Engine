package org.nyawka_engine.core;

import java.awt.image.BufferedImage;

public sealed abstract class Graphic permits Shader, ShaderEnhanced {
    private int width;
    private int height;
    private int x;
    private int y;
    protected final BufferedImage buffer;

    public Graphic(int width, int height, int x, int y) {
        this.width = width;
        this.height = height;
        this.x = x;
        this.y = y;
        this.buffer = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
    }

    @Getter
    public final int width() {
        return width;
    }

    @Getter
    public final int height() {
        return height;
    }

    @Getter
    public final int x() {
        return x;
    }

    @Getter
    public final int y() {
        return y;
    }

    @Setter
    public final int width(int width) {
        this.width = width;
        return this.width;
    }

    @Setter
    public final int height(int height) {
        this.height = height;
        return this.height;
    }

    @Setter
    public final int x(int x) {
        this.x = x;
        return this.x;
    }

    @Setter
    public final int y(int y) {
        this.y = y;
        return this.y;
    }

    @PaintsStuff
    public abstract int[] paint();
}
