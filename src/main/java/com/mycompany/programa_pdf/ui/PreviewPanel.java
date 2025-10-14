package com.mycompany.programa_pdf.ui;

import javax.swing.*;
import java.awt.*;
import java.io.File;

public class PreviewPanel extends JPanel {
    private ImageIcon img;
    private File file;
    private String hint = "Selecciona archivos o arrástralos aquí";

    public PreviewPanel() {
        setOpaque(true);
        setBackground(UIManager.getColor("Panel.background"));
        setBorder(BorderFactory.createTitledBorder("Vista previa"));
    }

    public void setImage(ImageIcon img, File file) {
        this.img = img;
        this.file = file;
        repaint();
    }
    public void setHint(String h) { this.hint = h; }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

        int w = getWidth()-20, h = getHeight()-40;
        int ox = 10, oy = 20;

        if (img != null) {
            int iw = img.getIconWidth(), ih = img.getIconHeight();
            double s = Math.min(w/(double)iw, h/(double)ih);
            int rw = Math.max(1, (int)Math.round(iw*s));
            int rh = Math.max(1, (int)Math.round(ih*s));
            int x = ox + (w - rw)/2, y = oy + (h - rh)/2;
            img.paintIcon(this, g2, x, y);
        } else {
            g2.setColor(new Color(130,130,130));
            String msg = hint != null ? hint : "Sin vista previa";
            Font f = getFont().deriveFont(Font.PLAIN, 14f);
            g2.setFont(f);
            FontMetrics fm = g2.getFontMetrics();
            int tx = ox + (w - fm.stringWidth(msg))/2;
            int ty = oy + h/2;
            g2.drawString(msg, tx, ty);
        }

        if (file != null) {
            g2.setColor(new Color(110,110,110));
            g2.setFont(getFont().deriveFont(Font.PLAIN, 12f));
            g2.drawString(file.getName(), 16, getHeight()-12);
        }
        g2.dispose();
    }
}
