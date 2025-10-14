package com.mycompany.programa_pdf.pdf;

import java.awt.*;
import java.awt.print.*;
import java.awt.image.BufferedImage;
import java.util.List;
import javax.swing.*;

// ===== para PDF =====
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.attribute.standard.PageRanges;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.printing.PDFPageable;

public final class PrintUtils {

    private PrintUtils() {}

    /** Imprime cada imagen de la lista en una página centrada (tu método original, intacto). */
    public static void printThumbnails(List<BufferedImage> images) {
        if (images == null || images.isEmpty()) return;

        PrinterJob job = PrinterJob.getPrinterJob();
        job.setJobName("Vista previa archivos");
        job.setPrintable(new Printable() {
            @Override
            public int print(Graphics g, PageFormat pf, int pageIndex) throws PrinterException {
                if (pageIndex < 0 || pageIndex >= images.size()) return NO_SUCH_PAGE;
                Graphics2D g2 = (Graphics2D) g;
                g2.translate(pf.getImageableX(), pf.getImageableY());
                BufferedImage img = images.get(pageIndex);

                double iw = img.getWidth(), ih = img.getHeight();
                double pw = pf.getImageableWidth(), ph = pf.getImageableHeight();
                double s = Math.min(pw/iw, ph/ih);
                int w = (int) Math.round(iw * s), h = (int) Math.round(ih * s);
                int x = (int) Math.round((pw - w)/2.0), y = (int) Math.round((ph - h)/2.0);
                g2.drawImage(img, x, y, w, h, null);
                return PAGE_EXISTS;
            }
        });

        if (job.printDialog()) {
            try { job.print(); }
            catch (PrinterException ex) {
                JOptionPane.showMessageDialog(null, "Error al imprimir: " + ex.getMessage(),
                        "Impresión", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // ===================== NUEVO: Impresión de PDF (PDFBox) =====================

    /** Abre el diálogo del sistema e imprime todo el PDF. */
    public static void printPdf(PDDocument doc) throws PrinterException {
        printPdf(doc, null, null);
    }

    /**
     * Abre el diálogo del sistema e imprime el PDF.
     * Si pasas startPage1 y endPage1 (1-based), se sugiere ese rango en el diálogo.
     */
    public static void printPdf(PDDocument doc, Integer startPage1, Integer endPage1) throws PrinterException {
        if (doc == null) return;

        PrinterJob job = PrinterJob.getPrinterJob();
        job.setJobName("Imprimir PDF");
        job.setPageable(new PDFPageable(doc)); // deja al driver duplex, escalado, etc.

        PrintRequestAttributeSet attrs = new HashPrintRequestAttributeSet();
        if (startPage1 != null && endPage1 != null) {
            attrs.add(new PageRanges(startPage1, endPage1));
        }

        if (!job.printDialog(attrs)) return; // usuario canceló
        job.print(attrs);
    }
}
