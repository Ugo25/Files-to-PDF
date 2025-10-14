package com.mycompany.programa_pdf.ui;


import javax.swing.*;
import java.awt.*;

public class RoundedDropPanel extends JPanel {
    private Color back;
    private Color border;
    private Color borderHighlight;
    private final int arc = 18;
    private boolean highlighted = false;

    public RoundedDropPanel() {
        setOpaque(false);
        setBorder(new javax.swing.border.EmptyBorder(32, 32, 32, 32));
        updateColors();
    }

    @Override public void updateUI() {
        super.updateUI();
        updateColors();
    }

    private void updateColors() {
        back            = UIManager.getColor("Panel.background");
        border          = UIManager.getColor("Component.borderColor");
        borderHighlight = UIManager.getColor("Component.focusColor");
        if (back == null)   back   = Color.WHITE;
        if (border == null) border = new Color(150,150,150);
        if (borderHighlight == null) borderHighlight = new Color(66,133,244);
        repaint();
    }

    public void setHighlighted(boolean h) {
        if (highlighted != h) { highlighted = h; repaint(); }
    }

    @Override protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Sombra sutil
        g2.setColor(new Color(0,0,0,18));
        g2.fillRoundRect(6, 8, getWidth()-12, getHeight()-12, arc+6, arc+6);

        // Fondo
        g2.setColor(back);
        g2.fillRoundRect(0, 2, getWidth()-12, getHeight()-12, arc, arc);

        // Borde
        g2.setStroke(new BasicStroke(highlighted ? 3f : 2f));
        g2.setColor(highlighted ? borderHighlight : border);
        g2.drawRoundRect(0, 2, getWidth()-12, getHeight()-12, arc, arc);

        if (highlighted) {
            Color glow = new Color(borderHighlight.getRed(), borderHighlight.getGreen(), borderHighlight.getBlue(), 40);
            g2.setColor(glow);
            g2.setStroke(new BasicStroke(6f));
            g2.drawRoundRect(1, 3, getWidth()-14, getHeight()-14, arc+2, arc+2);
        }

        g2.dispose();
        super.paintComponent(g);
    }
}
