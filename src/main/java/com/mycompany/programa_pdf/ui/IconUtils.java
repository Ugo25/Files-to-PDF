package com.mycompany.programa_pdf.ui;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.net.URL;

public final class IconUtils {
    private IconUtils(){}

    public static Icon svgOrPng(String path, int w, int h) {
        try {
            if (path != null && path.toLowerCase().endsWith(".svg")) return new FlatSVGIcon(trimSlash(path), w, h);
        } catch (Throwable ignore) {}

        try {
            URL url = IconUtils.class.getResource(path.startsWith("/") ? path : "/" + path);
            if (url != null) {
                BufferedImage img = ImageIO.read(url);
                if (img != null) return new ImageIcon(img.getScaledInstance(w, h, Image.SCALE_SMOOTH));
            }
        } catch (Throwable ignore) {}
        return null;
    }

    private static String trimSlash(String p){ return p.startsWith("/") ? p.substring(1) : p; }
}
