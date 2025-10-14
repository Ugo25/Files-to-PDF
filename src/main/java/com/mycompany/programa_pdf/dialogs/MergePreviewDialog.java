package com.mycompany.programa_pdf.dialogs;

import com.mycompany.programa_pdf.MainWindow;
import com.mycompany.programa_pdf.io.FxFileDialogs;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.printing.PDFPageable;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.print.PrinterJob;
import java.io.File;
import java.util.List;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Unir PDFs (vista previa) con:
 * - Lista reordenable con miniaturas y número de páginas
 * - Preview grande con slider de página
 * - Guardar con explorador nativo (FxFileDialogs)
 * - Imprimir… (une en memoria y abre el diálogo de impresión)
 */
public class MergePreviewDialog extends JDialog {

    private final MainWindow mw;

    // Lista y modelo
    private final DefaultListModel<File> model = new DefaultListModel<>();
    private final JList<File> list = new JList<>(model);

    // Preview y slider de página
    private final JLabel previewLbl = new JLabel("", SwingConstants.CENTER);
    private final JSlider sPage = new JSlider(1, 1, 1);
    private final JLabel lblPageInfo = new JLabel("Página 1/1");

    // Cachés (miniaturas y páginas)
    private final Map<File, Image> thumbCache = new ConcurrentHashMap<>();
    private final Map<File, Integer> pagesCache = new ConcurrentHashMap<>();

    public MergePreviewDialog(MainWindow mw, List<File> initialFiles) {
        super(mw, "Unir PDFs (vista previa)", true);
        this.mw = mw;

        setMinimumSize(new Dimension(980, 680));
        setLocationRelativeTo(mw);
        ((JComponent) getContentPane()).setBorder(new EmptyBorder(10, 10, 10, 10));
        setLayout(new BorderLayout(10, 10));

        // ===== izquierda: lista de PDFs =====
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setCellRenderer(new PdfCellRenderer());
        JScrollPane left = new JScrollPane(list);
        left.setPreferredSize(new Dimension(300, 500));

        // ===== centro: preview =====
        previewLbl.setOpaque(true);
        previewLbl.setBackground(new Color(245, 245, 245));
        JScrollPane right = new JScrollPane(previewLbl);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, right);
        split.setResizeWeight(0.42);
        add(split, BorderLayout.CENTER);

        // ===== inferior: barra con tres zonas =====
        JPanel south = new JPanel(new BorderLayout(10, 8));
        south.setOpaque(false);

        // izquierda: añadir/quitar/subir/bajar
        JPanel leftBtns = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        JButton btnAdd = new JButton("Añadir…");
        JButton btnRemove = new JButton("Quitar");
        JButton btnUp = new JButton("↑");
        JButton btnDown = new JButton("↓");
        leftBtns.add(btnAdd);
        leftBtns.add(btnRemove);
        leftBtns.add(btnUp);
        leftBtns.add(btnDown);

        // centro: slider de página
        JPanel centerBar = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        JLabel lblPag = new JLabel("Página:");
        sPage.setPreferredSize(new Dimension(220, sPage.getPreferredSize().height));
        centerBar.add(lblPag);
        centerBar.add(sPage);
        centerBar.add(lblPageInfo);

        // derecha: guardar / imprimir / cancelar
        JPanel rightBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        JButton btnSave = new JButton("Guardar…");
        JButton btnPrint = new JButton("Imprimir…");
        JButton btnCancel = new JButton("Cancelar");
        rightBtns.add(btnSave);
        rightBtns.add(btnPrint);
        rightBtns.add(btnCancel);

        south.add(leftBtns, BorderLayout.WEST);
        south.add(centerBar, BorderLayout.CENTER);
        south.add(rightBtns, BorderLayout.EAST);
        add(south, BorderLayout.SOUTH);

        // ===== datos iniciales =====
        if (initialFiles != null) {
            for (File f : initialFiles) if (isPdf(f)) model.addElement(f);
        }
        if (!model.isEmpty()) list.setSelectedIndex(0);

        // ===== eventos =====
        list.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                updatePageSliderForSelected();
                renderPreviewAsync(list.getSelectedValue(), sPage.getValue());
            }
        });

        sPage.addChangeListener(e -> {
            File sel = list.getSelectedValue();
            if (sel != null && !sPage.getValueIsAdjusting()) {
                renderPreviewAsync(sel, sPage.getValue());
            }
            updatePageInfo();
        });

        btnAdd.addActionListener(e -> onAdd());
        btnRemove.addActionListener(e -> removeSelected());
        btnUp.addActionListener(e -> moveSelected(-1));
        btnDown.addActionListener(e -> moveSelected(+1));
        btnSave.addActionListener(e -> onSave());
        btnPrint.addActionListener(e -> onPrint());
        btnCancel.addActionListener(e -> dispose());

        // primera carga
        updatePageSliderForSelected();
        renderPreviewAsync(list.getSelectedValue(), sPage.getValue());
    }

    /* ==================== Acciones ==================== */

    private void onAdd() {
        File baseDir = commonParentDir(Collections.list(model.elements()));
        List<File> chosen = FxFileDialogs.pickOpenMulti(
                this,
                "Selecciona 1+ PDFs para añadir",
                baseDir != null ? baseDir : new File("."),
                "pdf"
        );
        if (chosen == null || chosen.isEmpty()) return;
        int start = model.getSize();
        for (File f : chosen) if (isPdf(f)) model.addElement(f);
        if (model.getSize() > start) list.setSelectedIndex(start);
    }

    private void removeSelected() {
        int i = list.getSelectedIndex();
        if (i < 0) return;
        File f = model.remove(i);
        thumbCache.remove(f);
        pagesCache.remove(f);
        if (!model.isEmpty()) list.setSelectedIndex(Math.min(i, model.size() - 1));
        else previewLbl.setIcon(null);
        updatePageSliderForSelected();
    }

    private void moveSelected(int delta) {
        int i = list.getSelectedIndex();
        if (i < 0) return;
        int j = i + delta;
        if (j < 0 || j >= model.size()) return;
        File f = model.getElementAt(i);
        model.remove(i);
        model.add(j, f);
        list.setSelectedIndex(j);
    }

    private void onSave() {
        if (model.size() < 2) {
            JOptionPane.showMessageDialog(this, "Necesitas al menos 2 PDFs.", "Unir PDFs", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        List<File> files = Collections.list(model.elements());
        File baseDir = commonParentDir(files);
        String suggested = suggestName(files);

        // Explorador nativo (SWT → AWT fallback)
        File out = FxFileDialogs.pickSave(
                this,
                "Guardar PDF unido como…",
                baseDir != null ? baseDir : new File("."),
                suggested,
                "pdf"
        );
        if (out == null) return;

        mw.runAsync(() -> {
            try {
                File res = mw.getSvc().mergePdfs(files, out);
                mw.ok("PDF unido: " + res.getAbsolutePath());
                mw.getHistory().add(
                        "Unir PDFs (preview)",
                        files.stream().map(File::getAbsolutePath).toList(),
                        res.getAbsolutePath()
                );
                PdfPreviewDialog.showFile(mw, res);
                mw.openIfWanted(res);
                dispose();
            } catch (Exception ex) {
                mw.fail(ex);
            }
        });
    }

    private void onPrint() {
        if (model.size() < 1) {
            JOptionPane.showMessageDialog(this, "No hay PDFs para imprimir.", "Imprimir", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        // Une en memoria y abre el diálogo de impresión nativo
        mw.runAsync(() -> {
            try (PDDocument merged = mergeInMemory(Collections.list(model.elements()))) {
                PrinterJob job = PrinterJob.getPrinterJob();
                job.setPageable(new PDFPageable(merged));
                if (job.printDialog()) job.print();
            } catch (Exception ex) {
                mw.fail(ex);
            }
        });
    }

    /* ==================== Preview ==================== */

    private void updatePageSliderForSelected() {
        File sel = list.getSelectedValue();
        int pages = (sel != null) ? getPageCount(sel) : 1;
        if (pages < 1) pages = 1;
        sPage.setMinimum(1);
        sPage.setMaximum(pages);
        if (sPage.getValue() > pages) sPage.setValue(pages);
        if (sPage.getValue() < 1) sPage.setValue(1);
        updatePageInfo();
    }

    private void updatePageInfo() {
        File sel = list.getSelectedValue();
        int p = sPage.getValue();
        int total = (sel != null) ? getPageCount(sel) : 1;
        lblPageInfo.setText("Página " + p + "/" + Math.max(total, 1));
    }

    private void renderPreviewAsync(File file, int page) {
        if (file == null) { previewLbl.setIcon(null); return; }
        int pageIndex = Math.max(0, page - 1);

        new SwingWorker<Image, Void>() {
            @Override protected Image doInBackground() {
                try (PDDocument doc = Loader.loadPDF(file)) {
                    PDFRenderer r = new PDFRenderer(doc);
                    // 144 dpi para nítido; usamos ImageType.RGB
                    BufferedImage img = r.renderImageWithDPI(pageIndex, 144, ImageType.RGB);
                    return img;
                } catch (Exception ignore) { return null; }
            }
            @Override protected void done() {
                try {
                    Image img = get();
                    setPreviewScaled(img);
                } catch (Exception ignore) { setPreviewScaled(null); }
            }
        }.execute();
    }

    private void setPreviewScaled(Image img) {
        if (img == null) { previewLbl.setIcon(null); return; }
        int availW = Math.max(200, previewLbl.getParent().getWidth() - 40);
        int availH = Math.max(200, previewLbl.getParent().getHeight() - 40);
        int w = img.getWidth(null), h = img.getHeight(null);
        double s = Math.min(availW / (double) w, availH / (double) h);
        if (s > 1.0) s = 1.0;
        Image scaled = img.getScaledInstance((int)(w * s), (int)(h * s), Image.SCALE_SMOOTH);
        previewLbl.setIcon(new ImageIcon(scaled));
    }

    /* ==================== Utilidades ==================== */

    private static boolean isPdf(File f) {
        if (f == null || !f.isFile()) return false;
        String n = f.getName().toLowerCase(Locale.ROOT);
        return n.endsWith(".pdf");
    }

    private static File commonParentDir(List<File> files) {
        if (files == null || files.isEmpty()) return null;
        File parent = files.get(0).getParentFile();
        if (parent == null) return null;
        for (File f : files) {
            File p = (f == null ? null : f.getParentFile());
            if (p == null || !p.equals(parent)) return parent;
        }
        return parent;
    }

    private static String suggestName(List<File> files) {
        if (files == null || files.isEmpty()) return "unidos.pdf";
        String n = files.get(0).getName();
        int i = n.toLowerCase(Locale.ROOT).lastIndexOf(".pdf");
        String base = (i > 0) ? n.substring(0, i) : n;
        return base + "_unido.pdf";
    }

    private int getPageCount(File f) {
        Integer cached = pagesCache.get(f);
        if (cached != null) return cached;
        try (PDDocument doc = Loader.loadPDF(f)) {
            int p = doc.getNumberOfPages();
            pagesCache.put(f, p);
            return p;
        } catch (Exception e) {
            pagesCache.put(f, 1);
            return 1;
        }
    }

    private Image getThumb(File f) {
        Image cached = thumbCache.get(f);
        if (cached != null) return cached;
        try (PDDocument doc = Loader.loadPDF(f)) {
            int pages = doc.getNumberOfPages();
            pagesCache.put(f, pages);
            PDFRenderer r = new PDFRenderer(doc);
            BufferedImage img = r.renderImageWithDPI(0, 96, ImageType.RGB);
            thumbCache.put(f, img);
            return img;
        } catch (Exception e) {
            return null;
        }
    }

    /** Une en memoria usando PDFBox (para imprimir). */
    private static PDDocument mergeInMemory(List<File> files) throws Exception {
        // Cargamos y fusionamos con PDFMergerUtility
        PDFMergerUtility ut = new PDFMergerUtility();
        PDDocument target = new PDDocument();
        for (File f : files) {
            try (PDDocument src = Loader.loadPDF(f)) {
                ut.appendDocument(target, src);
            }
        }
        return target; // caller cierra
    }

    /* ============ Renderer de la lista (miniatura + nombre + "N pág.") ============ */
    private class PdfCellRenderer extends JPanel implements ListCellRenderer<File> {
        private final JLabel thumb = new JLabel();
        private final JLabel name = new JLabel();
        private final JLabel sub = new JLabel();

        PdfCellRenderer() {
            setOpaque(true);
            setLayout(new BorderLayout(8, 6));
            setBorder(new EmptyBorder(6, 6, 6, 6));

            thumb.setPreferredSize(new Dimension(64, 64));
            name.setFont(name.getFont().deriveFont(Font.BOLD, 12f));
            sub.setFont(sub.getFont().deriveFont(Font.PLAIN, 11f));
            sub.setForeground(new Color(130, 130, 130));

            JPanel center = new JPanel();
            center.setOpaque(false);
            center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
            center.add(name);
            center.add(Box.createVerticalStrut(2));
            center.add(sub);

            add(thumb, BorderLayout.WEST);
            add(center, BorderLayout.CENTER);
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends File> list, File value, int index, boolean isSelected, boolean cellHasFocus) {
            if (value != null) {
                name.setText(value.getName());
                int p = getPageCount(value);
                sub.setText(p + (p == 1 ? " pág." : " pág."));

                Image t = getThumb(value);
                if (t != null) {
                    int w = 64, h = (int) (t.getHeight(null) * (64.0 / t.getWidth(null)));
                    thumb.setIcon(new ImageIcon(t.getScaledInstance(w, Math.max(48, h), Image.SCALE_SMOOTH)));
                } else {
                    thumb.setIcon(null);
                }
            } else {
                name.setText("");
                sub.setText("");
                thumb.setIcon(null);
            }

            if (isSelected) {
                setBackground(list.getSelectionBackground());
                setForeground(list.getSelectionForeground());
            } else {
                setBackground(list.getBackground());
                setForeground(list.getForeground());
            }
            return this;
        }
    }
}