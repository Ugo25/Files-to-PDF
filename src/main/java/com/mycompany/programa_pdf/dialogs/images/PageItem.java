package com.mycompany.programa_pdf.dialogs.images;

import java.awt.image.BufferedImage;
import java.io.File;

/** Modelo de ítem en la lista: puede ser una página del PDF o una imagen nueva. */
public class PageItem {

    public enum Kind { PDF_PAGE, IMAGE_PAGE }

    public final Kind kind;
    public final int pdfPageIndex;    // si es página del PDF
    public final File imageFile;      // si es imagen externa
    public int rotation = 0;          // solo aplica a IMAGE_PAGE
    public BufferedImage thumb, thumbRot; // cache de miniatura

    public PageItem(int pdfIndex) { this.kind = Kind.PDF_PAGE; this.pdfPageIndex = pdfIndex; this.imageFile = null; }
    public PageItem(File img)     { this.kind = Kind.IMAGE_PAGE; this.imageFile = img; this.pdfPageIndex = -1; }

    /** Rotar imagen en múltiplos de 90° (solo para IMAGE_PAGE). */
    public void rotate(int delta) {
        if (kind != Kind.IMAGE_PAGE) return;
        rotation = ((rotation + delta) % 360 + 360) % 360;
        thumbRot = null;
    }
}
