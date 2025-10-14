package com.mycompany.programa_pdf.dialogs.merge;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.apache.pdfbox.pdmodel.PDDocument;

import java.io.File;
import java.io.IOException;
import java.util.List;

/** Servicio para unir PDFs (en memoria o a archivo). */
public final class MergeService {

    /** Une los PDFs en un PDDocument en memoria. Debe cerrarse por el caller. */
    public PDDocument mergeInMemory(List<File> pdfs) throws IOException {
        PDDocument target = new PDDocument();
        PDFMergerUtility util = new PDFMergerUtility();
        for (File f : pdfs) {
            try (PDDocument src = Loader.loadPDF(f)) {
                util.appendDocument(target, src);
            }
        }
        return target;
    }

    /** Une y guarda directamente en archivo. Devuelve el mismo File. */
    public File mergeToFile(List<File> pdfs, File out) throws IOException {
        try (PDDocument merged = mergeInMemory(pdfs)) {
            merged.save(out);
        }
        return out;
    }
}
