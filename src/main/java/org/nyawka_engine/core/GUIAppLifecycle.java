package org.nyawka_engine.core;

import static org.lwjgl.glfw.GLFW.glfwDestroyWindow;
import static org.lwjgl.glfw.GLFW.glfwMakeContextCurrent;
import static org.lwjgl.glfw.GLFW.glfwTerminate;

public abstract class GUIAppLifecycle extends Window {
    private volatile long delta = 0;
    private volatile long time = System.nanoTime();
    private volatile boolean running = false;
    private final long glContextID;
    private final Thread lifecycle;

    public GUIAppLifecycle(int width, int height, int x, int y, String title, boolean undecorated, GLThreadRegistry gtr) {
        super(width, height, x, y, title, undecorated);
        this.glContextID = gtr.getSharedGLContext();
        lifecycle = new Thread(null, () -> {
            try {
                GLThreshold.bindContext(gtr.getSharedGLContext());
                while (running) {
                    delta = System.nanoTime() - time;
                    time = System.nanoTime();
                    Shader.tick(glContextID, () -> {
                        update(delta);
                        GUIAppLifecycle.this.__paint__(this.buffer);
                        GUIAppLifecycle.this.repaint();
                    });
                }
            } finally {
                endMessage();
                kill();
            }
        }, "Nyawka Engine program update cycle", 2048 * 2048);
    }

    public void start() {
        running = true;
        lifecycle.start();
    }

    public void kill() {
        running = false;
        lifecycle.interrupt();

        glfwMakeContextCurrent(0);

        glfwDestroyWindow(glContextID);

        glfwTerminate();
    }

    private static void endMessage() {
        System.out.println("Update cycle has been ended.");
    }

    public final int width() {
        return this.getFrame().getWidth() + 16;
    }

    public final int height() {
        return this.getFrame().getHeight() + 39;
    }

    protected abstract void update(long delta);
}
