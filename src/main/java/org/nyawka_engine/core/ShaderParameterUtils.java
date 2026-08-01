package org.nyawka_engine.core;

import java.util.HashMap;
import java.util.Map;

/**
 * ShaderParameterUtils - утилиты для работы с ShaderParameter.
 * Предоставляет удобные методы для конверсии типов, валидации и логирования.
 */
public final class ShaderParameterUtils {
    
    /**
     * Конвертирует значение из одного типа в другой если это возможно.
     * 
     * @param value исходное значение
     * @param targetType целевой тип
     * @return конвертированное значение или null если конверсия невозможна
     */
    public static Object convertValue(Object value, ShaderParameter.ParameterType targetType) {
        if (value == null) return null;
        
        // Если тип совпадает, возвращаем как есть
        if (getTypeOf(value) == targetType) {
            return value;
        }
        
        switch (targetType) {
            case FLOAT:
                if (value instanceof Number) {
                    return ((Number) value).floatValue();
                }
                return null;
                
            case DOUBLE:
                if (value instanceof Number) {
                    return ((Number) value).doubleValue();
                } else if (value instanceof DoubleDouble) {
                    return ((DoubleDouble) value).toDouble();
                }
                return null;
                
            case INT:
                if (value instanceof Number) {
                    return ((Number) value).intValue();
                }
                return null;
                
            case BOOL:
                if (value instanceof Boolean) {
                    return value;
                } else if (value instanceof Number) {
                    return ((Number) value).intValue() != 0;
                }
                return null;
                
            case DOUBLE_DOUBLE:
                if (value instanceof DoubleDouble) {
                    return value;
                } else if (value instanceof Number) {
                    return new DoubleDouble(((Number) value).doubleValue());
                }
                return null;
                
            default:
                return null;
        }
    }
    
    /**
     * Определяет тип значения.
     */
    public static ShaderParameter.ParameterType getTypeOf(Object value) {
        if (value instanceof Float) {
            return ShaderParameter.ParameterType.FLOAT;
        } else if (value instanceof Double) {
            return ShaderParameter.ParameterType.DOUBLE;
        } else if (value instanceof Integer) {
            return ShaderParameter.ParameterType.INT;
        } else if (value instanceof Boolean) {
            return ShaderParameter.ParameterType.BOOL;
        } else if (value instanceof DoubleDouble) {
            return ShaderParameter.ParameterType.DOUBLE_DOUBLE;
        } else if (value instanceof float[]) {
            float[] arr = (float[]) value;
            if (arr.length == 2) return ShaderParameter.ParameterType.VEC2;
            if (arr.length == 3) return ShaderParameter.ParameterType.VEC3;
            if (arr.length == 4) return ShaderParameter.ParameterType.VEC4;
        } else if (value instanceof double[]) {
            double[] arr = (double[]) value;
            if (arr.length == 2) return ShaderParameter.ParameterType.DVEC2;
            if (arr.length == 3) return ShaderParameter.ParameterType.DVEC3;
            if (arr.length == 4) return ShaderParameter.ParameterType.DVEC4;
        }
        return null;
    }
    
    /**
     * Проверяет совместимость типов для конверсии.
     */
    public static boolean isTypeCompatible(ShaderParameter.ParameterType from, 
                                           ShaderParameter.ParameterType to) {
        // Скалярные типы в основном совместимы между собой
        if (isScalarType(from) && isScalarType(to)) {
            return true;
        }
        
        // Вектор типы совместимы, если совпадает размер и базовый тип
        if (isVectorType(from) && isVectorType(to)) {
            return getVectorSize(from) == getVectorSize(to);
        }
        
        return false;
    }
    
    /**
     * Проверяет является ли тип скалярным.
     */
    public static boolean isScalarType(ShaderParameter.ParameterType type) {
        return type == ShaderParameter.ParameterType.FLOAT ||
               type == ShaderParameter.ParameterType.DOUBLE ||
               type == ShaderParameter.ParameterType.INT ||
               type == ShaderParameter.ParameterType.BOOL ||
               type == ShaderParameter.ParameterType.DOUBLE_DOUBLE;
    }
    
    /**
     * Проверяет является ли тип векторным.
     */
    public static boolean isVectorType(ShaderParameter.ParameterType type) {
        return type == ShaderParameter.ParameterType.VEC2 ||
               type == ShaderParameter.ParameterType.VEC3 ||
               type == ShaderParameter.ParameterType.VEC4 ||
               type == ShaderParameter.ParameterType.DVEC2 ||
               type == ShaderParameter.ParameterType.DVEC3 ||
               type == ShaderParameter.ParameterType.DVEC4;
    }
    
    /**
     * Получает размер вектора.
     */
    public static int getVectorSize(ShaderParameter.ParameterType type) {
        switch (type) {
            case VEC2:
            case DVEC2:
                return 2;
            case VEC3:
            case DVEC3:
                return 3;
            case VEC4:
            case DVEC4:
                return 4;
            default:
                return 0;
        }
    }
    
    /**
     * Проверяет использует ли тип двойную точность.
     */
    public static boolean isDoublePrecision(ShaderParameter.ParameterType type) {
        return type == ShaderParameter.ParameterType.DOUBLE ||
               type == ShaderParameter.ParameterType.DOUBLE_DOUBLE ||
               type == ShaderParameter.ParameterType.DVEC2 ||
               type == ShaderParameter.ParameterType.DVEC3 ||
               type == ShaderParameter.ParameterType.DVEC4;
    }
    
    /**
     * Создает красивый вывод значения параметра.
     */
    public static String formatParameterValue(ShaderParameter param) {
        Object value = param.getValue();
        
        if (value == null) {
            return "null";
        }
        
        if (value instanceof float[]) {
            float[] arr = (float[]) value;
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < arr.length; i++) {
                if (i > 0) sb.append(", ");
                sb.append(String.format("%.6f", arr[i]));
            }
            sb.append("]");
            return sb.toString();
        }
        
        if (value instanceof double[]) {
            double[] arr = (double[]) value;
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < arr.length; i++) {
                if (i > 0) sb.append(", ");
                sb.append(String.format("%.15f", arr[i]));
            }
            sb.append("]");
            return sb.toString();
        }
        
        if (value instanceof DoubleDouble) {
            DoubleDouble dd = (DoubleDouble) value;
            return String.format("%.15f (DD)", dd.toDouble());
        }
        
        if (value instanceof Double) {
            return String.format("%.15f", (double) value);
        }
        
        if (value instanceof Float) {
            return String.format("%.6f", (float) value);
        }
        
        return value.toString();
    }
    
    /**
     * Создает строковое представление всех параметров.
     */
    public static String formatAllParameters(Map<String, ShaderParameter> parameters) {
        StringBuilder sb = new StringBuilder();
        sb.append("=== Shader Parameters ===\n");
        
        for (Map.Entry<String, ShaderParameter> entry : parameters.entrySet()) {
            ShaderParameter param = entry.getValue();
            sb.append(String.format("  %-15s : %-12s = %s\n",
                param.getName(),
                param.getType(),
                formatParameterValue(param)
            ));
        }
        
        return sb.toString();
    }
    
    /**
     * Проверяет и логирует несоответствия типов в параметрах.
     */
    public static void validateParameters(Map<String, ShaderParameter> parameters) {
        for (Map.Entry<String, ShaderParameter> entry : parameters.entrySet()) {
            ShaderParameter param = entry.getValue();
            Object value = param.getValue();
            ShaderParameter.ParameterType detectedType = getTypeOf(value);
            
            if (detectedType != param.getType()) {
                System.err.printf("Warning: Parameter '%s' type mismatch. " +
                    "Declared: %s, Actual: %s\n",
                    param.getName(),
                    param.getType(),
                    detectedType);
            }
        }
    }
    
    /**
     * Клонирует параметр с новым значением.
     */
    public static ShaderParameter cloneParameter(ShaderParameter original, Object newValue) {
        return new ShaderParameter(
            original.getName(),
            original.getType(),
            newValue
        );
    }
    
    /**
     * Копирует все параметры из одного шейдера в другой.
     */
    public static void copyParameters(ShaderEnhanced source, ShaderEnhanced target) {
        for (Map.Entry<String, ShaderParameter> entry : source.getParameters().entrySet()) {
            String name = entry.getKey();
            ShaderParameter param = entry.getValue();
            
            // Клонируем значение если оно массив
            Object value = param.getValue();
            if (value instanceof float[]) {
                value = ((float[]) value).clone();
            } else if (value instanceof double[]) {
                value = ((double[]) value).clone();
            } else if (value instanceof DoubleDouble) {
                value = new DoubleDouble((DoubleDouble) value);
            }
            
            target.getParameters().put(name, 
                new ShaderParameter(name, param.getType(), value));
        }
    }
    
    /**
     * Создает снимок всех параметров.
     */
    public static Map<String, ShaderParameter> snapshotParameters(
            Map<String, ShaderParameter> parameters) {
        Map<String, ShaderParameter> snapshot = new HashMap<>();
        
        for (Map.Entry<String, ShaderParameter> entry : parameters.entrySet()) {
            ShaderParameter original = entry.getValue();
            Object value = original.getValue();
            
            // Клонируем значение
            if (value instanceof float[]) {
                value = ((float[]) value).clone();
            } else if (value instanceof double[]) {
                value = ((double[]) value).clone();
            } else if (value instanceof DoubleDouble) {
                value = new DoubleDouble((DoubleDouble) value);
            }
            
            snapshot.put(entry.getKey(),
                new ShaderParameter(original.getName(), original.getType(), value));
        }
        
        return snapshot;
    }
    
    /**
     * Сравнивает два набора параметров и возвращает различия.
     */
    public static Map<String, String> compareParameters(
            Map<String, ShaderParameter> params1,
            Map<String, ShaderParameter> params2) {
        Map<String, String> differences = new HashMap<>();
        
        for (String name : params1.keySet()) {
            if (!params2.containsKey(name)) {
                differences.put(name, "Missing in second set");
                continue;
            }
            
            ShaderParameter p1 = params1.get(name);
            ShaderParameter p2 = params2.get(name);
            
            if (p1.getType() != p2.getType()) {
                differences.put(name, 
                    String.format("Type mismatch: %s vs %s", p1.getType(), p2.getType()));
                continue;
            }
            
            Object v1 = p1.getValue();
            Object v2 = p2.getValue();
            
            if (!valuesEqual(v1, v2)) {
                differences.put(name, 
                    String.format("Value mismatch: %s vs %s", v1, v2));
            }
        }
        
        for (String name : params2.keySet()) {
            if (!params1.containsKey(name)) {
                differences.put(name, "Missing in first set");
            }
        }
        
        return differences;
    }
    
    private static boolean valuesEqual(Object v1, Object v2) {
        if (v1 == null && v2 == null) return true;
        if (v1 == null || v2 == null) return false;
        
        if (v1 instanceof float[] && v2 instanceof float[]) {
            float[] a1 = (float[]) v1;
            float[] a2 = (float[]) v2;
            if (a1.length != a2.length) return false;
            for (int i = 0; i < a1.length; i++) {
                if (Math.abs(a1[i] - a2[i]) > 1e-6f) return false;
            }
            return true;
        }
        
        if (v1 instanceof double[] && v2 instanceof double[]) {
            double[] a1 = (double[]) v1;
            double[] a2 = (double[]) v2;
            if (a1.length != a2.length) return false;
            for (int i = 0; i < a1.length; i++) {
                if (Math.abs(a1[i] - a2[i]) > 1e-15) return false;
            }
            return true;
        }
        
        if (v1 instanceof Double && v2 instanceof Double) {
            return Math.abs((double) v1 - (double) v2) < 1e-15;
        }
        
        if (v1 instanceof Float && v2 instanceof Float) {
            return Math.abs((float) v1 - (float) v2) < 1e-6f;
        }
        
        return v1.equals(v2);
    }
}