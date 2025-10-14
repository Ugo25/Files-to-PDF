package com.mycompany.programa_pdf.dialogs;

import com.mycompany.programa_pdf.MainWindow;
import com.mycompany.programa_pdf.pdf.PrintUtils;
import com.mycompany.programa_pdf.io.FxFileDialogs;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.util.Matrix;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;

/**
 * Vista previa de marca de agua con miniaturas, imprimir y guardar (explorador nativo),
 * y opción "Aplicar a: todas / solo esta página".
 */
public class WatermarkPreviewDialog extends JDialog {

    private static final int PREVIEW_DPI = 144;
    private static final int THUMB_W = 130;

    private final MainWindow mw;
    private final File pdfIn;

    private PDDocument doc;
    private PDFRenderer renderer;

    // Controles
    private final JTextField txtText = new JTextField("CONFIDENCIAL", 16);
    private final JSpinner spFont = new JSpinner(new SpinnerNumberModel(48, 8, 400, 1));
    private final JSlider sAngle = new JSlider(-90, 90, 45);
    private final JSlider sX = new JSlider(0, 100, 50);
    private final JSlider sY = new JSlider(0, 100, 50);
    private final JSlider sOpacity = new JSlider(5, 100, 25);
    private final JButton btnColor = new JButton("Cambiar…");
    private final JPanel colorSwatch = new JPanel();
    private Color color = new Color(200, 160, 60);

    private final JList<Integer> lstPages = new JList<>();
    private final Preview preview = new Preview();
    private float zoom = 0.85f;
    private final JSlider sZoom = new JSlider(20, 200, 85);

    private final JRadioButton rbAll = new JRadioButton("Todas las páginas", true);
    private final JRadioButton rbOne = new JRadioButton("Solo esta página", false);

    private final JButton btnPrint = new JButton("Imprimir…");
    private final JButton btnSave  = new JButton("Guardar…");

    // Caché miniaturas
    private final Map<Integer, ImageIcon> thumbCache = new HashMap<>();

    public WatermarkPreviewDialog(MainWindow mw, File pdfIn) {
        super(mw, "Marca de agua (vista previa)", true);
        this.mw = mw;
        this.pdfIn = pdfIn;

        setMinimumSize(new Dimension(1120, 720));
        setLocationRelativeTo(mw);
        ((JComponent)getContentPane()).setBorder(new EmptyBorder(10,10,10,10));
        setLayout(new BorderLayout(8,8));

        // Cierre seguro del documento
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) { closeDoc(); }
            @Override public void windowClosed (WindowEvent e) { closeDoc(); }
        });

        // Izquierda: miniaturas
        JScrollPane left = new JScrollPane(lstPages);
        left.setPreferredSize(new Dimension(180, 600));
        add(left, BorderLayout.WEST);

        // Centro: preview + zoom
        JPanel center = new JPanel(new BorderLayout(6,6));
        center.add(preview, BorderLayout.CENTER);
        JPanel zoomBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 4));
        zoomBar.add(new JLabel("Zoom:"));
        sZoom.addChangeListener(e -> { zoom = sZoom.getValue()/100f; preview.repaint(); });
        zoomBar.add(sZoom);
        center.add(zoomBar, BorderLayout.SOUTH);
        add(center, BorderLayout.CENTER);

        // Derecha: controles
        JPanel right = new JPanel(new GridBagLayout());
        right.setBorder(new EmptyBorder(0,8,0,0));
        add(right, BorderLayout.EAST);

        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx=0; gc.gridy=0; gc.anchor=GridBagConstraints.WEST; gc.fill=GridBagConstraints.HORIZONTAL;
        gc.insets = new Insets(2,0,2,0);
        gc.weightx = 1;

        right.add(new JLabel("Texto:"), gc); gc.gridy++;
        right.add(txtText, gc); gc.gridy++;

        right.add(new JLabel("Tamaño:"), gc); gc.gridy++;
        right.add(spFont, gc); gc.gridy++;

        right.add(new JLabel("Ángulo:"), gc); gc.gridy++;
        sAngle.setMajorTickSpacing(15);
        sAngle.setPaintTicks(true);
        right.add(sAngle, gc); gc.gridy++;

        right.add(new JLabel("Posición X (0-100%):"), gc); gc.gridy++;
        right.add(sX, gc); gc.gridy++;
        right.add(new JLabel("Posición Y (0-100%):"), gc); gc.gridy++;
        right.add(sY, gc); gc.gridy++;
        right.add(new JLabel("Opacidad (%):"), gc); gc.gridy++;
        right.add(sOpacity, gc); gc.gridy++;

        // Color
        JPanel colorRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        JLabel colorLbl = new JLabel("Color:");
        colorSwatch.setPreferredSize(new Dimension(26,16));
        colorSwatch.setBorder(BorderFactory.createLineBorder(new Color(0,0,0,100)));
        colorSwatch.setBackground(color);
        colorRow.add(colorLbl);
        colorRow.add(colorSwatch);
        colorRow.add(btnColor);
        right.add(colorRow, gc); gc.gridy++;

        // Aplicar a
        right.add(new JLabel("Aplicar a:"), gc); gc.gridy++;
        ButtonGroup gApply = new ButtonGroup(); gApply.add(rbAll); gApply.add(rbOne);
        right.add(rbAll, gc); gc.gridy++;
        right.add(rbOne, gc); gc.gridy++;

        // Botonera
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        buttons.add(btnPrint);
        buttons.add(btnSave);
        right.add(buttons, gc); gc.gridy++;

        // Listeners
        var repaintListener = (javax.swing.event.ChangeListener) e -> onChange();
        spFont.addChangeListener(repaintListener);
        sAngle.addChangeListener(repaintListener);
        sX.addChangeListener(repaintListener);
        sY.addChangeListener(repaintListener);
        sOpacity.addChangeListener(repaintListener);
        txtText.getDocument().addDocumentListener(new SimpleDocListener(this::onChange));

        btnColor.addActionListener(e -> {
            Color c = JColorChooser.showDialog(this, "Color de marca", color);
            if (c != null) {
                color = c;
                colorSwatch.setBackground(color);
                onChange();
            }
        });

        btnPrint.addActionListener(e -> doPrint());
        btnSave.addActionListener(e -> saveResult());

        loadDoc();
    }

    private void onChange() {
        preview.repaint();
        thumbCache.clear();
        lstPages.repaint();
    }

    private void loadDoc() {
        try {
            doc = Loader.loadPDF(pdfIn);
            renderer = new PDFRenderer(doc);

            Integer[] ids = new Integer[doc.getNumberOfPages()];
            for (int i=0;i<ids.length;i++) ids[i] = i;
            lstPages.setListData(ids);
            lstPages.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            if (ids.length > 0) lstPages.setSelectedIndex(0);
            lstPages.addListSelectionListener(e -> preview.repaint());

            lstPages.setCellRenderer(new PageThumbRenderer());
            // Dejo selección “plana” y marco yo con borde en el renderer
            lstPages.setSelectionBackground(lstPages.getBackground());
            lstPages.setSelectionForeground(lstPages.getForeground());
            lstPages.setFixedCellWidth(THUMB_W + 24);
            lstPages.setFixedCellHeight((int)Math.ceil(THUMB_W*1.30)+34);
            lstPages.setVisibleRowCount(-1);

        } catch (Exception ex) {
            mw.fail(ex);
            dispose();
        }
    }

    private void saveResult() {
        try {
            String baseName = pdfIn.getName().replaceAll("(?i)\\.pdf$","");
            File out = FxFileDialogs.pickSave(
                this,
                "Guardar PDF con marca…",
                pdfIn.getParentFile(),
                baseName + "_marca.pdf",
                "pdf"
            );
            if (out == null) return;

            final String text = txtText.getText();
            final float fontSizePt = ((Number)spFont.getValue()).floatValue();
            final float anglePreviewRad = (float) Math.toRadians(sAngle.getValue());
            final float op = sOpacity.getValue()/100f;
            final Color c = color;

            final float nx = sX.getValue()/100f;
            final float nyTop = sY.getValue()/100f;

            final int from, to;
            if (rbOne.isSelected()) {
                int sel = Math.max(0, lstPages.getSelectedIndex());
                from = sel; to = sel;
            } else {
                from = 0; to = doc.getNumberOfPages()-1;
            }

            mw.runAsync(() -> {
                try {
                    for (int i = from; i <= to; i++) {
                        PDPage page = doc.getPage(i);
                        PDRectangle mb = page.getMediaBox();

                        float px = nx * mb.getWidth();
                        float py = (1f - nyTop) * mb.getHeight(); // invertir Y respecto al preview
                        float anglePdf = -anglePreviewRad;        // signo invertido respecto al preview

                        // Estado gráfico (opacidad)
                        PDExtendedGraphicsState gs = new PDExtendedGraphicsState();
                        gs.setNonStrokingAlphaConstant(op);

                        try (PDPageContentStream cs =
                                     new PDPageContentStream(doc, page, PDPageContentStream.AppendMode.APPEND, true, true)) {
                            cs.setNonStrokingColor(c);
                            // PDFBox se encarga de registrar el ExtGState en los resources del page
                            cs.setGraphicsStateParameters(gs);
                            cs.beginText();
                            cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), fontSizePt);
                            cs.setTextMatrix(Matrix.getRotateInstance(anglePdf, px, py));
                            cs.showText(text);
                            cs.endText();
                        }
                    }

                    doc.save(out);
                    SwingUtilities.invokeLater(() -> {
                        mw.ok("PDF con marca guardado: " + out.getAbsolutePath());
                        mw.getHistory().add("Marca de agua (preview)", List.of(pdfIn.getAbsolutePath()), out.getAbsolutePath());
                        mw.openIfWanted(out);
                        dispose();
                    });
                } catch (Exception ex) {
                    mw.fail(ex);
                }
            });

        } catch (Exception ex) {
            mw.fail(ex);
        }
    }

    private void doPrint() {
        try {
            final List<BufferedImage> imgs = new ArrayList<>();
            final int from, to;
            if (rbAll.isSelected()) {
                from = 0; to = doc.getNumberOfPages()-1;
            } else {
                int sel = Math.max(0, lstPages.getSelectedIndex());
                from = sel; to = sel;
            }

            mw.runAsync(() -> {
                try {
                    for (int i = from; i <= to; i++) {
                        BufferedImage base = renderer.renderImageWithDPI(i, PREVIEW_DPI, ImageType.RGB);
                        BufferedImage withWM = drawWatermarkOn(base, PREVIEW_DPI);
                        imgs.add(withWM);
                    }
                    PrintUtils.printThumbnails(imgs);
                } catch (Exception ex) {
                    mw.fail(ex);
                }
            });
        } catch (Exception ex) {
            mw.fail(ex);
        }
    }

    private class PageThumbRenderer extends JPanel implements ListCellRenderer<Integer> {
        private final JLabel previewLbl = new JLabel("", SwingConstants.CENTER);
        private final JLabel caption = new JLabel("", SwingConstants.CENTER);

        private final Border selBorder =
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(UIManager.getColor("Component.focusColor")),
                        new EmptyBorder(6,6,8,6)
                );
        private final Border noSelBorder = new EmptyBorder(7,7,9,7);

        PageThumbRenderer() {
            setOpaque(true);
            setLayout(new BorderLayout(0, 6));
            previewLbl.setOpaque(false);
            caption.setOpaque(false);
            caption.setFont(caption.getFont().deriveFont(Font.PLAIN, 12f));
            caption.setForeground(UIManager.getColor("Label.foreground"));
            add(previewLbl, BorderLayout.CENTER);
            add(caption, BorderLayout.SOUTH);
        }

        @Override public Component getListCellRendererComponent(
                JList<? extends Integer> list, Integer pageIndex,
                int index, boolean isSelected, boolean cellHasFocus) {

            setBackground(list.getBackground());
            setBorder(isSelected ? selBorder : noSelBorder);

            previewLbl.setIcon(getThumbIcon(pageIndex));
            caption.setText("Página " + (index + 1));
            return this;
        }
    }

    private ImageIcon getThumbIcon(int pageIndex) {
        ImageIcon icon = thumbCache.get(pageIndex);
        if (icon != null) return icon;

        try {
            final int BASE_DPI = 110;
            BufferedImage page = renderer.renderImageWithDPI(pageIndex, BASE_DPI, ImageType.RGB);
            BufferedImage withWM = drawWatermarkOn(page, BASE_DPI);

            int w = THUMB_W;
            int h = withWM.getHeight() * w / withWM.getWidth();
            BufferedImage thumb = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = thumb.createGraphics();
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.drawImage(withWM, 0, 0, w, h, null);
            g2.dispose();

            icon = new ImageIcon(thumb);
            thumbCache.put(pageIndex, icon);
            return icon;
        } catch (Exception ex) {
            BufferedImage blank = new BufferedImage(THUMB_W, (int)(THUMB_W*1.3), BufferedImage.TYPE_INT_ARGB);
            return new ImageIcon(blank);
        }
    }

    private class Preview extends JPanel {
        private Rectangle lastDraw = new Rectangle();

        Preview(){
            setBackground(new Color(40,40,40));
            var ma = new java.awt.event.MouseAdapter() {
                private boolean dragging = false;
                @Override public void mousePressed(java.awt.event.MouseEvent e) {
                    if (updateFromMouse(e.getX(), e.getY())) dragging = true;
                }
                @Override public void mouseDragged(java.awt.event.MouseEvent e) {
                    if (dragging) updateFromMouse(e.getX(), e.getY());
                }
                @Override public void mouseReleased(java.awt.event.MouseEvent e) { dragging = false; }
            };
            addMouseListener(ma);
            addMouseMotionListener(ma);
        }

        private boolean updateFromMouse(int mx, int my) {
            if (!lastDraw.contains(mx, my)) return false;
            float rx = (mx - lastDraw.x) / (float) lastDraw.width;
            float ry = (my - lastDraw.y) / (float) lastDraw.height;
            rx = Math.min(1, Math.max(0, rx));
            ry = Math.min(1, Math.max(0, ry));
            sX.setValue(Math.round(rx * 100));
            sY.setValue(Math.round(ry * 100));
            repaint();
            return true;
        }

        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (doc == null) return;
            int sel = lstPages.getSelectedIndex();
            int pageIndex = Math.max(0, sel < 0 ? 0 : sel);
            try {
                BufferedImage base = renderer.renderImageWithDPI(pageIndex, PREVIEW_DPI, ImageType.RGB);
                BufferedImage withWM = drawWatermarkOn(base, PREVIEW_DPI);

                int availW = getWidth() - 40, availH = getHeight() - 40;
                int w = (int) (base.getWidth() * zoom);
                int h = (int) (base.getHeight()* zoom);
                if (w > availW || h > availH) {
                    double s = Math.min(availW/(double)w, availH/(double)h);
                    w = (int) (w * s);
                    h = (int) (h * s);
                }
                int x = (getWidth() - w)/2, y = (getHeight() - h)/2;
                ((Graphics2D)g).setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g.drawImage(withWM, x, y, w, h, null);
                lastDraw.setBounds(x, y, w, h);
            } catch (Exception ignore) {}
        }
    }

    private BufferedImage drawWatermarkOn(BufferedImage base, int dpi) {
        BufferedImage copy = new BufferedImage(base.getWidth(), base.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = copy.createGraphics();
        g2.drawImage(base, 0, 0, null);

        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        String text = txtText.getText();
        float fontSizePt = ((Number)spFont.getValue()).floatValue();
        float fontSizePx = fontSizePt * dpi / 72f;
        g2.setFont(g2.getFont().deriveFont(Font.BOLD, fontSizePx));
        g2.setColor(color);
        g2.setComposite(AlphaComposite.SrcOver.derive(sOpacity.getValue()/100f));

        float px = (sX.getValue()/100f) * base.getWidth();
        float py = (sY.getValue()/100f) * base.getHeight();

        float angle = (float) Math.toRadians(sAngle.getValue());
        g2.translate(px, py);
        g2.rotate(angle);
        g2.drawString(text, 0, 0);

        g2.dispose();
        return copy;
    }

    private static class SimpleDocListener implements javax.swing.event.DocumentListener {
        private final Runnable r;
        SimpleDocListener(Runnable r){ this.r = r; }
        @Override public void insertUpdate(javax.swing.event.DocumentEvent e) { r.run(); }
        @Override public void removeUpdate(javax.swing.event.DocumentEvent e) { r.run(); }
        @Override public void changedUpdate(javax.swing.event.DocumentEvent e) { r.run(); }
    }

    private void closeDoc() {
        try { if (doc != null) doc.close(); } catch (Exception ignore) {}
        doc = null; renderer = null;
    }
}
