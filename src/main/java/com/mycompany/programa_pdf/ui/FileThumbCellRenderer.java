package com.mycompany.programa_pdf.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileSystemView;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.function.Function;

public class FileThumbCellRenderer extends DefaultListCellRenderer {

    private final Function<File, ImageIcon> thumbProvider;

    public FileThumbCellRenderer(Function<File, ImageIcon> thumbProvider) {
        this.thumbProvider = thumbProvider;
    }

    @Override
    public Component getListCellRendererComponent(JList<?> list, Object value,
                                                  int index, boolean isSelected, boolean cellHasFocus) {
        JLabel lbl = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        File f = (File) value;
        lbl.setText(f.getName());
        lbl.setBorder(new EmptyBorder(6,8,6,8));
        try {
            ImageIcon ic = thumbProvider.apply(f);
            lbl.setIcon(ic);
        } catch (Exception ex) {
            Icon sys = FileSystemView.getFileSystemView().getSystemIcon(f);
            lbl.setIcon(sys);
        }
        lbl.setHorizontalTextPosition(SwingConstants.RIGHT);
        lbl.setIconTextGap(12);
        return lbl;
    }

    /** Utilidad pública para convertir iconos del sistema a BufferedImage. */
    public static BufferedImage iconToImage(Icon icon, int w, int h) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        int x = (w - icon.getIconWidth())/2;
        int y = (h - icon.getIconHeight())/2;
        icon.paintIcon(null, g, Math.max(0,x), Math.max(0,y));
        g.dispose();
        return img;
    }
}
