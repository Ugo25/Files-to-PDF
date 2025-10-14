package com.mycompany.programa_pdf.pdf;

import com.mycompany.programa_pdf.dialogs.images.PageItem;
import com.mycompany.programa_pdf.images.ImageThumbCache;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.util.List;

/** Crea un nuevo PDDocument combinando páginas del PDF original + imágenes. */
public class ImagesAsPagesComposer {

    /**
     * @param src       PDF de origen (para importar páginas).
     * @param sequence  orden final (páginas del PDF e imágenes).
     * @return PDDocument listo para guardar o imprimir (caller debe cerrar).
     */
    public PDDocument compose(PDDocument src, List<PageItem> sequence) throws Exception {
        PDDocument out = new PDDocument();
        for (PageItem it : sequence) {
            if (it.kind == PageItem.Kind.PDF_PAGE) {
                out.importPage(src.getPage(it.pdfPageIndex));
            } else {
                BufferedImage img = ImageIO.read(it.imageFile);
                if (img == null) continue;
                if (it.rotation != 0) img = ImageThumbCache.rotate90s(img, it.rotation);

                PDPage page = new PDPage(new PDRectangle(img.getWidth(), img.getHeight()));
                out.addPage(page);

                var pdImg = LosslessFactory.createFromImage(out, img);
                float marginX = (float) (img.getWidth() * 0.05);
                float marginY = (float) (img.getHeight() * 0.05);
                float w = img.getWidth() - 2 * marginX;
                float h = img.getHeight() - 2 * marginY;

                try (PDPageContentStream cs = new PDPageContentStream(out, page)) {
                    cs.drawImage(pdImg, marginX, marginY, w, h);
                }
            }
        }
        return out;
    }
}
