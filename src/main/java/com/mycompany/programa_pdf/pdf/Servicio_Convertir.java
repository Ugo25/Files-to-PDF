package com.mycompany.programa_pdf.pdf;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.PDPageContentStream.AppendMode;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.util.Matrix;

import org.jodconverter.core.DocumentConverter;
import org.jodconverter.core.office.OfficeException;
import org.jodconverter.local.LocalConverter;
import org.jodconverter.local.office.LocalOfficeManager;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class Servicio_Convertir {

    /* ============== Helpers (no crear carpetas) ============== */

    /** Exige que exista la carpeta contenedora del archivo de salida. */
    private static void requireParentExists(File out) throws IOException {
        File dir = (out == null) ? null : out.getParentFile();
        if (dir == null || !dir.isDirectory()) {
            throw new IOException("La carpeta destino no existe: " +
                    (dir != null ? dir.getAbsolutePath() : "<sin carpeta>"));
        }
    }

    /** Nombre base sin extensión. */
    private static String base(String name) {
        int i = (name == null) ? -1 : name.lastIndexOf('.');
        return (i > 0) ? name.substring(0, i) : (name != null ? name : "archivo");
    }

    /* ============== A) Imagen(es) -> PDF ============== */

    /** Una imagen (png/jpg/webp/...) -> PDF (página del tamaño de la imagen). */
    public File imageToPdf(File image, File outPdf) throws IOException {
        BufferedImage img = ImageIO.read(image);
        if (img == null) throw new IOException("No se pudo leer la imagen: " + image.getAbsolutePath());
        requireParentExists(outPdf);

        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(new PDRectangle(img.getWidth(), img.getHeight()));
            doc.addPage(page);
            PDImageXObject pdImg = PDImageXObject.createFromFileByContent(image, doc);
            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                cs.drawImage(pdImg, 0, 0, img.getWidth(), img.getHeight());
            }
            doc.save(outPdf);
        }
        return outPdf;
    }

    /** Varias imágenes -> 1 PDF (cada imagen = una página). */
    public File imagesToPdf(List<File> images, File outPdf) throws IOException {
        if (images == null || images.isEmpty())
            throw new IOException("No se recibieron imágenes para convertir.");
        requireParentExists(outPdf);

        try (PDDocument doc = new PDDocument()) {
            for (File imgFile : images) {
                BufferedImage img = ImageIO.read(imgFile);
                if (img == null) throw new IOException("No se pudo leer: " + imgFile.getAbsolutePath());
                PDPage page = new PDPage(new PDRectangle(img.getWidth(), img.getHeight()));
                doc.addPage(page);
                PDImageXObject pdImg = PDImageXObject.createFromFileByContent(imgFile, doc);
                try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                    cs.drawImage(pdImg, 0, 0, img.getWidth(), img.getHeight());
                }
            }
            doc.save(outPdf);
        }
        return outPdf;
    }

    /* ============== B) Office -> PDF (LibreOffice) ============== */

    /**
     * DOC/DOCX/XLS/XLSX/PPT/PPTX -> PDF usando LibreOffice en modo headless.
     * @param libreOfficeHomeOrNull Carpeta de instalación de LibreOffice (p. ej. "C:/Program Files/LibreOffice") o null para autodetectar.
     */
    public File officeToPdf(File officeFile, File outPdf, File libreOfficeHomeOrNull)
            throws OfficeException, IOException {

        if (officeFile == null || !officeFile.exists())
            throw new IOException("Archivo de Office no encontrado: " + officeFile);
        requireParentExists(outPdf);

        LocalOfficeManager.Builder builder = LocalOfficeManager.builder();
        if (libreOfficeHomeOrNull != null) builder.officeHome(libreOfficeHomeOrNull);
        builder.taskExecutionTimeout(120_000L);

        LocalOfficeManager office = builder.build();
        try {
            office.start();
            DocumentConverter conv = LocalConverter.make(office);
            conv.convert(officeFile).to(outPdf).execute();
            return outPdf;
        } finally {
            office.stop();
        }
    }

    /* ============== C) PDF -> Imágenes en ZIP ============== */

    /**
     * Exporta cada página del PDF a PNG/JPG y las empaqueta en un ZIP elegido por el usuario.
     * No se crean carpetas; se exige que exista la carpeta del ZIP.
     * @param format "png" o "jpg"
     * @param dpi    resolución de renderizado (150/200/300...)
     */
    public File pdfToImagesAsZip(File pdf, File zipOut, String format, float dpi) throws IOException {
        String fmt = (format == null) ? "png" : format.toLowerCase();
        if (!fmt.equals("png") && !fmt.equals("jpg") && !fmt.equals("jpeg")) {
            throw new IOException("Formato no soportado: " + format + " (usa png/jpg)");
        }
        requireParentExists(zipOut);

        try (PDDocument doc = Loader.loadPDF(pdf);
             ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipOut))) {

            PDFRenderer renderer = new PDFRenderer(doc);
            String ext = fmt.equals("jpg") ? "jpg" : "png";

            for (int i = 0; i < doc.getNumberOfPages(); i++) {
                var bim = renderer.renderImageWithDPI(i, dpi, ImageType.RGB);

                String entryName = "%s_page_%03d.%s".formatted(base(pdf.getName()), i + 1, ext);

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(bim, ext, baos);
                baos.flush();

                zos.putNextEntry(new ZipEntry(entryName));
                zos.write(baos.toByteArray());
                zos.closeEntry();
                baos.close();
            }
        }
        return zipOut;
    }

    /* ============== D) Unir PDFs ============== */

    public File mergePdfs(List<File> pdfs, File outPdf) throws IOException {
        if (pdfs == null || pdfs.isEmpty()) throw new IOException("No se recibieron PDFs.");
        requireParentExists(outPdf);

        PDFMergerUtility mu = new PDFMergerUtility();
        for (File f : pdfs) {
            if (f == null || !f.exists()) throw new IOException("PDF no encontrado: " + f);
            mu.addSource(f);
        }
        mu.setDestinationFileName(outPdf.getAbsolutePath());
        mu.mergeDocuments(null); // si usas PDFBox 3, puede aceptarte null sin problema
        return outPdf;
    }

    /* ===================== E) Dividir por rango ===================== */

    /** Extrae un rango [fromPage..toPage] (1-based, inclusivo) a un nuevo PDF. */
    public File splitRange(File inputPdf, File outPdf, int fromPage, int toPage) throws IOException {
        if (fromPage < 1 || toPage < fromPage) {
            throw new IOException("Rango inválido: " + fromPage + "-" + toPage);
        }
        requireParentExists(outPdf);

        try (PDDocument src = Loader.loadPDF(inputPdf);
             PDDocument dst = new PDDocument()) {

            int total = src.getNumberOfPages();
            if (toPage > total) toPage = total;

            for (int i = fromPage - 1; i <= toPage - 1; i++) {
                dst.importPage(src.getPage(i));
            }
            dst.save(outPdf);
            return outPdf;
        }
    }

    /* ===================== F) Rotar páginas ===================== */

    /** Rota TODAS las páginas. degrees: 90, 180 o 270. */
    public File rotateAll(File inputPdf, File outPdf, int degrees) throws IOException {
        requireParentExists(outPdf);
        try (PDDocument doc = Loader.loadPDF(inputPdf)) {
            for (int i = 0; i < doc.getNumberOfPages(); i++) {
                PDPage p = doc.getPage(i);
                int rot = p.getRotation();
                p.setRotation((rot + degrees) % 360);
            }
            doc.save(outPdf);
            return outPdf;
        }
    }

    /** Rota un rango [fromPage..toPage] (1-based). */
    public File rotateRange(File inputPdf, File outPdf, int degrees, int fromPage, int toPage) throws IOException {
        requireParentExists(outPdf);
        try (PDDocument doc = Loader.loadPDF(inputPdf)) {
            int total = doc.getNumberOfPages();
            if (fromPage < 1) fromPage = 1;
            if (toPage > total) toPage = total;
            for (int i = fromPage - 1; i <= toPage - 1; i++) {
                PDPage p = doc.getPage(i);
                int rot = p.getRotation();
                p.setRotation((rot + degrees) % 360);
            }
            doc.save(outPdf);
            return outPdf;
        }
    }

    /* ===================== G) Marca de agua de texto ===================== */

    public File watermarkText(File inputPdf, File outPdf, String text, float fontSize) throws IOException {
        requireParentExists(outPdf);
        try (PDDocument doc = Loader.loadPDF(inputPdf)) {
            for (PDPage page : doc.getPages()) {
                PDRectangle media = page.getMediaBox();
                float pw = media.getWidth();
                float ph = media.getHeight();

                // Transparencia (20%)
                PDExtendedGraphicsState gs = new PDExtendedGraphicsState();
                gs.setNonStrokingAlphaConstant(0.20f);

                // Asegurar resources en la página (no necesitamos el nombre COS para setearlo)
                PDResources res = page.getResources();
                if (res == null) { res = new PDResources(); page.setResources(res); }
                // res.add(gs); // opcional; no es necesario para aplicar con setGraphicsStateParameters

                try (PDPageContentStream cs = new PDPageContentStream(
                        doc, page, AppendMode.APPEND, true, true)) {

                    // Aplica el estado extendido directamente
                    cs.setGraphicsStateParameters(gs);

                    // Color gris claro (opción A con java.awt.Color; opción B sería normalizar 0..1)
                    cs.setNonStrokingColor(new Color(200, 200, 200));

                    PDType1Font font = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);

                    // Ancho aproximado del texto para poder centrar
                    float textWidth = font.getStringWidth(text) / 1000f * fontSize;

                    // Matriz: centro de la página, rotación 45°, y desplazar medio ancho/alto
                    Matrix m = new Matrix();
                    m.translate(pw / 2f, ph / 2f);
                    m.rotate((float) Math.toRadians(45));
                    m.translate(-textWidth / 2f, -fontSize / 2f);

                    cs.beginText();
                    cs.setFont(font, fontSize);
                    cs.setTextMatrix(m);
                    cs.showText(text);
                    cs.endText();
                }
            }
            doc.save(outPdf);
            return outPdf;
        }
    }
    // ===== Marca de agua avanzada (posición/ángulo/color/alpha, todos o rango) =====
public File watermarkTextAdvanced(
        File inputPdf, File outPdf,
        String text, float fontSize,
        float angleDeg,
        float posRelX, float posRelY,   // 0..1 relativos al tamaño de página
        java.awt.Color color, float opacity, // opacity 0..1
        boolean allPages, int fromPage, int toPage
) throws IOException {

    if (opacity < 0f) opacity = 0f;
    if (opacity > 1f) opacity = 1f;

    requireParentExists(outPdf);
    try (PDDocument doc = Loader.loadPDF(inputPdf)) {

        int total = doc.getNumberOfPages();
        int start = allPages ? 1 : Math.max(1, fromPage);
        int end   = allPages ? total : Math.min(total, toPage);

        for (int i = start-1; i <= end-1; i++) {
            PDPage page = doc.getPage(i);
            PDRectangle media = page.getMediaBox();
            float pw = media.getWidth();
            float ph = media.getHeight();

            // transparencia
            org.apache.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState gs =
                    new org.apache.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState();
            gs.setNonStrokingAlphaConstant(opacity);

            org.apache.pdfbox.pdmodel.PDResources res = page.getResources();
            if (res == null) { res = new org.apache.pdfbox.pdmodel.PDResources(); page.setResources(res); }

            try (PDPageContentStream cs = new PDPageContentStream(
                    doc, page, AppendMode.APPEND, true, true)) {

                // aplica estado de transparencia
                cs.setGraphicsStateParameters(gs);

                // Color
                cs.setNonStrokingColor(color);

                // Fuente
                var font = new org.apache.pdfbox.pdmodel.font.PDType1Font(
                        org.apache.pdfbox.pdmodel.font.Standard14Fonts.FontName.HELVETICA_BOLD);

                // calcular ancho para centrar si se desea (usamos posRelX/Y como centro)
                float textWidth = font.getStringWidth(text) / 1000f * fontSize;

                // posición absoluta en página (centro)
                float cx = pw * posRelX;
                float cy = ph * posRelY;

                // Matriz: trasladar al punto, rotar, y desplazar media caja del texto
                org.apache.pdfbox.util.Matrix m = new org.apache.pdfbox.util.Matrix();
                m.translate(cx, cy);
                m.rotate((float)Math.toRadians(angleDeg));
                m.translate(-textWidth/2f, -fontSize/2f);

                cs.beginText();
                cs.setFont(font, fontSize);
                cs.setTextMatrix(m);
                cs.showText(text);
                cs.endText();
            }
        }
        doc.save(outPdf);
        return outPdf;
    }
}


    /* ===================== H) Extraer texto ===================== */

    public String extractText(File inputPdf) throws IOException {
        try (PDDocument doc = Loader.loadPDF(inputPdf)) {
            PDFTextStripper st = new PDFTextStripper();
            return st.getText(doc);
        }
    }
    // === I: construir PDDocument en memoria para preview ===
public PDDocument imagesToPdfDoc(java.util.List<File> images) throws java.io.IOException {
    if (images == null || images.isEmpty()) throw new java.io.IOException("Sin imágenes.");
    PDDocument doc = new PDDocument();
    try {
        for (File f : images) {
            var img = javax.imageio.ImageIO.read(f);
            if (img == null) throw new java.io.IOException("No se pudo leer: " + f);
            var page = new PDPage(new PDRectangle(img.getWidth(), img.getHeight()));
            doc.addPage(page);
            var pdImg = org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject.createFromFileByContent(f, doc);
            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                cs.drawImage(pdImg, 0, 0, img.getWidth(), img.getHeight());
            }
        }
        return doc; // el caller debe cerrar
    } catch (Exception e) {
        try { doc.close(); } catch (Exception ignore) {}
        throw e;
    }
}

public PDDocument mergePdfsDoc(java.util.List<File> pdfs) throws java.io.IOException {
    if (pdfs == null || pdfs.size() < 2) throw new java.io.IOException("Selecciona al menos 2 PDFs.");
    PDDocument out = new PDDocument();
    try {
        for (File f : pdfs) {
            try (PDDocument in = Loader.loadPDF(f)) {
                for (PDPage p : in.getPages()) out.addPage(p); // importa páginas
            }
        }
        return out;
    } catch (Exception e) {
        try { out.close(); } catch (Exception ignore) {}
        throw e;
    }
}

}
