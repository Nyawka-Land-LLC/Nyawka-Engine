package org.nyawka_engine.core;

import java.nio.IntBuffer;
import java.util.HashMap;
import java.util.Map;
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
import static org.lwjgl.opengl.GL46.glUniform1d;
import static org.lwjgl.opengl.GL46.glUniform2d;
import static org.lwjgl.opengl.GL46.glUniform3d;
import static org.lwjgl.opengl.GL46.glUniform4d;
import static org.lwjgl.opengl.GL20.glUniform1i;
import static org.lwjgl.opengl.GL20.glUniform2f;
import static org.lwjgl.opengl.GL20.glUniform3f;
import static org.lwjgl.opengl.GL20.glUniform4f;
import static org.lwjgl.opengl.GL20.glUseProgram;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;
import static org.lwjgl.opengl.GL30.GL_COLOR_ATTACHMENT0;
import static org.lwjgl.opengl.GL30.GL_FRAMEBUFFER;
import static org.lwjgl.opengl.GL30.GL_FRAMEBUFFER_COMPLETE;
import static org.lwjgl.opengl.GL30.GL_TEXTURE_3D;
import static org.lwjgl.opengl.GL30.GL_TEXTURE_CUBE_MAP;
import static org.lwjgl.opengl.GL30.glActiveTexture;
import static org.lwjgl.opengl.GL30.GL_TEXTURE0;
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
 * ShaderEnhanced - расширенная версия класса Shader с поддержкой типизированных параметров.
 * 
 * Позволяет работать с:
 * - float, double, int, boolean
 * - DoubleDouble (двойная точность)
 * - Векторами (vec2, vec3, vec4 для float, dvec2, dvec3, dvec4 для double)
 * 
 * Пример использования:
 * <pre>
 * ShaderEnhanced shader = new ShaderEnhanced(width, height, x, y, vertexSrc, fragmentSrc);
 * 
 * // Добавление параметров
 * shader.setParameter("centerX", new DoubleDouble(-0.75));
 * shader.setParameter("centerY", new DoubleDouble(0.0));
 * shader.setParameter("scale", new DoubleDouble(3.0));
 * shader.setParameter("zoom", 2.5f);
 * shader.setParameter("iterations", 300);
 * shader.setParameter("time", 0.0);
 * 
 * // Получение параметров
 * DoubleDouble x = shader.getParameterAsDoubleDouble("centerX");
 * float zoom = shader.getParameterAsFloat("zoom");
 * </pre>
 */
public final class ShaderEnhanced extends Graphic implements ShaderProgram {

    private final Map<String, ShaderParameter> parameters = new HashMap<>();

    @MultithreadSystemComponent
    private final static Object lock = new Object();

    private final OneInitReference<Integer> glProgramId = new OneInitReference<>();

    private final String vertexSource;
    private final String fragmentSource;

    private boolean compiled = false;

    public ShaderEnhanced(int width, int height, int x, int y, String vertexSource, String fragmentSource) {
        super(width, height, x, y);
        this.vertexSource = vertexSource;
        this.fragmentSource = fragmentSource;
    }

    // ===== Методы установки параметров =====
    
    @Setter
    public void setParameter(String name, float value) {
        if (parameters.containsKey(name)) {
            parameters.get(name).setValue(value);
        } else {
            parameters.put(name, new ShaderParameter(name, value));
        }
    }
    
    @Setter
    public void setParameter(String name, double value) {
        if (parameters.containsKey(name)) {
            ShaderParameter param = parameters.get(name);
            if (param.getType() == ShaderParameter.ParameterType.DOUBLE ||
                param.getType() == ShaderParameter.ParameterType.DOUBLE_DOUBLE) {
                param.setValue(value);
            } else {
                throw new IllegalStateException("Parameter type mismatch for: " + name);
            }
        } else {
            // По умолчанию создаем как DOUBLE
            parameters.put(name, new ShaderParameter(name, value));
        }
    }
    
    @Setter
    public void setParameter(String name, int value) {
        if (parameters.containsKey(name)) {
            parameters.get(name).setValue(value);
        } else {
            parameters.put(name, new ShaderParameter(name, value));
        }
    }
    
    @Setter
    public void setParameter(String name, boolean value) {
        if (parameters.containsKey(name)) {
            parameters.get(name).setValue(value);
        } else {
            parameters.put(name, new ShaderParameter(name, value));
        }
    }

    @Setter
    public void setParameter(String name, IntBuffer value, int width, int height) {
        if (parameters.containsKey(name)) {
            parameters.get(name).setValue(value, width, height);
        } else {
            parameters.put(name, new ShaderParameter(name, ShaderParameter.ParameterType.SAMPLER2D, new Object[]{value, width, height}));
        }
    }
    
    @Setter
    public void setParameter(String name, IntBuffer value, int width, int height, int depth) {
        if (parameters.containsKey(name)) {
            parameters.get(name).setValue(value, width, height, depth);
        } else {
            parameters.put(name, new ShaderParameter(name, ShaderParameter.ParameterType.SAMPLER3D, new Object[]{value, width, height, depth}));
        }
    }

    @Setter
    public void setParameter(String name, IntBuffer[] value, int size) {
        if (parameters.containsKey(name)) {
            parameters.get(name).setValue(value, size);
        } else {
            parameters.put(name, new ShaderParameter(name, ShaderParameter.ParameterType.SAMPLERCUBE, new Object[]{value, size}));
        }
    }

    @Setter
    public void setParameter(String name, DoubleDouble value) {
        if (parameters.containsKey(name)) {
            parameters.get(name).setValue(value);
        } else {
            parameters.put(name, new ShaderParameter(name, value));
        }
    }
    
    @Setter
    public void setParameter(String name, float[] value) {
        if (parameters.containsKey(name)) {
            parameters.get(name).setValue(value);
        } else {
            parameters.put(name, new ShaderParameter(name, value));
        }
    }
    
    @Setter
    public void setParameter(String name, double[] value) {
        if (parameters.containsKey(name)) {
            parameters.get(name).setValue(value);
        } else {
            parameters.put(name, new ShaderParameter(name, value));
        }
    }
    
    // ===== Методы получения параметров =====
    
    @Getter
    public ShaderParameter getParameter(String name) {
        return parameters.getOrDefault(name, null);
    }
    
    @Getter
    public float getParameterAsFloat(String name) {
        ShaderParameter param = parameters.get(name);
        if (param == null) throw new IllegalArgumentException("Parameter not found: " + name);
        return param.getAsFloat();
    }
    
    @Getter
    public double getParameterAsDouble(String name) {
        ShaderParameter param = parameters.get(name);
        if (param == null) throw new IllegalArgumentException("Parameter not found: " + name);
        return param.getAsDouble();
    }
    
    @Getter
    public int getParameterAsInt(String name) {
        ShaderParameter param = parameters.get(name);
        if (param == null) throw new IllegalArgumentException("Parameter not found: " + name);
        return param.getAsInt();
    }
    
    @Getter
    public boolean getParameterAsBoolean(String name) {
        ShaderParameter param = parameters.get(name);
        if (param == null) throw new IllegalArgumentException("Parameter not found: " + name);
        return param.getAsBoolean();
    }
    
    @Getter
    public DoubleDouble getParameterAsDoubleDouble(String name) {
        ShaderParameter param = parameters.get(name);
        if (param == null) throw new IllegalArgumentException("Parameter not found: " + name);
        return param.getAsDoubleDouble();
    }
    
    @Getter
    public float[] getParameterAsVec2(String name) {
        ShaderParameter param = parameters.get(name);
        if (param == null) throw new IllegalArgumentException("Parameter not found: " + name);
        return param.getAsVec2();
    }
    
    @Getter
    public float[] getParameterAsVec3(String name) {
        ShaderParameter param = parameters.get(name);
        if (param == null) throw new IllegalArgumentException("Parameter not found: " + name);
        return param.getAsVec3();
    }
    
    @Getter
    public float[] getParameterAsVec4(String name) {
        ShaderParameter param = parameters.get(name);
        if (param == null) throw new IllegalArgumentException("Parameter not found: " + name);
        return param.getAsVec4();
    }

    /**
     * Paints the shader to a pixel array. 
     * This method requires an active OpenGL context and should be called from a thread that has been 
     * registered with the GLThreadRegistry.
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

            int textureUnit = 0;

            // Отправляем параметры в шейдер с учетом их типов
            for (Map.Entry<String, ShaderParameter> entry : parameters.entrySet()) {
                int location = glGetUniformLocation(glProgramId.getValue(), entry.getKey());

                if (location == -1)
                    continue;

                ShaderParameter param = entry.getValue();
                
                switch (param.getType()) {
                    case FLOAT:
                        glUniform1f(location, param.getAsFloat());
                        break;
                    case DOUBLE:
                        glUniform1d(location, param.getAsDouble());
                        break;
                    case DOUBLE_DOUBLE:
                        // Отправляем как double (более точно, чем float)
                        glUniform1d(location, param.getAsDoubleDouble().toDouble());
                        break;
                    case INT:
                        glUniform1i(location, param.getAsInt());
                        break;
                    case BOOL:
                        glUniform1i(location, param.getAsBoolean() ? 1 : 0);
                        break;
                    case VEC2:
                        float[] v2 = param.getAsVec2();
                        glUniform2f(location, v2[0], v2[1]);
                        break;
                    case VEC3:
                        float[] v3 = param.getAsVec3();
                        glUniform3f(location, v3[0], v3[1], v3[2]);
                        break;
                    case VEC4:
                        float[] v4 = param.getAsVec4();
                        glUniform4f(location, v4[0], v4[1], v4[2], v4[3]);
                        break;
                    // DVEC* типы требуют расширенный OpenGL и дополнительные функции
                    case DVEC2:
                        double[] dv2 = param.getAsDVec2();
                        glUniform2d(location, dv2[0], dv2[1]);
                        break;
                    case DVEC3:
                        double[] dv3 = param.getAsDVec3();
                        glUniform3d(location, dv3[0], dv3[1], dv3[2]);
                        break;
                    case DVEC4:
                        double[] dv4 = param.getAsDVec4();
                        glUniform4d(location, dv4[0], dv4[1], dv4[2], dv4[3]);
                        break;
                    case SAMPLER2D:
                        glActiveTexture(GL_TEXTURE0 + textureUnit);
                        glBindTexture(GL_TEXTURE_2D,
                                param.getAsSampler2DTextureID());
                        glUniform1i(location, textureUnit);
                        textureUnit++;
                        break;

                    case SAMPLER3D:
                        glActiveTexture(GL_TEXTURE0 + textureUnit);
                        glBindTexture(GL_TEXTURE_3D,
                                param.getAsSampler3DTextureID());
                        glUniform1i(location, textureUnit);
                        textureUnit++;
                        break;

                    case SAMPLERCUBE:
                        glActiveTexture(GL_TEXTURE0 + textureUnit);
                        glBindTexture(GL_TEXTURE_CUBE_MAP,
                                param.getAsSamplerCubeTextureID());
                        glUniform1i(location, textureUnit);
                        textureUnit++;
                        break;

                    default:
                        throw new IllegalStateException("Unsupported parameter type: " + param.getType());
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
                // Already initialized
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
        return "ShaderEnhanced(%s, %s)".formatted(vertexSource, fragmentSource);
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

    public Map<String, ShaderParameter> getParameters() {
        return parameters;
    }
}