package com.mycompany.programa_pdf.dialogs.images;

import com.mycompany.programa_pdf.images.ImageThumbCache;
import org.apache.pdfbox.rendering.PDFRenderer;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/** Renderer de celdas para la lista de páginas/imagenes. */
public class PageThumbRenderer extends JPanel implements ListCellRenderer<PageItem> {

    private final JLabel pic = new JLabel("", SwingConstants.CENTER);
    private final JLabel txt = new JLabel("", SwingConstants.CENTER);
    private final PDFRenderer renderer;

    public PageThumbRenderer(PDFRenderer renderer) {
        this.renderer = renderer;
        setLayout(new BorderLayout(4,4));
        setOpaque(true);
        setBackground(UIManager.getColor("Panel.background"));
        txt.setForeground(UIManager.getColor("Label.infoForeground"));
        txt.setFont(txt.getFont().deriveFont(Font.PLAIN, 11f));
        add(pic, BorderLayout.CENTER);
        add(txt, BorderLayout.SOUTH);
        setBorder(new EmptyBorder(6,6,6,6));
        setPreferredSize(new Dimension(200, 260));
    }

    @Override
    public Component getListCellRendererComponent(JList<? extends PageItem> list, PageItem value, int index, boolean isSelected, boolean cellHasFocus) {
        try {
            if (value.kind == PageItem.Kind.PDF_PAGE) {
                if (value.thumb == null) value.thumb = ImageThumbCache.thumbForPdf(renderer, value.pdfPageIndex, 180, 230);
                pic.setIcon(new ImageIcon(value.thumb));
                txt.setText("Página " + (value.pdfPageIndex + 1));
            } else {
                if (value.thumb == null) {
                    value.thumb = ImageThumbCache.thumbForImage(value.imageFile, 0, 180, 230);
                    value.thumbRot = null;
                }
                if (value.rotation == 0) {
                    pic.setIcon(new ImageIcon(value.thumb));
                } else {
                    if (value.thumbRot == null) value.thumbRot = ImageThumbCache.scale(ImageThumbCache.rotate90s(value.thumb, value.rotation), 180, 230);
                    pic.setIcon(new ImageIcon(value.thumbRot));
                }
                txt.setText("Imagen" + (value.rotation != 0 ? (" (" + value.rotation + "°)") : ""));
            }
        } catch (Exception ex) {
            pic.setIcon(null);
            txt.setText("Error");
        }

        setBackground(isSelected ? list.getSelectionBackground() : list.getBackground());
        setForeground(isSelected ? list.getSelectionForeground() : list.getForeground());
        return this;
    }
}
