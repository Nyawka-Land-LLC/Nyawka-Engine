package org.nyawka_engine.core;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.lwjgl.opengl.GL;

import static org.lwjgl.opengl.GL11.GL_FALSE;
import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL11.GL_NEAREST;
import static org.lwjgl.opengl.GL11.GL_RGBA;
import static org.lwjgl.opengl.GL11.GL_RGBA8;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_MAG_FILTER;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_MIN_FILTER;
import static org.lwjgl.opengl.GL11.GL_TRIANGLES;
import static org.lwjgl.opengl.GL11.GL_UNSIGNED_BYTE;
import static org.lwjgl.opengl.GL11.glBindTexture;
import static org.lwjgl.opengl.GL11.glDeleteTextures;
import static org.lwjgl.opengl.GL11.glDrawArrays;
import static org.lwjgl.opengl.GL11.glGenTextures;
import static org.lwjgl.opengl.GL11.glReadPixels;
import static org.lwjgl.opengl.GL11.glTexImage2D;
import static org.lwjgl.opengl.GL11.glTexParameteri;
import static org.lwjgl.opengl.GL11.glViewport;
import static org.lwjgl.opengl.GL15.GL_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.GL_STATIC_DRAW;
import static org.lwjgl.opengl.GL15.glBindBuffer;
import static org.lwjgl.opengl.GL15.glBufferData;
import static org.lwjgl.opengl.GL15.glDeleteBuffers;
import static org.lwjgl.opengl.GL15.glGenBuffers;
import static org.lwjgl.opengl.GL20.GL_COMPILE_STATUS;
import static org.lwjgl.opengl.GL20.GL_FRAGMENT_SHADER;
import static org.lwjgl.opengl.GL20.GL_LINK_STATUS;
import static org.lwjgl.opengl.GL20.GL_VERTEX_SHADER;
import static org.lwjgl.opengl.GL20.glAttachShader;
import static org.lwjgl.opengl.GL20.glCompileShader;
import static org.lwjgl.opengl.GL20.glCreateProgram;
import static org.lwjgl.opengl.GL20.glCreateShader;
import static org.lwjgl.opengl.GL20.glDeleteShader;
import static org.lwjgl.opengl.GL20.glDetachShader;
import static org.lwjgl.opengl.GL20.glDisableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glGetProgramInfoLog;
import static org.lwjgl.opengl.GL20.glGetProgrami;
import static org.lwjgl.opengl.GL20.glGetShaderInfoLog;
import static org.lwjgl.opengl.GL20.glGetShaderi;
import static org.lwjgl.opengl.GL20.glGetUniformLocation;
import static org.lwjgl.opengl.GL20.glLinkProgram;
import static org.lwjgl.opengl.GL20.glShaderSource;
import static org.lwjgl.opengl.GL20.glUniform1f;
import static org.lwjgl.opengl.GL20.glUseProgram;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;
import static org.lwjgl.opengl.GL30.GL_COLOR_ATTACHMENT0;
import static org.lwjgl.opengl.GL30.GL_FRAMEBUFFER;
import static org.lwjgl.opengl.GL30.GL_FRAMEBUFFER_COMPLETE;
import static org.lwjgl.opengl.GL30.glBindFramebuffer;
import static org.lwjgl.opengl.GL30.glBindVertexArray;
import static org.lwjgl.opengl.GL30.glCheckFramebufferStatus;
import static org.lwjgl.opengl.GL30.glDeleteFramebuffers;
import static org.lwjgl.opengl.GL30.glDeleteVertexArrays;
import static org.lwjgl.opengl.GL30.glFramebufferTexture2D;
import static org.lwjgl.opengl.GL30.glGenFramebuffers;
import static org.lwjgl.opengl.GL30.glGenVertexArrays;
import static org.lwjgl.glfw.GLFW.glfwGetCurrentContext;
import static org.lwjgl.glfw.GLFW.glfwPollEvents;
import static org.lwjgl.glfw.GLFW.glfwSwapBuffers;

/**
 * Shader - class, which implements all GLSL shader properties
 */
public final class Shader extends Graphic implements ShaderProgram {

    @GLTypeFloat
    private final Map<String, AtomicReference<String>> parameters = new HashMap<>();

    @MultithreadSystemComponent
    private final static Object lock = new Object();

    private final OneInitReference<Integer> glProgramId = new OneInitReference<>();

    private final String vertexSource;
    private final String fragmentSource;

    private boolean compiled = false;

    public Shader(int width, int height, int x, int y, String vertexSource, String fragmentSource) {
        super(width, height, x, y);
        this.vertexSource = vertexSource;
        this.fragmentSource = fragmentSource;
    }

    @Setter
    public void setParameter(String name, String value) {
        if (parameters.containsKey(name)) parameters.get(name).set(value);
        else parameters.put(name, new AtomicReference<>(value));
        
    }
    
    @Getter
    public String getParameter(String name) {
        if (!parameters.containsKey(name)) return "null";
        else return parameters.get(name).get();
    }

    /**
     * Paints the shader to a pixel array. 
     * This method requires an active OpenGL context and should be called from a thread that has been 
     * registered with the GLThreadRegistry.
     * 
     * It has a lot of low level OpenGL calls to set up a framebuffer, 
     * compile and use the shader program, 
     * draw a fullscreen quad, and read the resulting pixels into an array.
     */
    @Override
    @GLContextExclusive
    @PaintsStuff
    public int[] paint() {
        if (glfwGetCurrentContext() == 0)
            throw new IllegalStateException("No current context");

        GL.getCapabilities();

        synchronized (lock) {
            if (!compiled) {
                return new int[0];
            }

            // ---------- Framebuffer ----------
            int fbo = glGenFramebuffers();
            glBindFramebuffer(GL_FRAMEBUFFER, fbo);

            int texture = glGenTextures();
            glBindTexture(GL_TEXTURE_2D, texture);

            glTexImage2D(
                    GL_TEXTURE_2D,
                    0,
                    GL_RGBA8,
                    width(),
                    height(),
                    0,
                    GL_RGBA,
                    GL_UNSIGNED_BYTE,
                    0
            );

            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);

            glFramebufferTexture2D(
                    GL_FRAMEBUFFER,
                    GL_COLOR_ATTACHMENT0,
                    GL_TEXTURE_2D,
                    texture,
                    0
            );

            if (glCheckFramebufferStatus(GL_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE) {
                throw new IllegalStateException("Framebuffer is incomplete.");
            }

            // ---------- Viewport ----------
            glViewport(0, 0, width(), height());

            // ---------- Shader ----------
            glUseProgram(glProgramId.getValue());

            for (Map.Entry<String, AtomicReference<String>> entry : parameters.entrySet()) {
                int location = glGetUniformLocation(glProgramId.getValue(), entry.getKey());

                if (location == -1)
                    continue;

                String value = entry.getValue().get();

                try {
                    glUniform1f(location, Float.parseFloat(value));
                } catch (NumberFormatException ignored) {
                    // Пока поддерживаются только float.
                }
            }

            // ---------- Fullscreen quad ----------
            float[] vertices = {
                    -1f, -1f,
                    1f, -1f,
                    1f,  1f,

                    -1f, -1f,
                    1f,  1f,
                    -1f,  1f
            };

            int vao = glGenVertexArrays();
            int vbo = glGenBuffers();

            glBindVertexArray(vao);

            glBindBuffer(GL_ARRAY_BUFFER, vbo);
            glBufferData(GL_ARRAY_BUFFER, vertices, GL_STATIC_DRAW);

            glVertexAttribPointer(0, 2, GL_FLOAT, false, 2 * Float.BYTES, 0);
            glEnableVertexAttribArray(0);

            glDrawArrays(GL_TRIANGLES, 0, 6);

            // ---------- Read pixels ----------
            int[] pixels = new int[width() * height()];

            glReadPixels(
                    0,
                    0,
                    width(),
                    height(),
                    GL_RGBA,
                    GL_UNSIGNED_BYTE,
                    pixels
            );

            // ---------- Cleanup ----------
            glDisableVertexAttribArray(0);

            glBindBuffer(GL_ARRAY_BUFFER, 0);
            glBindVertexArray(0);

            glDeleteBuffers(vbo);
            glDeleteVertexArrays(vao);

            glUseProgram(0);

            glBindFramebuffer(GL_FRAMEBUFFER, 0);

            glDeleteTextures(texture);
            glDeleteFramebuffers(fbo);

            return pixels;
        }
    }
        
    @Override
    @GLContextExclusive
    public boolean compile(GLThreadRegistry registry) {
        if (glfwGetCurrentContext() == 0) {
            return true;
        }

        if (GL.getCapabilities() == null) {
            GL.createCapabilities();
        } 

        synchronized (lock) {
            int vertexShader = glCreateShader(GL_VERTEX_SHADER);
            glShaderSource(vertexShader, vertexSource);
            glCompileShader(vertexShader);
            checkShader(vertexShader, "Vertex");

            int fragmentShader = glCreateShader(GL_FRAGMENT_SHADER);
            glShaderSource(fragmentShader, fragmentSource);
            glCompileShader(fragmentShader);
            checkShader(fragmentShader, "Fragment");

            try {
                glProgramId.setValue(glCreateProgram());
            } catch (IllegalStateException e) {

            }

            glAttachShader(glProgramId.getValue(), vertexShader);
            glAttachShader(glProgramId.getValue(), fragmentShader);

            glLinkProgram(glProgramId.getValue());

            if (glGetProgrami(glProgramId.getValue(), GL_LINK_STATUS) == GL_FALSE) {
                throw new IllegalStateException(glGetProgramInfoLog(glProgramId.getValue()));
            }

            glDetachShader(glProgramId.getValue(), vertexShader);
            glDetachShader(glProgramId.getValue(), fragmentShader);

            glDeleteShader(vertexShader);
            glDeleteShader(fragmentShader);

            compiled = true;

            tick(registry.getSharedGLContext(), () -> {});

            return false;
        }
    }

    final static void tick(long ctx, Runnable upd) {
        glfwPollEvents();

        upd.run();

        glfwSwapBuffers(ctx);
    }

    @Override
    public String toString() {
        return "Shader(%s, %s)".formatted(vertexSource, fragmentSource);
    }

    private static void checkShader(int shader, String type) {
        int status = glGetShaderi(shader, GL_COMPILE_STATUS);

        if (status == GL_FALSE) {
            String log = glGetShaderInfoLog(shader);

            throw new IllegalStateException(
                type + " shader compilation failed:\n" + log
            );
        }
    }

    public Map<String, AtomicReference<String>> getParameters() {
        return parameters;
    }
}
