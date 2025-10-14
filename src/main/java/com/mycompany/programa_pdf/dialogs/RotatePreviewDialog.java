package com.mycompany.programa_pdf.dialogs;

import com.mycompany.programa_pdf.MainWindow;
import com.mycompany.programa_pdf.io.FxFileDialogs;
import com.mycompany.programa_pdf.pdf.PrintUtils;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RotatePreviewDialog extends JDialog {

    private final MainWindow mw;
    private final File pdfIn;

    private PDDocument doc;
    private PDFRenderer renderer;

    private final PreviewPanel preview = new PreviewPanel();
    private final JSpinner spPage = new JSpinner();
    private final JCheckBox chkAll = new JCheckBox("Aplicar a todas", false);
    private final JLabel lblDeg = new JLabel("0°");
    private int pageCount = 1;

    /** Rotaciones por página (0/90/180/270). */
    private final Map<Integer, Integer> rotations = new HashMap<>();

    public RotatePreviewDialog(MainWindow mw, File pdfIn) {
        super(mw, "Rotar páginas", true);
        this.mw = mw;
        this.pdfIn = pdfIn;

        setMinimumSize(new Dimension(980, 680));
        setLocationRelativeTo(mw);
        setLayout(new BorderLayout(8, 8));
        ((JComponent) getContentPane()).setBorder(new EmptyBorder(10, 10, 10, 10));

        // Centro: preview
        add(preview, BorderLayout.CENTER);

        // ===== Barra inferior en 2 filas (responsiva) =====
        JPanel south = new JPanel(new GridLayout(2, 1, 0, 4));

        // --- Fila 1: página + rotaciones
        JPanel row1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        row1.add(new JLabel("Página:"));
        spPage.setModel(new SpinnerNumberModel(1, 1, 1, 1));
        spPage.addChangeListener(e -> updatePreview());
        row1.add(spPage);

        JButton btnLeft = new JButton("↺ 90°");
        btnLeft.addActionListener(e -> rotate(-90));
        JButton btnRight = new JButton("↻ 90°");
        btnRight.addActionListener(e -> rotate(90));
        row1.add(btnLeft);
        row1.add(btnRight);

        JPanel presets = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        for (int p : new int[]{0, 90, 180, 270}) {
            JButton b = new JButton(p + "°");
            b.addActionListener(e -> setCurrentRotation(p));
            presets.add(b);
        }
        row1.add(presets);

        // --- Fila 2: izquierda info; derecha acciones
        JPanel row2 = new JPanel(new BorderLayout());

        JPanel row2Left = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        row2Left.add(new JLabel("Ángulo actual:"));
        lblDeg.setFont(lblDeg.getFont().deriveFont(Font.BOLD));
        row2Left.add(lblDeg);
        row2Left.add(chkAll);

        JPanel row2Right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 6));
        JButton btnPrint = new JButton("Imprimir…");
        btnPrint.addActionListener(e -> onPrint());
        JButton btnSave = new JButton("Guardar…");
        btnSave.addActionListener(e -> saveResult());
        JButton btnClose = new JButton("Cerrar");
        btnClose.addActionListener(e -> dispose());
        row2Right.add(btnPrint);
        row2Right.add(btnSave);
        row2Right.add(btnClose);

        row2.add(row2Left, BorderLayout.WEST);
        row2.add(row2Right, BorderLayout.EAST);

        south.add(row1);
        south.add(row2);
        add(south, BorderLayout.SOUTH);

        // Cargar documento y preparar
        loadDoc();
        addComponentListener(new ComponentAdapter() {
            @Override public void componentResized(ComponentEvent e) { preview.repaint(); }
        });
    }

    private void loadDoc() {
        try {
            doc = Loader.loadPDF(pdfIn);
            renderer = new PDFRenderer(doc);
            pageCount = doc.getNumberOfPages();
            spPage.setModel(new SpinnerNumberModel(1, 1, pageCount, 1));
            updatePreview();
        } catch (Exception ex) {
            mw.fail(ex);
            dispose();
        }
    }

    private int getCurrentPageIndex() { return ((Number) spPage.getValue()).intValue() - 1; }

    private int getRotationFor(int pageIndex) {
        Integer v = rotations.get(pageIndex);
        return (v == null ? 0 : v);
    }

    private void setCurrentRotation(int deg) {
        int pi = getCurrentPageIndex();
        rotations.put(pi, norm(deg));
        lblDeg.setText(norm(deg) + "°");
        preview.repaint();
    }

    private void rotate(int delta) {
        int pi = getCurrentPageIndex();
        int newDeg = norm(getRotationFor(pi) + delta);
        if (chkAll.isSelected()) {
            for (int i = 0; i < pageCount; i++) rotations.put(i, newDeg);
        } else {
            rotations.put(pi, newDeg);
        }
        lblDeg.setText(newDeg + "°");
        preview.repaint();
    }

    private int norm(int d) {
        int r = ((d % 360) + 360) % 360;
        if (r == 360) r = 0;
        if (r % 90 != 0) {
            r = Math.round(r / 90f) * 90;
            r = (r + 360) % 360;
        }
        return r;
    }

    private void updatePreview() {
        lblDeg.setText(getRotationFor(getCurrentPageIndex()) + "°");
        preview.repaint();
    }

    private void onPrint() {
        // Aplicar rotaciones en memoria, imprimir y restaurar
        List<Integer> old = new ArrayList<>(pageCount);
        try {
            for (int i = 0; i < pageCount; i++) {
                PDPage p = doc.getPage(i);
                int base = p.getRotation();
                old.add(base);
                int extra = getRotationFor(i);
                p.setRotation((base + extra) % 360);
            }
            PrintUtils.printPdf(doc);
        } catch (Exception ex) {
            mw.fail(ex);
        } finally {
            try {
                for (int i = 0; i < pageCount; i++) doc.getPage(i).setRotation(old.get(i));
            } catch (Exception ignore) {}
        }
    }

    private void saveResult() {
        try {
            String base = pdfIn.getName().replaceAll("(?i)\\.pdf$", "");
            File out = FxFileDialogs.pickSave(
                    this,
                    "Guardar PDF rotado como…",
                    pdfIn.getParentFile(),
                    base + "_rotado.pdf",
                    "pdf"
            );
            if (out == null) return;

            // Aplicar rotaciones y guardar
            for (int i = 0; i < doc.getNumberOfPages(); i++) {
                PDPage p = doc.getPage(i);
                int baseRot = p.getRotation();
                int extra = getRotationFor(i);
                p.setRotation((baseRot + extra) % 360);
            }
            doc.save(out);

            mw.ok("PDF rotado: " + out.getAbsolutePath());
            mw.getHistory().add("Rotar páginas (preview)",
                    List.of(pdfIn.getAbsolutePath()),
                    out.getAbsolutePath());
            mw.openIfWanted(out);
            dispose();
        } catch (Exception ex) {
            mw.fail(ex);
        }
    }

    @Override public void dispose() {
        try { if (doc != null) doc.close(); } catch (Exception ignore) {}
        super.dispose();
    }

    /* ==== panel preview ==== */
    private class PreviewPanel extends JPanel {
        PreviewPanel() { setBackground(Color.DARK_GRAY); }

        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (doc == null) return;
            int pi = getCurrentPageIndex();
            try {
                float dpi = 140f;
                BufferedImage img = renderer.renderImageWithDPI(pi, dpi, ImageType.RGB);

                int rot = getRotationFor(pi);

                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int w = getWidth() - 20, h = getHeight() - 20;
                double scale = Math.min(w / (double) img.getWidth(), h / (double) img.getHeight());
                int drawW = (int) (img.getWidth() * scale);
                int drawH = (int) (img.getHeight() * scale);
                int cx = (getWidth() - drawW) / 2;
                int cy = (getHeight() - drawH) / 2;

                g2.translate(cx + drawW / 2.0, cy + drawH / 2.0);
                g2.rotate(Math.toRadians(rot));
                g2.translate(-drawW / 2.0, -drawH / 2.0);
                g2.drawImage(img, 0, 0, drawW, drawH, null);
                g2.dispose();
            } catch (Exception ignore) {}
        }
    }
}
