package com.mycompany.programa_pdf.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

public class SmoothRoundPanel extends JPanel {

    private int arc = 24;
    private float borderWidth = 1f;
    private Color borderColor = new Color(255, 255, 255, 36); // sutil
    public SmoothRoundPanel() { setOpaque(false); }

    public void setArc(int arc) { this.arc = Math.max(0, arc); repaint(); }
    public void setBorderWidth(float w) { this.borderWidth = Math.max(0f, w); repaint(); }
    public void setBorderColor(Color c) { this.borderColor = c; repaint(); }

    @Override public void paint(Graphics g) {
        int w = getWidth(), h = getHeight();
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

        // 0.5 para alinear el trazo a pixel
        Shape rr = new RoundRectangle2D.Double(0.5, 0.5, w - 1, h - 1, arc, arc);

        // fondo del card
        g2.setColor(getBackground());
        g2.fill(rr);

        // recortamos TODO lo que se pinte después (componentes hijos incluidos)
        g2.setClip(rr);
        super.paint(g2);
        g2.setClip(null);

        // borde suave (sin picos)
        if (borderColor != null && borderWidth > 0f) {
            g2.setColor(borderColor);
            g2.setStroke(new BasicStroke(borderWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.draw(rr);
        }
        g2.dispose();
    }
}
