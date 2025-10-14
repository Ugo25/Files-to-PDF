package com.mycompany.programa_pdf.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class RoundedCard extends JPanel {

    // ===== State =====
    private boolean hover = false;
    private int arc = 22;

    // Theme-derived colors
    private Color bg;
    private Color border;
    private Color hoverBg;
    private Color hoverBorder;

    // ===== Ctor =====
    public RoundedCard() {
        setOpaque(false);
        setBorder(new EmptyBorder(16, 16, 16, 16));
        updateThemeColors();
        addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseEntered(java.awt.event.MouseEvent e) { setHover(true); }
            @Override public void mouseExited (java.awt.event.MouseEvent e) { setHover(false); }
        });
    }

    // ===== API =====
    public void setHover(boolean h) {
        if (hover != h) { hover = h; repaint(); }
    }
    public void setArc(int arc) {
        if (arc > 0 && this.arc != arc) { this.arc = arc; repaint(); }
    }

    // ===== LaF hook =====
    @Override public void updateUI() {
        super.updateUI();
        updateThemeColors();
    }

    // ===== Paint =====
    @Override protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        Insets in = getInsets();                 // respeta el padding real del componente
        int x = in.left, y = in.top;
        int w = getWidth() - in.left - in.right;
        int h = getHeight() - in.top - in.bottom;

        // sombras suaves
        g2.setColor(new Color(0, 0, 0, hover ? 36 : 22));
        g2.fillRoundRect(x + 6, y + 6, w - 6, h - 6, arc + 10, arc + 10);
        g2.setColor(new Color(0, 0, 0, 10));
        g2.fillRoundRect(x + 4, y + 4, w - 4, h - 4, arc + 12, arc + 12);

        // fondo
        g2.setColor(hover ? hoverBg : bg);
        g2.fillRoundRect(x, y, w - 8, h - 8, arc, arc);

        // borde
        g2.setStroke(new BasicStroke(2f));
        g2.setColor(hover ? hoverBorder : border);
        g2.drawRoundRect(x, y, w - 8, h - 8, arc, arc);

        g2.dispose();
        super.paintComponent(g);
    }

    @Override public boolean isOpaque() { return false; }

    // ===== Theme =====
    private void updateThemeColors() {
        Color panelBg    = UIManager.getColor("Panel.background");
        Color listBg     = UIManager.getColor("List.background");
        Color compBorder = UIManager.getColor("Component.borderColor");
        Color focus      = UIManager.getColor("Component.focusColor");

        if (panelBg == null)    panelBg    = new Color(245, 245, 245);
        if (listBg == null)     listBg     = new Color(250, 250, 250);
        if (compBorder == null) compBorder = new Color(180, 180, 180);
        if (focus == null)      focus      = new Color(66, 133, 244);

        boolean dark = isDark(panelBg);
        bg          = mix(listBg, panelBg, dark ? 0.08f : -0.04f);
        border      = compBorder;
        hoverBg     = mix(bg, focus, dark ? 0.06f : 0.04f);
        hoverBorder = focus;

        repaint();
    }

    // ===== Utils =====
    private static boolean isDark(Color c) {
        double Y = (0.2126 * c.getRed() + 0.7152 * c.getGreen() + 0.0722 * c.getBlue()) / 255.0;
        return Y < 0.5;
    }

    private static Color mix(Color bg, Color fg, float factor) {
        float f = factor;
        int r = clamp((int) (bg.getRed()   + f * (fg.getRed()   - bg.getRed())));
        int g = clamp((int) (bg.getGreen() + f * (fg.getGreen() - bg.getGreen())));
        int b = clamp((int) (bg.getBlue()  + f * (fg.getBlue()  - bg.getBlue())));
        return new Color(r, g, b);
    }

    private static int clamp(int v) {
        return Math.max(0, Math.min(255, v));
    }
}
