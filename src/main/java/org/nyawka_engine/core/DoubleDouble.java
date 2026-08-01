package org.nyawka_engine.core;

/**
 * Класс для работы с удвоенной точностью (DoubleDouble precision).
 * Представляет число как сумму двух double: x = hi + lo, где lo << hi
 */
public class DoubleDouble {
    
    public double hi;
    public double lo;
    
    public DoubleDouble() {
        this.hi = 0.0;
        this.lo = 0.0;
    }
    
    public DoubleDouble(double value) {
        this.hi = value;
        this.lo = 0.0;
    }
    
    public DoubleDouble(double hi, double lo) {
        this.hi = hi;
        this.lo = lo;
    }
    
    public DoubleDouble(DoubleDouble other) {
        this.hi = other.hi;
        this.lo = other.lo;
    }
    
    /**
     * Нормализирует число, перемещая значимые биты из lo в hi
     */
    public DoubleDouble normalize() {
        double s = this.hi + this.lo;
        double e = this.lo - (s - this.hi);
        this.hi = s;
        this.lo = e;
        return this;
    }
    
    /**
     * Сложение с double
     */
    public DoubleDouble add(double y) {
        double s = this.hi + y;
        double e = (this.hi - s) + y;
        return new DoubleDouble(s, this.lo + e).normalize();
    }
    
    /**
     * Сложение двух DoubleDouble
     */
    public DoubleDouble add(DoubleDouble y) {
        double s = this.hi + y.hi;
        double e = (this.hi - s) + y.hi;
        double t = this.lo + y.lo;
        e += t;
        return new DoubleDouble(s, e).normalize();
    }
    
    /**
     * Вычитание double
     */
    public DoubleDouble subtract(double y) {
        double s = this.hi - y;
        double e = (this.hi - s) - y;
        return new DoubleDouble(s, this.lo + e).normalize();
    }
    
    /**
     * Вычитание двух DoubleDouble
     */
    public DoubleDouble subtract(DoubleDouble y) {
        double s = this.hi - y.hi;
        double e = (this.hi - s) - y.hi;
        double t = this.lo - y.lo;
        e += t;
        return new DoubleDouble(s, e).normalize();
    }
    
    /**
     * Умножение на double (Knuth)
     */
    public DoubleDouble multiply(double y) {
        double p = this.hi * y;
        double e = (this.hi * y - p) + this.lo * y;
        return new DoubleDouble(p, e).normalize();
    }
    
    /**
     * Умножение двух DoubleDouble (Knuth)
     */
    public DoubleDouble multiply(DoubleDouble y) {
        double p = this.hi * y.hi;
        double e = (this.hi * y.hi - p) + this.hi * y.lo + this.lo * y.hi + this.lo * y.lo;
        return new DoubleDouble(p, e).normalize();
    }
    
    /**
     * Деление на double
     */
    public DoubleDouble divide(double y) {
        double q = this.hi / y;
        double e = ((this.hi - q * y) + this.lo) / y;
        return new DoubleDouble(q, e).normalize();
    }
    
    /**
     * Деление двух DoubleDouble
     */
    public DoubleDouble divide(DoubleDouble y) {
        double q = this.hi / y.hi;
        DoubleDouble r = this.subtract(y.multiply(q));
        double e = r.hi / y.hi;
        return new DoubleDouble(q, e).normalize();
    }
    
    /**
     * Квадрат числа
     */
    public DoubleDouble square() {
        double p = this.hi * this.hi;
        double e = (this.hi * this.hi - p) + 2.0 * this.hi * this.lo + this.lo * this.lo;
        return new DoubleDouble(p, e).normalize();
    }
    
    /**
     * Скалярное произведение: x*y + z
     */
    public static DoubleDouble fma(DoubleDouble x, DoubleDouble y, DoubleDouble z) {
        return x.multiply(y).add(z);
    }
    
    /**
     * Абсолютное значение
     */
    public DoubleDouble abs() {
        if (this.hi < 0.0) {
            return new DoubleDouble(-this.hi, -this.lo);
        }
        return new DoubleDouble(this.hi, this.lo);
    }
    
    /**
     * Преобразование в обычный double
     */
    public double toDouble() {
        return this.hi + this.lo;
    }
    
    /**
     * Сравнение с double
     */
    public boolean greaterThan(double y) {
        return this.hi > y || (this.hi == y && this.lo > 0.0);
    }
    
    /**
     * Сравнение с DoubleDouble
     */
    public boolean greaterThan(DoubleDouble y) {
        return this.hi > y.hi || (this.hi == y.hi && this.lo > y.lo);
    }
    
    /**
     * Равенство с double
     */
    public boolean equals(double y) {
        return this.hi == y && this.lo == 0.0;
    }
    
    @Override
    public String toString() {
        return String.format("DD(%.17e + %.17e)", hi, lo);
    }
}