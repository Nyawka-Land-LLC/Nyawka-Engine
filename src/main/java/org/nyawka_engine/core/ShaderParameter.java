package org.nyawka_engine.core;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL11.GL_LINEAR;
import static org.lwjgl.opengl.GL11.GL_NEAREST;
import static org.lwjgl.opengl.GL11.GL_RGBA;
import static org.lwjgl.opengl.GL11.GL_RGBA8;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_MAG_FILTER;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_MIN_FILTER;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_WRAP_S;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_WRAP_T;
import static org.lwjgl.opengl.GL11.GL_UNSIGNED_BYTE;
import static org.lwjgl.opengl.GL11.glBindTexture;
import static org.lwjgl.opengl.GL11.glGenTextures;
import static org.lwjgl.opengl.GL11.glTexImage2D;
import static org.lwjgl.opengl.GL11.glTexParameteri;
import static org.lwjgl.opengl.GL12.GL_CLAMP_TO_EDGE;
import static org.lwjgl.opengl.GL12.GL_TEXTURE_3D;
import static org.lwjgl.opengl.GL12.GL_TEXTURE_WRAP_R;
import static org.lwjgl.opengl.GL12.glTexImage3D;
import static org.lwjgl.opengl.GL13.GL_TEXTURE_CUBE_MAP;
import static org.lwjgl.opengl.GL13.GL_TEXTURE_CUBE_MAP_POSITIVE_X;
import static org.lwjgl.opengl.GL46.*;

/**
 * ShaderParameter - обертка для параметров шейдера с поддержкой различных типов.
 * Позволяет хранить и конвертировать параметры между типами: double, float, int, DoubleDouble и т.д.
 */
public class ShaderParameter {
    
    /**
     * Тип параметра шейдера
     */
    public enum ParameterType {
        FLOAT("float"),
        DOUBLE("double"),
        INT("int"),
        BOOL("bool"),
        DOUBLE_DOUBLE("doubleDouble"),  // Наш кастомный тип
        VEC2("vec2"),
        VEC3("vec3"),
        VEC4("vec4"),
        DVEC2("dvec2"),                 // double precision vectors
        DVEC3("dvec3"),
        DVEC4("dvec4"),
        SAMPLER2D("sampler2D"),
        SAMPLER3D("sampler3D"),
        SAMPLERCUBE("samplerCube");
        
        private final String glslType;
        
        ParameterType(String glslType) {
            this.glslType = glslType;
        }
        
        public String getGLSLType() {
            return glslType;
        }
    }
    
    private final String name;
    private final ParameterType type;
    private Object value;

    private int textureID = 0; // Для SAMPLER2D, SAMPLER3D и SAMPLERCUBE типов
    
    // Конструкторы для разных типов
    
    public ShaderParameter(String name, float value) {
        this.name = name;
        this.type = ParameterType.FLOAT;
        this.value = value;
    }
    
    public ShaderParameter(String name, double value) {
        this.name = name;
        this.type = ParameterType.DOUBLE;
        this.value = value;
    }
    
    public ShaderParameter(String name, int value) {
        this.name = name;
        this.type = ParameterType.INT;
        this.value = value;
    }
    
    public ShaderParameter(String name, boolean value) {
        this.name = name;
        this.type = ParameterType.BOOL;
        this.value = value;
    }
    
    public ShaderParameter(String name, DoubleDouble value) {
        this.name = name;
        this.type = ParameterType.DOUBLE_DOUBLE;
        this.value = new DoubleDouble(value);
    }
    
    public ShaderParameter(String name, float[] value) {
        this.name = name;
        if (value.length == 2) {
            this.type = ParameterType.VEC2;
        } else if (value.length == 3) {
            this.type = ParameterType.VEC3;
        } else if (value.length == 4) {
            this.type = ParameterType.VEC4;
        } else {
            throw new IllegalArgumentException("Invalid vector size: " + value.length);
        }
        this.value = value.clone();
    }
    
    public ShaderParameter(String name, double[] value) {
        this.name = name;
        if (value.length == 2) {
            this.type = ParameterType.DVEC2;
        } else if (value.length == 3) {
            this.type = ParameterType.DVEC3;
        } else if (value.length == 4) {
            this.type = ParameterType.DVEC4;
        } else {
            throw new IllegalArgumentException("Invalid vector size: " + value.length);
        }
        this.value = value.clone();
    }
    
    // general constructor for any type
    public ShaderParameter(String name, ParameterType type, Object value) {
        this.name = name;
        this.type = type;
        this.value = value;
    }
    
    // Геттеры
    
    public String getName() {
        return name;
    }
    
    public ParameterType getType() {
        return type;
    }
    
    public Object getValue() {
        return value;
    }
    
    public float getAsFloat() {
        if (value instanceof Float) {
            return (Float) value;
        } else if (value instanceof Double) {
            return ((Double) value).floatValue();
        } else if (value instanceof Integer) {
            return ((Integer) value).floatValue();
        } else if (value instanceof DoubleDouble) {
            return (float) ((DoubleDouble) value).toDouble();
        }
        throw new ClassCastException("Cannot convert " + value.getClass() + " to float");
    }
    
    public double getAsDouble() {
        if (value instanceof Double) {
            return (Double) value;
        } else if (value instanceof Float) {
            return ((Float) value).doubleValue();
        } else if (value instanceof Integer) {
            return ((Integer) value).doubleValue();
        } else if (value instanceof DoubleDouble) {
            return ((DoubleDouble) value).toDouble();
        }
        throw new ClassCastException("Cannot convert " + value.getClass() + " to double");
    }
    
    public int getAsInt() {
        if (value instanceof Integer) {
            return (Integer) value;
        } else if (value instanceof Float) {
            return ((Float) value).intValue();
        } else if (value instanceof Double) {
            return ((Double) value).intValue();
        }
        throw new ClassCastException("Cannot convert " + value.getClass() + " to int");
    }
    
    public boolean getAsBoolean() {
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        throw new ClassCastException("Cannot convert " + value.getClass() + " to boolean");
    }
    
    public DoubleDouble getAsDoubleDouble() {
        if (value instanceof DoubleDouble) {
            return new DoubleDouble((DoubleDouble) value);
        } else if (value instanceof Double) {
            return new DoubleDouble((Double) value);
        } else if (value instanceof Float) {
            return new DoubleDouble(((Float) value).doubleValue());
        }
        throw new ClassCastException("Cannot convert " + value.getClass() + " to DoubleDouble");
    }
    
    public float[] getAsVec2() {
        if (type == ParameterType.VEC2 && value instanceof float[]) {
            return ((float[]) value).clone();
        }
        throw new ClassCastException("Parameter is not VEC2");
    }
    
    public float[] getAsVec3() {
        if (type == ParameterType.VEC3 && value instanceof float[]) {
            return ((float[]) value).clone();
        }
        throw new ClassCastException("Parameter is not VEC3");
    }
    
    public float[] getAsVec4() {
        if (type == ParameterType.VEC4 && value instanceof float[]) {
            return ((float[]) value).clone();
        }
        throw new ClassCastException("Parameter is not VEC4");
    }
    
    public double[] getAsDVec2() {
        if (type == ParameterType.DVEC2 && value instanceof double[]) {
            return ((double[]) value).clone();
        }
        throw new ClassCastException("Parameter is not DVEC2");
    }
    
    public double[] getAsDVec3() {
        if (type == ParameterType.DVEC3 && value instanceof double[]) {
            return ((double[]) value).clone();
        }
        throw new ClassCastException("Parameter is not DVEC3");
    }
    
    public double[] getAsDVec4() {
        if (type == ParameterType.DVEC4 && value instanceof double[]) {
            return ((double[]) value).clone();
        }
        throw new ClassCastException("Parameter is not DVEC4");
    }

    public IntBuffer getAsSampler2DBuffer() {
        if (type != ParameterType.SAMPLER2D) {
            throw new IllegalStateException("Parameter is not a SAMPLER2D");
        }
        return (IntBuffer) (((Object[]) value)[0]);
    }

    public IntBuffer getAsSampler3DBuffer() {
        if (type != ParameterType.SAMPLER3D) {
            throw new IllegalStateException("Parameter is not a SAMPLER3D");
        }
        return (IntBuffer) (((Object[]) value)[0]);
    }

    public IntBuffer[] getAsSamplerCubeBuffers() {
        if (type != ParameterType.SAMPLERCUBE) {
            throw new IllegalStateException("Parameter is not a SAMPLERCUBE");
        }
        return (IntBuffer[]) (((Object[]) value)[0]);
    }

    public int getAsSampler2DTextureID() {
        if (type != ParameterType.SAMPLER2D) {
            throw new IllegalStateException("Parameter is not a SAMPLER2D");
        }

        Integer width = (Integer) (((Object[]) value)[1]);
        Integer height = (Integer) (((Object[]) value)[2]);

        if (textureID == 0) {
            textureID = glGenTextures();

            glBindTexture(GL_TEXTURE_2D, textureID);

            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);

            glTexImage2D(
                    GL_TEXTURE_2D,
                    0,
                    GL_RGBA8,
                    width,
                    height,
                    0,
                    GL_RGBA,
                    GL_UNSIGNED_BYTE,
                    getAsSampler2DBuffer()
            );

            glBindTexture(GL_TEXTURE_2D, 0);
        }

        return textureID;
    }

    public int getAsSampler3DTextureID() {
        if (type != ParameterType.SAMPLER3D) {
            throw new IllegalStateException("Parameter is not a SAMPLER3D");
        }

        Integer width = (Integer) (((Object[]) value)[1]);
        Integer height = (Integer) (((Object[]) value)[2]);
        Integer depth = (Integer) (((Object[]) value)[3]);

        if (textureID == 0) {
            textureID = glGenTextures();

            glBindTexture(GL_TEXTURE_3D, textureID);

            glTexParameteri(GL_TEXTURE_3D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
            glTexParameteri(GL_TEXTURE_3D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);

            glTexImage3D(
                    GL_TEXTURE_3D,
                    0,
                    GL_RGBA8,
                    width,
                    height,
                    depth,
                    0,
                    GL_RGBA,
                    GL_UNSIGNED_BYTE,
                    getAsSampler3DBuffer()
            );

            glBindTexture(GL_TEXTURE_3D, 0);
        }

        return textureID;
    }

    public int getAsSamplerCubeTextureID() {
        if (type != ParameterType.SAMPLERCUBE) {
            throw new IllegalStateException("Parameter is not a SAMPLERCUBE");
        }

        Integer size = (Integer) (((Object[]) value)[1]);

        IntBuffer[] faces = getAsSamplerCubeBuffers();

        if (textureID == 0) {
            textureID = glGenTextures();

            glBindTexture(GL_TEXTURE_CUBE_MAP, textureID);

            glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
            glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_MAG_FILTER, GL_LINEAR);

            glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
            glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
            glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_WRAP_R, GL_CLAMP_TO_EDGE);

            for (int i = 0; i < 6; i++) {
                glTexImage2D(
                        GL_TEXTURE_CUBE_MAP_POSITIVE_X + i,
                        0,
                        GL_RGBA8,
                        size,
                        size,
                        0,
                        GL_RGBA,
                        GL_UNSIGNED_BYTE,
                        faces[i]
                );
            }

            glBindTexture(GL_TEXTURE_CUBE_MAP, 0);
        }

        return textureID;
    }
    
    // Сеттеры
    
    public void setValue(float value) {
        if (type != ParameterType.FLOAT) {
            throw new IllegalStateException("Parameter type is " + type + ", not FLOAT");
        }
        this.value = value;
    }
    
    public void setValue(double value) {
        if (type == ParameterType.DOUBLE) {
            this.value = value;
        } else if (type == ParameterType.DOUBLE_DOUBLE) {
            this.value = new DoubleDouble(value);
        } else {
            throw new IllegalStateException("Parameter type is " + type + ", not DOUBLE or DOUBLE_DOUBLE");
        }
    }
    
    public void setValue(int value) {
        if (type != ParameterType.INT && type != ParameterType.SAMPLER2D && type != ParameterType.SAMPLER3D && type != ParameterType.SAMPLERCUBE) {
            throw new IllegalStateException("Parameter type is " + type + ", not INT");
        }
        this.value = value;
    }
    
    public void setValue(boolean value) {
        if (type != ParameterType.BOOL) {
            throw new IllegalStateException("Parameter type is " + type + ", not BOOL");
        }
        this.value = value;
    }
    
    public void setValue(DoubleDouble value) {
        if (type != ParameterType.DOUBLE_DOUBLE) {
            throw new IllegalStateException("Parameter type is " + type + ", not DOUBLE_DOUBLE");
        }
        this.value = new DoubleDouble(value);
    }

    public void setValue(IntBuffer value, int width, int height) {
        if (type != ParameterType.SAMPLER2D) {
            throw new IllegalStateException("Parameter type is " + type + ", not SAMPLER2D");
        }

        this.value = new Object[]{value, width, height};
    }

    public void setValue(IntBuffer value, int width, int height, int depth) {
        if (type != ParameterType.SAMPLER3D) {
            throw new IllegalStateException("Parameter type is " + type + ", not SAMPLER3D");
        }

        this.value = new Object[]{value, width, height, depth};
    }

    public void setValue(IntBuffer[] value, int size) {
        if (type != ParameterType.SAMPLERCUBE) {
            throw new IllegalStateException("Parameter type is " + type + ", not SAMPLERCUBE");
        }

        this.value = new Object[]{value, size};
    }
    
    public void setValue(float[] value) {
        if (value.length == 2 && type == ParameterType.VEC2) {
            this.value = value.clone();
        } else if (value.length == 3 && type == ParameterType.VEC3) {
            this.value = value.clone();
        } else if (value.length == 4 && type == ParameterType.VEC4) {
            this.value = value.clone();
        } else {
            throw new IllegalArgumentException("Vector size mismatch or type mismatch");
        }
    }
    
    public void setValue(double[] value) {
        if (value.length == 2 && type == ParameterType.DVEC2) {
            this.value = value.clone();
        } else if (value.length == 3 && type == ParameterType.DVEC3) {
            this.value = value.clone();
        } else if (value.length == 4 && type == ParameterType.DVEC4) {
            this.value = value.clone();
        } else {
            throw new IllegalArgumentException("Vector size mismatch or type mismatch");
        }
    }
    
    @Override
    public String toString() {
        return String.format("ShaderParameter{name='%s', type=%s, value=%s}", 
            name, type, valueToString());
    }
    
    private String valueToString() {
        if (value instanceof float[]) {
            float[] arr = (float[]) value;
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < arr.length; i++) {
                if (i > 0) sb.append(", ");
                sb.append(arr[i]);
            }
            sb.append("]");
            return sb.toString();
        } else if (value instanceof double[]) {
            double[] arr = (double[]) value;
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < arr.length; i++) {
                if (i > 0) sb.append(", ");
                sb.append(arr[i]);
            }
            sb.append("]");
            return sb.toString();
        }
        return String.valueOf(value);
    }
}