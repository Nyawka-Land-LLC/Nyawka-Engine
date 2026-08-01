package org.nyawka_engine.core;

import static org.lwjgl.glfw.GLFW.glfwCreateWindow;
import static org.lwjgl.glfw.GLFW.glfwMakeContextCurrent;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL;

public class GLThreshold {

    private static final Object CONTEXT_CREATION_LOCK = new Object();

    public static long bindContext(long contextID) {
        synchronized (CONTEXT_CREATION_LOCK) {
            GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE);
            long sharedWindow = glfwCreateWindow(1, 1, "", 0, contextID);

            if (sharedWindow == 0)
                throw new IllegalStateException("Cannot create shared window");

            glfwMakeContextCurrent(sharedWindow);


            GL.createCapabilities();


            return sharedWindow;
        }
    }
}