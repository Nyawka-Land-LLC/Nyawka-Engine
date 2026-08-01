package org.nyawka_engine.core;

import java.awt.Graphics;
import java.awt.GraphicsEnvironment;
import java.awt.image.BufferedImage;

import javax.swing.JFrame;
import javax.swing.JPanel;

public abstract class Window extends JPanel {
    private final JFrame frame;

    protected final BufferedImage buffer = new BufferedImage(
            GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDisplayMode().getWidth(),
            GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDisplayMode().getHeight(),
            BufferedImage.TYPE_INT_ARGB);

    public Window(int width, int height, int x, int y, String title, boolean undecorated) {
        this.frame = new JFrame();
        this.frame.setSize(width, height);
        this.frame.setLocation(x, y);
        this.frame.setTitle(title);
        this.frame.setUndecorated(undecorated);
        this.frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.frame.add(this);
        this.frame.setVisible(true);
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.drawImage(buffer, 0, 0, null);
    }

    @PaintsStuff
    protected abstract void __paint__(BufferedImage buffer);

    @Getter
    public JFrame getFrame() {
        return frame;
    }
}
