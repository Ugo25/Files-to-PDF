package com.mycompany.programa_pdf.dialogs.merge;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Render de páginas + cache de #páginas y miniaturas. */
public final class PdfPreviewer {
    private final Map<File, Integer> pagesCache = new ConcurrentHashMap<>();
    private final Map<File, Image> thumbCache = new ConcurrentHashMap<>();

    /** Devuelve el número de páginas (con caché). */
    public int getPageCount(File pdf) throws IOException {
        Integer c = pagesCache.get(pdf);
        if (c != null) return c;
        try (PDDocument doc = Loader.loadPDF(pdf)) {
            int p = doc.getNumberOfPages();
            pagesCache.put(pdf, p);
            return p;
        }
    }

    /** Renderiza una página a un DPI dado. pageIndex es 0-based. */
    public BufferedImage renderPage(File pdf, int pageIndex, int dpi) throws IOException {
        try (PDDocument doc = Loader.loadPDF(pdf)) {
            PDFRenderer r = new PDFRenderer(doc);
            return r.renderImageWithDPI(pageIndex, dpi, ImageType.RGB);
        }
    }

    /** Miniatura (caché); usa 96 DPI y escala (lado ancho=64px aprox). */
    public Image getThumb(File pdf) throws IOException {
        Image c = thumbCache.get(pdf);
        if (c != null) return c;
        try (PDDocument doc = Loader.loadPDF(pdf)) {
            PDFRenderer r = new PDFRenderer(doc);
            BufferedImage img = r.renderImageWithDPI(0, 96, ImageType.RGB);
            int w = 64;
            int h = Math.max(48, (int) Math.round(img.getHeight() * (w / (double) img.getWidth())));
            Image scaled = img.getScaledInstance(w, h, Image.SCALE_SMOOTH);
            thumbCache.put(pdf, scaled);
            pagesCache.putIfAbsent(pdf, doc.getNumberOfPages());
            return scaled;
        }
    }

    /** Invalida cachés para un archivo concreto (opcional). */
    public void invalidate(File pdf) {
        pagesCache.remove(pdf);
        thumbCache.remove(pdf);
    }

    /** Limpia todos los cachés. */
    public void clearCaches() {
        pagesCache.clear();
        thumbCache.clear();
    }
}
