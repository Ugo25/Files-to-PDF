package com.mycompany.programa_pdf.images;

import java.awt.*;
import java.awt.image.BufferedImage;

public final class ImageUtils {
    private ImageUtils(){}

    public static BufferedImage scale(BufferedImage src, int maxW, int maxH) {
        double sx = maxW / (double) src.getWidth();
        double sy = maxH / (double) src.getHeight();
        double s = Math.min(sx, sy);
        int w = Math.max(1, (int) Math.round(src.getWidth() * s));
        int h = Math.max(1, (int) Math.round(src.getHeight() * s));
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.drawImage(src, 0, 0, w, h, null);
        g.dispose();
        return out;
    }

    public static BufferedImage rotate90s(BufferedImage src, int deg) {
        int steps = ((deg % 360) + 360) % 360 / 90;
        BufferedImage img = src;
        for (int i = 0; i < steps; i++) {
            int w = img.getWidth(), h = img.getHeight();
            BufferedImage dst = new BufferedImage(h, w, img.getType() == 0 ? BufferedImage.TYPE_INT_ARGB : img.getType());
            Graphics2D g = dst.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.translate(h, 0);
            g.rotate(Math.toRadians(90));
            g.drawImage(img, 0, 0, null);
            g.dispose();
            img = dst;
        }
        return img;
    }
}
