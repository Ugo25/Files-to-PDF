package com.mycompany.programa_pdf.dialogs;

import com.mycompany.programa_pdf.io.FxFileDialogs;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.printing.PDFPageable;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;

import java.awt.Desktop;
import com.mycompany.programa_pdf.MainWindow;


import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.awt.print.PrinterJob;
import java.io.File;
import java.io.IOException;
import java.lang.ref.SoftReference;
import java.util.LinkedHashMap;
import java.util.Map;

public class PdfPreviewDialog extends JDialog {

    // ====== Modelo/render ======
    private final PDDocument doc;
    private final PDFRenderer renderer;

    // Archivo fuente (para sugerir nombre/carpeta al guardar)
    private final File sourceFile;

    // ====== UI ======
    private final JList<Integer> thumbs = new JList<>();
    private final PreviewCanvas canvas = new PreviewCanvas();
    private JScrollPane spCanvas;
    private final JComboBox<String> cmbZoom;
    private final JSlider sZoom;

    // ====== Estado ======
    private int page = 0;
    private float zoom = 1.0f; // 1.0 = 100 %
    private final int screenDpi = Toolkit.getDefaultToolkit().getScreenResolution(); // ~96 en Windows
    private boolean updatingUi = false;

    private enum ZoomMode { MANUAL, FIT_PAGE, FIT_WIDTH }
    private ZoomMode zoomMode = ZoomMode.FIT_PAGE;

    private static final float ZOOM_MIN = 0.05f;
    private static final float ZOOM_MAX = 8.00f;

    private static final int MARGIN = 20;

    // Cache de thumbnails
    private final Map<Integer, ImageIcon> thumbCache = new LinkedHashMap<>(64, 0.75f, true) {
        @Override protected boolean removeEldestEntry(Map.Entry<Integer, ImageIcon> e) {
            return size() > 80;
        }
    };

    // Cache de páginas renderizadas (por página + dpi)
    private static final class CacheKey {
        final int page, dpi; CacheKey(int p, int d){ page=p; dpi=d; }
        @Override public boolean equals(Object o){ return (o instanceof CacheKey k) && k.page==page && k.dpi==dpi; }
        @Override public int hashCode(){ return 31*page + dpi; }
    }
    private final Map<CacheKey, SoftReference<BufferedImage>> pageCache =
            new LinkedHashMap<>(16, 0.75f, true) {
                @Override protected boolean removeEldestEntry(Map.Entry<CacheKey, SoftReference<BufferedImage>> e) {
                    return size() > 10;
                }
            };

    // === Constructores ===
    public PdfPreviewDialog(Window owner, PDDocument doc, String title) {
        this(owner, doc, title, null);
    }
    public PdfPreviewDialog(Window owner, PDDocument doc, String title, File sourceFile) {
        super(owner, title, ModalityType.APPLICATION_MODAL);
        this.doc = doc;
        this.renderer = new PDFRenderer(doc);
        this.sourceFile = sourceFile;

        setLayout(new BorderLayout(8, 8));
        ((JComponent) getContentPane()).setBorder(new EmptyBorder(8, 8, 8, 8));
        setMinimumSize(new Dimension(1000, 700));
        fitToScreen(owner, true);

        // ====== Thumbnails ======
        Integer[] ids = new Integer[doc.getNumberOfPages()];
        for (int i=0;i<ids.length;i++) ids[i]=i;

        thumbs.setListData(ids);
        thumbs.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        thumbs.setSelectedIndex(0);
        thumbs.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int idx = Math.max(0, thumbs.getSelectedIndex());
                if (idx != page) { page = idx; applyZoom(); }
            }
        });
        thumbs.setCellRenderer(new DefaultListCellRenderer() {
            @Override public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                JLabel l = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                try {
                    ImageIcon icon = thumbCache.get(index);
                    if (icon == null) {
                        BufferedImage bim = renderer.renderImageWithDPI(index, 64, ImageType.RGB);
                        int w = 110, h = bim.getHeight() * w / bim.getWidth();
                        Image scaled = bim.getScaledInstance(w, h, Image.SCALE_SMOOTH);
                        icon = new ImageIcon(scaled);
                        thumbCache.put(index, icon);
                    }
                    l.setIcon(icon);
                } catch (Exception ignore) {}
                l.setText("Página " + (index + 1));
                l.setHorizontalTextPosition(SwingConstants.CENTER);
                l.setVerticalTextPosition(SwingConstants.BOTTOM);
                return l;
            }
        });
        JScrollPane spThumbs = new JScrollPane(thumbs);
        spThumbs.setPreferredSize(new Dimension(140, 200));
        add(spThumbs, BorderLayout.WEST);

        // ====== Centro: canvas dentro de scroll ======
        spCanvas = new JScrollPane(canvas);
        spCanvas.getViewport().addComponentListener(new ComponentAdapter() {
            @Override public void componentResized(ComponentEvent e) {
                if (zoomMode == ZoomMode.FIT_PAGE || zoomMode == ZoomMode.FIT_WIDTH) applyZoom();
            }
        });
        add(spCanvas, BorderLayout.CENTER);

        // ====== Barra inferior: Zoom + botones ======
        JPanel south = new JPanel(new BorderLayout());

        String[] zooms = {"Ajustar", "Ajustar ancho", "25%", "50%", "75%", "100%", "125%", "150%", "200%"};
        cmbZoom = new JComboBox<>(zooms);
        cmbZoom.setEditable(true);
        cmbZoom.setSelectedItem("Ajustar");
        cmbZoom.addActionListener(e -> {
            if (updatingUi) return;
            Object it = cmbZoom.isEditable() ? cmbZoom.getEditor().getItem() : cmbZoom.getSelectedItem();
            String txt = (it == null) ? "" : it.toString().trim();
            if (txt.equalsIgnoreCase("Ajustar")) { zoomMode = ZoomMode.FIT_PAGE;  applyZoom(); }
            else if (txt.equalsIgnoreCase("Ajustar ancho")) { zoomMode = ZoomMode.FIT_WIDTH; applyZoom(); }
            else { float z = parseZoom(txt, zoom); zoomMode = ZoomMode.MANUAL; setZoomPercent(z); }
        });

        sZoom = new JSlider(Math.round(ZOOM_MIN * 100), 400, 100);
        sZoom.setPaintTicks(true);
        sZoom.setMajorTickSpacing(50);
        sZoom.setMinorTickSpacing(5);
        sZoom.addChangeListener(e -> {
            if (sZoom.getValueIsAdjusting() || updatingUi) return;
            zoomMode = ZoomMode.MANUAL;
            setZoomPercent(sZoom.getValue() / 100f);
        });

        canvas.addMouseWheelListener(e -> {
            if (!e.isControlDown()) return;
            zoomMode = ZoomMode.MANUAL;
            float factor = (e.getWheelRotation() < 0) ? 1.10f : 0.90f;
            setZoomPercent(zoom * factor);
            e.consume();
        });

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 4));
        left.add(new JLabel("Zoom:"));
        left.add(cmbZoom);
        left.add(sZoom);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 4));
        // Rotación
        JButton btnRotL = new JButton("⟲ Rotar");
        btnRotL.setToolTipText("Girar 90° a la izquierda (Ctrl+L)");
        btnRotL.addActionListener(e -> rotateCurrentPage(-90));

        JButton btnRotR = new JButton("Rotar ⟳");
        btnRotR.setToolTipText("Girar 90° a la derecha (Ctrl+R)");
        btnRotR.addActionListener(e -> rotateCurrentPage(+90));

        // Atajos
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("control L"), "rotL");
        getRootPane().getActionMap().put("rotL", new AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { rotateCurrentPage(-90); }
        });
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("control R"), "rotR");
        getRootPane().getActionMap().put("rotR", new AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { rotateCurrentPage(+90); }
        });

        JButton btnPrint = new JButton("Imprimir…");
        btnPrint.addActionListener(e -> doPrint());
        JButton btnSave = new JButton("Guardar…");
        btnSave.addActionListener(e -> doSave());
        JButton btnClose = new JButton("Cerrar");
        btnClose.addActionListener(e -> dispose());

        right.add(btnRotL);
        right.add(btnRotR);
        right.add(btnPrint);
        right.add(btnSave);
        right.add(btnClose);

        south.add(left, BorderLayout.WEST);
        south.add(right, BorderLayout.EAST);
        add(south, BorderLayout.SOUTH);

        // Primer render
        applyZoom();

        // Cierre doc asegurado
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosed(WindowEvent e) {
                try { doc.close(); } catch (Exception ignore) {}
            }
        });
        setLocationRelativeTo(owner);
    }

    // ====== Rotación de página ======
    private void rotateCurrentPage(int deltaDegrees) {
        try {
            var pdPage = doc.getPage(page);
            Integer rotObj = pdPage.getRotation();
            int cur = (rotObj == null) ? 0 : rotObj.intValue();
            int next = ((cur + deltaDegrees) % 360 + 360) % 360;
            pdPage.setRotation(next);

            invalidatePageCache(page);
            thumbCache.remove(page);

            applyZoom();
            thumbs.repaint();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "No se pudo rotar la página: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    private void invalidatePageCache(int pageIndex) {
        pageCache.entrySet().removeIf(e -> e.getKey().page == pageIndex);
    }

    // ====== Utilidades de zoom ======
    private float parseZoom(String s, float def) {
        if (s == null) return def;
        s = s.trim().replace("%", "").replace(",", ".");
        try {
            float v = Float.parseFloat(s) / 100f;
            if (Float.isNaN(v) || Float.isInfinite(v)) return def;
            return Math.max(ZOOM_MIN, Math.min(ZOOM_MAX, v));
        } catch (Exception ignore) { return def; }
    }

    private void setZoomPercent(float z) {
        z = Math.max(ZOOM_MIN, Math.min(ZOOM_MAX, z));
        if (Math.abs(z - zoom) < 0.001f) return;

        zoom = z;
        updatingUi = true;
        try {
            String label = Math.round(zoom * 100) + "%";
            if (cmbZoom.isEditable()) {
                Object it = cmbZoom.getEditor().getItem();
                if (it == null || !label.equals(it.toString())) cmbZoom.getEditor().setItem(label);
            } else {
                if (!label.equals(cmbZoom.getSelectedItem())) cmbZoom.setSelectedItem(label);
            }
            int v = Math.round(zoom * 100);
            int min = sZoom.getMinimum(), max = sZoom.getMaximum();
            if (v < min) v = min; else if (v > max) v = max;
            if (sZoom.getValue() != v) sZoom.setValue(v);
        } finally {
            updatingUi = false;
        }
        canvas.renderPage();
    }

    private void applyZoom() {
        if (zoomMode == ZoomMode.FIT_PAGE || zoomMode == ZoomMode.FIT_WIDTH) {
            float fit = (zoomMode == ZoomMode.FIT_PAGE) ? computeFitPageZoom() : computeFitWidthZoom();
            updatingUi = true;
            try {
                String label = (zoomMode == ZoomMode.FIT_PAGE) ? "Ajustar" : "Ajustar ancho";
                if (!label.equals(cmbZoom.getSelectedItem())) cmbZoom.setSelectedItem(label);
                int v = Math.round(fit * 100);
                v = Math.max(sZoom.getMinimum(), Math.min(sZoom.getMaximum(), v));
                if (sZoom.getValue() != v) sZoom.setValue(v);
            } finally { updatingUi = false; }
            zoom = fit;
            canvas.renderPage();
        } else {
            setZoomPercent(zoom);
        }
    }

    private float computeFitPageZoom() {
        Dimension view = spCanvas.getViewport().getExtentSize();
        if (view == null || view.width <= 1 || view.height <= 1) return zoom;

        var p = doc.getPage(page).getMediaBox();
        double pageWpx = p.getWidth()  * (screenDpi / 72.0);
        double pageHpx = p.getHeight() * (screenDpi / 72.0);

        double scaleW = (view.width  - 2.0 * MARGIN) / pageWpx;
        double scaleH = (view.height - 2.0 * MARGIN) / pageHpx;
        double fit = Math.max(0.01, Math.min(scaleW, scaleH));
        return (float) Math.max(ZOOM_MIN, Math.min(ZOOM_MAX, fit));
    }

    private float computeFitWidthZoom() {
        Dimension view = spCanvas.getViewport().getExtentSize();
        if (view == null || view.width <= 1) return zoom;

        var p = doc.getPage(page).getMediaBox();
        double pageWpx = p.getWidth() * (screenDpi / 72.0);

        double scaleW = (view.width - 2.0 * MARGIN) / pageWpx;
        double fit = Math.max(0.01, scaleW);
        return (float) Math.max(ZOOM_MIN, Math.min(ZOOM_MAX, fit));
    }

    // ====== Acciones ======
    private void doPrint() {
        try {
            PrinterJob job = PrinterJob.getPrinterJob();
            job.setPageable(new PDFPageable(doc));
            if (job.printDialog()) job.print();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.toString(), "Error al imprimir", JOptionPane.ERROR_MESSAGE);
        }
    }

   private void doSave() {
    try {
        File initialDir = (sourceFile != null && sourceFile.getParentFile() != null)
                ? sourceFile.getParentFile()
                : new File(System.getProperty("user.home"));

        String defaultName = (sourceFile != null) ? sourceFile.getName() : "documento.pdf";
        if (!defaultName.toLowerCase().endsWith(".pdf")) defaultName += ".pdf";

        File out = FxFileDialogs.pickSave(this, "Guardar PDF como…", initialDir, defaultName, "pdf");
        if (out == null) return;

        doc.save(out);

        // 👇 abrir el PDF guardado
        Window owner = getOwner();
        if (owner instanceof MainWindow mw) {
            mw.openIfWanted(out);
        } else {
            try { Desktop.getDesktop().open(out); } catch (Exception ignore) {}
        }
    } catch (Exception ex) {
        JOptionPane.showMessageDialog(this, ex.toString(), "Error al guardar", JOptionPane.ERROR_MESSAGE);
    }
}

    @Override public void dispose() {
        super.dispose();
    }

    // ========= Canvas =========
    private class PreviewCanvas extends JComponent {
        private volatile BufferedImage img;
        private int imgPage = -1, imgDpi = -1;
        private SwingWorker<BufferedImage, Void> task;
        private volatile boolean loading = false;
        private int job = 0;

        PreviewCanvas() {
            setOpaque(true);
            setBackground(new Color(40,40,40));
            setDoubleBuffered(true);
        }

        private int computeDpi() {
            int raw = Math.round(screenDpi * zoom);
            int snapped = Math.round(raw / 12f) * 12;
            return Math.max(72, Math.min(240, snapped));
        }

        private Dimension expectedSizeUsingEffectiveZoom(int pageIndex) {
            var p = doc.getPage(pageIndex).getMediaBox();
            int effDpi = Math.max(1, Math.round(screenDpi * zoom));
            int w = Math.max(1, Math.round(p.getWidth()  * effDpi / 72f));
            int h = Math.max(1, Math.round(p.getHeight() * effDpi / 72f));
            return new Dimension(w + 2*MARGIN, h + 2*MARGIN);
        }

        void renderPage() {
            final int targetPage = page;
            final int dpi = computeDpi();

            Dimension pref = expectedSizeUsingEffectiveZoom(targetPage);
            setPreferredSize(pref);
            revalidate();

            BufferedImage cached = fromCache(targetPage, dpi);
            if (cached != null) {
                img = cached; imgPage = targetPage; imgDpi = dpi;
                repaint();
                prefetchNeighbors(dpi);
                return;
            }

            loading = true;
            repaint();

            if (task != null) task.cancel(true);
            final int thisJob = ++job;

            task = new SwingWorker<>() {
                @Override protected BufferedImage doInBackground() throws Exception {
                    return renderer.renderImageWithDPI(targetPage, dpi, ImageType.RGB);
                }
                @Override protected void done() {
                    if (isCancelled() || thisJob != job) return;
                    try {
                        BufferedImage bi = get();
                        toCache(targetPage, dpi, bi);
                        img = bi; imgPage = targetPage; imgDpi = dpi;
                    } catch (Exception ignore) {
                    } finally {
                        loading = false;
                        repaint();
                        prefetchNeighbors(dpi);
                    }
                }
            };
            task.execute();
        }

        private BufferedImage fromCache(int page, int dpi) {
            var ref = pageCache.get(new CacheKey(page, dpi));
            return ref == null ? null : ref.get();
        }
        private void toCache(int page, int dpi, BufferedImage bi) {
            pageCache.put(new CacheKey(page, dpi), new SoftReference<>(bi));
        }
        private void prefetchNeighbors(int dpi) {
            prefetch(page - 1, dpi);
            prefetch(page + 1, dpi);
        }
        private void prefetch(int p, int dpi) {
            if (p < 0 || p >= doc.getNumberOfPages()) return;
            if (fromCache(p, dpi) != null) return;
            new SwingWorker<BufferedImage, Void>() {
                @Override protected BufferedImage doInBackground() throws Exception {
                    return renderer.renderImageWithDPI(p, dpi, ImageType.RGB);
                }
                @Override protected void done() {
                    try { toCache(p, dpi, get()); } catch (Exception ignore) {}
                }
            }.execute();
        }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setComposite(AlphaComposite.Src);
            g2.setColor(getBackground());
            g2.fillRect(0, 0, getWidth(), getHeight());

            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

            BufferedImage local = img;
            if (local != null) {
                double drawScale = (screenDpi * zoom) / Math.max(1.0, imgDpi);
                int drawW = Math.max(1, (int) Math.round(local.getWidth()  * drawScale));
                int drawH = Math.max(1, (int) Math.round(local.getHeight() * drawScale));
                int x = (getWidth()  - drawW) / 2;
                int y = (getHeight() - drawH) / 2;
                g2.drawImage(local, x, y, drawW, drawH, null);
            }

            if (loading) {
                g2.setColor(new Color(0, 0, 0, 80));
                g2.fillRoundRect(10, 10, 160, 28, 12, 12);
                g2.setColor(Color.WHITE);
                g2.setFont(getFont().deriveFont(Font.BOLD, 13f));
                g2.drawString("Renderizando…", 20, 30);
            }
            g2.dispose();
        }
    }

    // === Helper estático: abrir PDF desde File ===
    public static void showFile(Window owner, File pdfFile) {
        if (pdfFile == null) return;
        PDDocument d = null;
        try {
            d = Loader.loadPDF(pdfFile);
            PdfPreviewDialog dlg = new PdfPreviewDialog(owner, d, pdfFile.getName(), pdfFile);
            dlg.setLocationRelativeTo(owner);
            dlg.setVisible(true);
        } catch (Exception ex) {
            if (d != null) try { d.close(); } catch (IOException ignore) {}
            ex.printStackTrace();
        }
    }

    private void fitToScreen(Window anchor, boolean withMargin) {
        GraphicsConfiguration gc = (anchor != null)
                ? anchor.getGraphicsConfiguration()
                : GraphicsEnvironment.getLocalGraphicsEnvironment()
                .getDefaultScreenDevice().getDefaultConfiguration();

        Rectangle b = gc.getBounds();
        Insets ins = Toolkit.getDefaultToolkit().getScreenInsets(gc);

        int w = b.width  - ins.left - ins.right;
        int h = b.height - ins.top  - ins.bottom;

        if (withMargin) {
            w = (int) Math.round(w * 0.96);
            h = (int) Math.round(h * 0.96);
        }

        setBounds(b.x + ins.left, b.y + ins.top, w, h);
        setMinimumSize(new Dimension(800, 600));
    }
}
