package org.nyawka_engine.examples;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.concurrent.atomic.AtomicReference;

import org.nyawka_engine.core.DoubleDouble;
import org.nyawka_engine.core.GLThreadRegistry;
import org.nyawka_engine.core.GUIAppLifecycle;
import org.nyawka_engine.core.ShaderEnhanced;

public class Example1 {

    private static final AtomicReference<Double> x = new AtomicReference<Double>(-0.75);
    private static final AtomicReference<Double> y = new AtomicReference<Double>(0.0); // NaN
    private static final AtomicReference<Double> scale = new AtomicReference<Double>(1.0); // NaN

    public static void main(String[] args) {
        GLThreadRegistry gtr = new GLThreadRegistry();

        // Используем DoubleDouble для параметров шейдера
        ShaderEnhanced shader = new ShaderEnhanced(2560, 1440, 0, 0, """
                #version 460 core

                layout (location = 0) in vec2 position;

                out vec2 uv;

                void main() {
                    uv = position * 0.5 + 0.5;

                    gl_Position = vec4(position, 0.0, 1.0);
                }
                """, 
                """
#version 460 core
#extension GL_ARB_gpu_shader_fp64 : require

out vec4 FragColor;

uniform float width;
uniform float height;

uniform double centerX;
uniform double centerY;

uniform double scale;

vec3 hsv2rgb(vec3 c) {
    vec4 K = vec4(
        1.0,
        2.0 / 3.0,
        1.0 / 3.0,
        3.0
    );

    vec3 p = abs(fract(c.xxx + K.xyz) * 6.0 - K.www);

    return c.z * mix(
        K.xxx,
        clamp(p - K.xxx, 0.0, 1.0),
        c.y
    );
}

void main() {

    // координаты пикселя относительно центра
    vec2 uv = gl_FragCoord.xy / vec2(width, height);

    // перевод в комплексную плоскость
    double aspect = double(width) / double(height);

    double x = (double(uv.x) - 0.5) * scale * aspect + centerX;
    double y = (double(uv.y) - 0.5) * scale + centerY;

    if (x > 100000 && y > 100000) {
        FragColor = vec4(0.0, 1.0, 0.0, 1.0);
        return;
    }

    // z = x + yi
    double zx = 0.0LF;
    double zy = 0.0LF;

    int iterations = 300;
    int i = 0;

    while (i < iterations) {

        double newZx = zx * zx * zx * zx - 6 * zx * zx * zy * zy + zy * zy * zy * zy + x;
        double newZy = 4 * zx * zx * zx * zy - 4 * zx * zy * zy * zy + y;

        if (newZx * newZx + newZy * newZy > 14000) {
            break;
        }

        zx = newZx;
        zy = newZy;

        i++;
    }


    if(i == iterations) {
        // внутри множества
        FragColor = vec4(0.0, 0.0, 0.0, 1.0);
    }
    else {

        double smoothed = double(i)
    - double(log2(log2(float(zx*zx + zy*zy))/2)/2);

        double huer = sin(float(smoothed / 100)) / 2.7;
        double hue = huer * huer;

        double v = 1 - hue * 2.7 * 2.7;

        vec3 color = hsv2rgb(
            vec3(
                float(hue) + 0.6,
                float(hue) * 2.7 * 2.7,
                float(v)
            )
        );

        FragColor = vec4(color, 1.0);
    }
}
                """);

        // Инициализируем с DoubleDouble параметрами
        DoubleDouble centerX = new DoubleDouble(-0.75);
        DoubleDouble centerY = new DoubleDouble(0.0);
        DoubleDouble scale = new DoubleDouble(3.0);

        shader.setParameter("width", (float) shader.width());
        shader.setParameter("height", (float) shader.height());
        shader.setParameter("centerX", centerX.toDouble());
        shader.setParameter("centerY", centerY.toDouble());
        shader.setParameter("scale", scale.toDouble());

        gtr.consumeShader(shader);

        GUIAppLifecycle gal = new GUIAppLifecycle(1920, 1080, 50, 50, "Example DoubleDouble", false, gtr) {

            

            @Override
            protected void update(long delta) {
                shader.setParameter("width", (float) this.width());
                shader.setParameter("height", (float) this.height());
                shader.width(this.getFrame().getWidth());
                shader.height(this.getFrame().getHeight());

                // for (Map.Entry<String, ShaderParameter> entry : shader.getParameters().entrySet()) {
                //     System.out.println(entry.getKey() + ": " + entry.getValue());
                // }

            }

            @Override
            protected void __paint__(BufferedImage buffer) {
                writePixels(buffer, 0, 0, this.getFrame().getWidth(), this.getFrame().getHeight(), shader.paint());
                Graphics2D g2d = buffer.createGraphics();
                g2d.setColor(Color.BLACK);
                g2d.fillRect(0, 0, 300, 100);
                g2d.setColor(Color.WHITE);
                g2d.setFont(new Font("System", Font.PLAIN, 12));
                g2d.drawString("X: " + Example1.x.get(), 10, 20);
                g2d.drawString("Y: " + Example1.y.get(), 10, 40);
                g2d.drawString("Scale: " + Example1.scale.get(), 10, 60);
                g2d.dispose();
            }
            
        };

        // MouseAdapter с поддержкой DoubleDouble
        MouseAdapter mouse = new MouseAdapter() {

            // Используем DoubleDouble для точных координат
            private DoubleDouble centerX = new DoubleDouble(-0.75);
            private DoubleDouble centerY = new DoubleDouble(0.0);
            private DoubleDouble scale = new DoubleDouble(3.0);

            private Point lastMouse = new Point();

            @Override
            public void mousePressed(MouseEvent e) {
                lastMouse = e.getPoint();
            }

            @Override
            public void mouseDragged(MouseEvent e) {

                double aspect = shader.width() / (double) shader.height();

                double dx = e.getX() - lastMouse.x;
                double dy = e.getY() - lastMouse.y;

                // Используем DoubleDouble для вычислений смещения
                DoubleDouble dxDD = new DoubleDouble(dx);
                DoubleDouble dyDD = new DoubleDouble(dy);
                
                DoubleDouble widthDD = new DoubleDouble(shader.width());
                DoubleDouble heightDD = new DoubleDouble(shader.height());
                DoubleDouble aspectDD = new DoubleDouble(aspect);
                DoubleDouble twoDD = new DoubleDouble(2.0);

                // centerX -= dx / shader.width() * scale * aspect * 2.0;
                centerX = centerX.subtract(
                    dxDD.divide(widthDD)
                        .multiply(scale)
                        .multiply(aspectDD)
                        .multiply(twoDD)
                );

                // centerY += dy / shader.height() * scale * 2.0;
                centerY = centerY.add(
                    dyDD.divide(heightDD)
                        .multiply(scale)
                        .multiply(twoDD)
                );

                shader.setParameter("centerX", centerX.toDouble());
                shader.setParameter("centerY", centerY.toDouble());

                lastMouse = e.getPoint();
            }

            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {

                double aspect = shader.width() / (double) shader.height();

                double zoom = Math.pow(1.1, e.getWheelRotation());
                
                // Используем DoubleDouble для вычислений масштабирования
                DoubleDouble zoomDD = new DoubleDouble(zoom);
                DoubleDouble newScale = scale.multiply(zoomDD);

                // NDC (-1..1)
                double ndcX = e.getX() / (double) shader.width() * 2.0 - 1.0;
                double ndcY = -(e.getY() / (double) shader.height() * 2.0 - 1.0);

                DoubleDouble ndcXDD = new DoubleDouble(ndcX);
                DoubleDouble ndcYDD = new DoubleDouble(ndcY);
                DoubleDouble aspectDD = new DoubleDouble(aspect);

                // Мировая координата под курсором ДО масштабирования
                // worldX = centerX + ndcX * aspect * scale;
                DoubleDouble worldX = centerX.add(
                    ndcXDD.multiply(aspectDD).multiply(scale)
                );
                
                // worldY = centerY + ndcY * scale;
                DoubleDouble worldY = centerY.add(
                    ndcYDD.multiply(scale)
                );

                // Новый центр (с повышенной точностью)
                // centerX = worldX - ndcX * aspect * newScale;
                centerX = worldX.subtract(
                    ndcXDD.multiply(aspectDD).multiply(newScale)
                );
                
                // centerY = worldY - ndcY * newScale;
                centerY = worldY.subtract(
                    ndcYDD.multiply(newScale)
                );

                scale = newScale;

                shader.setParameter("centerX", centerX.toDouble());
                shader.setParameter("centerY", centerY.toDouble());
                shader.setParameter("scale", scale.toDouble());

                Example1.x.set(centerX.toDouble());
                Example1.y.set(centerY.toDouble());
                Example1.scale.set(scale.toDouble());
            }
        };

        gal.getFrame().addMouseListener(mouse);
        gal.getFrame().addMouseMotionListener(mouse);
        gal.getFrame().addMouseWheelListener(mouse);

        gal.start();
    }

    public static void writePixels(
            BufferedImage bi,
            int x,
            int y,
            int width,
            int height,
            int[] pixels
    ) {
        if (pixels.length == 0) return;

        int[] buffer = ((DataBufferInt) bi.getRaster()
                .getDataBuffer())
                .getData();

        int imageWidth = bi.getWidth();

        for (int py = 0; py < height; py++) {

            // OpenGL идёт снизу вверх
            int srcY = height - py - 1;

            int dstY = y + py;

            if (dstY < 0 || dstY >= bi.getHeight())
                continue;

            for (int px = 0; px < width; px++) {

                int dstX = x + px;

                if (dstX < 0 || dstX >= imageWidth)
                    continue;

                int srcIndex = srcY * width + px;

                int rgba;
                try {
                    rgba = pixels[srcIndex];
                } catch (ArrayIndexOutOfBoundsException e) {
                    System.out.println("AIOOBE");
                    rgba = 0xFFFFFF;
                }

                // GL_RGBA -> ARGB для BufferedImage
                int r = rgba & 0xFF;
                int g = (rgba >> 8) & 0xFF;
                int b = (rgba >> 16) & 0xFF;
                int a = (rgba >> 24) & 0xFF;

                int argb =
                        (a << 24) |
                        (r << 16) |
                        (g << 8) |
                        b;

                buffer[dstY * imageWidth + dstX] = argb;
            }
        }
    }
}