package com.mycompany.programa_pdf.ui;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import javax.swing.*;

public class ScalableSvgLabel extends JLabel {
    private final String svgPath;
    private int lastW = -1, lastH = -1;

    public ScalableSvgLabel(String svgPath) {
        this.svgPath = svgPath;
        setHorizontalAlignment(SwingConstants.CENTER);
        setVerticalAlignment(SwingConstants.CENTER);
        setIcon(new FlatSVGIcon(svgPath, 96, 96)); // icono inicial
        addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override public void componentShown   (java.awt.event.ComponentEvent e) { refresh(); }
            @Override public void componentResized (java.awt.event.ComponentEvent e) { refresh(); }
        });
    }

    private void refresh() {
        int w = getWidth(), h = getHeight();
        if (w <= 0 || h <= 0) return;
        if (w == lastW && h == lastH) return;
        lastW = w; lastH = h;

        int side = Math.min(w, h);
        int size = (int) Math.round(side * 0.85); // ocupa 85% del slot
        size = Math.max(96, Math.min(300, size)); // límites
        setIcon(new FlatSVGIcon(svgPath, size, size));
        revalidate();
        repaint();
    }
}
