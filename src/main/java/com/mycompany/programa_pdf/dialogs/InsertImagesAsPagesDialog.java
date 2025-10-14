package com.mycompany.programa_pdf.dialogs;

import com.mycompany.programa_pdf.MainWindow;
import com.mycompany.programa_pdf.dialogs.images.PageItem;
import com.mycompany.programa_pdf.dialogs.images.PageThumbRenderer;
import com.mycompany.programa_pdf.io.FxFileDialogs;
import com.mycompany.programa_pdf.pdf.ImagesAsPagesComposer;
import com.mycompany.programa_pdf.ui.dnd.PageItemReorderAndFileDropHandler;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.printing.PDFPageable;
import org.apache.pdfbox.rendering.PDFRenderer;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.print.PrinterJob;
import java.io.File;
import java.util.List;

/**
 * Insertar imágenes como páginas nuevas en un PDF (UI fina que usa helpers externos).
 */
public class InsertImagesAsPagesDialog extends JDialog {

    private final DefaultListModel<PageItem> model = new DefaultListModel<>();
    private final JList<PageItem> list = new JList<>(model);
    private final JLabel bigPreview = new JLabel("", SwingConstants.CENTER);
    private final JSlider zoom = new JSlider(10, 300, 100);

    private final JButton btnAdd   = new JButton("Añadir imágenes…");
    private final JButton btnRemove= new JButton("Eliminar");
    private final JButton btnRotL  = new JButton("↺ 90°");
    private final JButton btnRotR  = new JButton("↻ 90°");
    private final JButton btnUp    = new JButton("Subir");
    private final JButton btnDown  = new JButton("Bajar");
    private final JButton btnPrint = new JButton("Imprimir");
    private final JButton btnSave  = new JButton("Guardar…");
    private final JButton btnClose = new JButton("Cerrar");

    private final MainWindow mw;
    private final File pdfFile;

    private PDDocument srcDoc;
    private PDFRenderer renderer;

    public InsertImagesAsPagesDialog(MainWindow mw, File pdfFile) throws Exception {
        super(mw, "Insertar imágenes como páginas", ModalityType.MODELESS);
        this.mw = mw; this.pdfFile = pdfFile;

        srcDoc = Loader.loadPDF(pdfFile);
        renderer = new PDFRenderer(srcDoc);

        buildUI();
        wireActions();
        loadInitial();

        setSize(1120, 680);
        setLocationRelativeTo(mw);
    }

    private void buildUI() {
        JPanel root = new JPanel(new BorderLayout(8,8));
        root.setBorder(new EmptyBorder(8,8,8,8));
        setContentPane(root);

        JToolBar tb = new JToolBar();
        tb.setFloatable(false);
        tb.add(btnAdd);
        tb.add(btnRemove);
        tb.addSeparator();
        tb.add(btnRotL); tb.add(btnRotR);
        tb.addSeparator();
        tb.add(btnUp); tb.add(btnDown);
        tb.addSeparator();
        tb.add(btnPrint); tb.add(btnSave);
        tb.add(Box.createHorizontalGlue()); tb.add(btnClose);
        root.add(tb, BorderLayout.NORTH);

        list.setLayoutOrientation(JList.HORIZONTAL_WRAP);
        list.setVisibleRowCount(-1);
        list.setFixedCellWidth(200);
        list.setFixedCellHeight(260);
        list.setBorder(new EmptyBorder(6,6,6,6));
        list.setCellRenderer(new PageThumbRenderer(renderer));
        list.setDragEnabled(true);
        list.setDropMode(DropMode.INSERT);
        list.setTransferHandler(new PageItemReorderAndFileDropHandler(list, model));
        list.addListSelectionListener(e -> { refreshBigPreview(); updateButtons(); });

        JScrollPane left = new JScrollPane(list);
        left.setBorder(new EmptyBorder(4,4,4,4));

        JPanel right = new JPanel(new BorderLayout());
        right.add(new JScrollPane(bigPreview), BorderLayout.CENTER);
        JPanel southR = new JPanel(new BorderLayout());
        JPanel zoomPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        zoomPanel.add(new JLabel("Zoom:"));
        zoom.setPreferredSize(new Dimension(260, zoom.getPreferredSize().height));
        zoomPanel.add(zoom);
        southR.add(zoomPanel, BorderLayout.WEST);
        right.add(southR, BorderLayout.SOUTH);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, right);
        split.setResizeWeight(0.36);
        root.add(split, BorderLayout.CENTER);

        JLabel hint = new JLabel("<html>Consejos: arrastra para reordenar • Supr = eliminar • Ctrl+↑/↓ = mover • R/L = rotar ↻/↺</html>");
        hint.setForeground(UIManager.getColor("Label.disabledForeground"));
        hint.setBorder(new EmptyBorder(0, 2, 4, 2));
        root.add(hint, BorderLayout.SOUTH);
    }

    private void wireActions() {
        btnAdd.addActionListener(e -> onAdd());
        btnRemove.addActionListener(e -> removeSelected());
        btnRotL.addActionListener(e -> rotateSelected(-90));
        btnRotR.addActionListener(e -> rotateSelected(+90));
        btnUp.addActionListener(e -> moveSelected(-1));
        btnDown.addActionListener(e -> moveSelected(+1));
        btnSave.addActionListener(e -> onSave());
        btnPrint.addActionListener(e -> onPrint());
        btnClose.addActionListener(e -> dispose());
        zoom.addChangeListener(e -> refreshBigPreview());

        InputMap im = list.getInputMap(JComponent.WHEN_FOCUSED);
        ActionMap am = list.getActionMap();

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "del");
        am.put("del", new AbstractAction() { @Override public void actionPerformed(ActionEvent e) { removeSelected(); } });

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_R, 0), "rotR");
        am.put("rotR", new AbstractAction() { @Override public void actionPerformed(ActionEvent e) { rotateSelected(+90); } });

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_L, 0), "rotL");
        am.put("rotL", new AbstractAction() { @Override public void actionPerformed(ActionEvent e) { rotateSelected(-90); } });

        int ctrl = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, ctrl), "moveUp");
        am.put("moveUp", new AbstractAction() { @Override public void actionPerformed(ActionEvent e) { moveSelected(-1); } });

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, ctrl), "moveDown");
        am.put("moveDown", new AbstractAction() { @Override public void actionPerformed(ActionEvent e) { moveSelected(+1); } });
    }

    private void loadInitial() {
        int n = srcDoc.getNumberOfPages();
        for (int i = 0; i < n; i++) model.addElement(new PageItem(i));
        if (n > 0) list.setSelectedIndex(0);
        updateButtons();
    }

    // ====== Acciones ======
    private void onAdd() {
        List<File> imgs = FxFileDialogs.pickOpenMulti(
                this, "Selecciona imagen(es)", pdfFile.getParentFile(),
                "png","jpg","jpeg","webp","bmp","gif"
        );
        if (imgs == null || imgs.isEmpty()) return;
        for (File f : imgs) model.addElement(new PageItem(f));
        if (list.getSelectedIndex() < 0 && model.size() > 0) list.setSelectedIndex(model.size() - 1);
        updateButtons();
    }

    private void removeSelected() {
        int[] sel = list.getSelectedIndices();
        if (sel == null || sel.length == 0) return;
        for (int i = sel.length - 1; i >= 0; i--) model.remove(sel[i]);
        if (!model.isEmpty()) list.setSelectedIndex(Math.min(model.size() - 1, Math.max(0, sel[0] - 1)));
        updateButtons();
    }

    private void rotateSelected(int delta) {
        int[] idx = list.getSelectedIndices();
        if (idx.length == 0) { Toolkit.getDefaultToolkit().beep(); return; }

        boolean changed = false;
        for (int i : idx) {
            PageItem it = model.get(i);
            if (it.kind == PageItem.Kind.IMAGE_PAGE) { it.rotate(delta); model.set(i, it); changed = true; }
        }
        if (!changed) {
            JOptionPane.showMessageDialog(this,
                    "Selecciona una o más IMÁGENES para rotar.\nLas páginas del PDF no se rotan aquí (usa 'Rotar páginas').",
                    "Nada que rotar", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        list.repaint();
        refreshBigPreview();
        updateButtons();
    }

    private void moveSelected(int dir) {
        int[] sel = list.getSelectedIndices();
        if (sel.length == 0) return;

        if (dir < 0 && sel[0] == 0) return;
        if (dir > 0 && sel[sel.length - 1] == model.size() - 1) return;

        if (dir > 0) {
            for (int i = sel.length - 1; i >= 0; i--) { int from = sel[i], to = from + 1; swap(from, to); sel[i] = to; }
        } else {
            for (int i = 0; i < sel.length; i++) { int from = sel[i], to = from - 1; swap(from, to); sel[i] = to; }
        }
        list.setSelectedIndices(sel);
        list.ensureIndexIsVisible(sel[0]);
        updateButtons();
    }

    private void swap(int i, int j) {
        if (i < 0 || j < 0 || i >= model.size() || j >= model.size()) return;
        PageItem a = model.get(i), b = model.get(j);
        model.set(i, b); model.set(j, a);
    }

    private void onSave() {
        if (model.isEmpty()) return;
        String name = pdfFile.getName();
        int dot = name.lastIndexOf('.');
        String base = (dot > 0) ? name.substring(0, dot) : name;

        File out = FxFileDialogs.pickSave(
                this, "Guardar PDF…",
                pdfFile.getParentFile(),
                base + "_con_imagenes.pdf",
                "pdf"
        );
        if (out == null) return;

        setUIEnabled(false);
        mw.runAsync(() -> {
            try (PDDocument doc = new ImagesAsPagesComposer().compose(srcDoc, java.util.Collections.list(model.elements()))) {
                doc.save(out);
                mw.ok("PDF guardado: " + out.getAbsolutePath());
                mw.openIfWanted(out);
            } catch (Exception ex) {
                mw.fail(ex);
            } finally {
                SwingUtilities.invokeLater(() -> setUIEnabled(true));
            }
        });
    }

    private void onPrint() {
        if (model.isEmpty()) return;
        setUIEnabled(false);
        mw.runAsync(() -> {
            try (PDDocument doc = new ImagesAsPagesComposer().compose(srcDoc, java.util.Collections.list(model.elements()))) {
                PrinterJob job = PrinterJob.getPrinterJob();
                job.setPageable(new PDFPageable(doc));
                if (job.printDialog()) job.print();
            } catch (Exception ex) {
                mw.fail(ex);
            } finally {
                SwingUtilities.invokeLater(() -> setUIEnabled(true));
            }
        });
    }

    // ====== UI state ======
    private void setUIEnabled(boolean en) {
        btnAdd.setEnabled(en);
        btnRemove.setEnabled(en);
        btnRotL.setEnabled(en);
        btnRotR.setEnabled(en);
        btnUp.setEnabled(en);
        btnDown.setEnabled(en);
        btnPrint.setEnabled(en);
        btnSave.setEnabled(en);
        btnClose.setEnabled(en);
        list.setEnabled(en);
        zoom.setEnabled(en);
    }

    private void updateButtons() {
        int[] sel = list.getSelectedIndices();
        boolean hasSel = sel != null && sel.length > 0;

        boolean hasImage = false;
        if (hasSel) for (int i : sel) if (model.get(i).kind == PageItem.Kind.IMAGE_PAGE) { hasImage = true; break; }

        boolean canUp = hasSel && sel[0] > 0;
        boolean canDown = hasSel && sel[sel.length - 1] < model.size() - 1;

        btnRemove.setEnabled(hasSel);
        btnUp.setEnabled(canUp);
        btnDown.setEnabled(canDown);
        btnRotL.setEnabled(hasImage);
        btnRotR.setEnabled(hasImage);
        btnSave.setEnabled(model.size() > 0);
        btnPrint.setEnabled(model.size() > 0);
    }

    private void refreshBigPreview() {
        int idx = list.getSelectedIndex();
        if (idx < 0) { bigPreview.setIcon(null); return; }

        PageItem it = model.get(idx);
        try {
            double z = zoom.getValue() / 100.0;
            if (it.kind == PageItem.Kind.PDF_PAGE) {
                float dpi = (float) (110 * z);
                Image img = renderer.renderImageWithDPI(it.pdfPageIndex, dpi);
                bigPreview.setIcon(new ImageIcon(img));
            } else {
                javax.imageio.ImageIO.setUseCache(false);
                java.awt.image.BufferedImage src = javax.imageio.ImageIO.read(it.imageFile);
                if (src == null) throw new Exception("No se pudo leer " + it.imageFile);
                if (it.rotation != 0) src = com.mycompany.programa_pdf.images.ImageThumbCache.rotate90s(src, it.rotation);
                int w = Math.max(1, (int) Math.round(src.getWidth() * z));
                int h = Math.max(1, (int) Math.round(src.getHeight() * z));
                java.awt.image.BufferedImage scaled = new java.awt.image.BufferedImage(w, h, java.awt.image.BufferedImage.TYPE_INT_ARGB);
                java.awt.Graphics2D g = scaled.createGraphics();
                g.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION, java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g.drawImage(src, 0, 0, w, h, null);
                g.dispose();
                bigPreview.setIcon(new ImageIcon(scaled));
            }
        } catch (Exception ex) {
            bigPreview.setIcon(null);
        }
    }

    @Override public void dispose() {
        super.dispose();
        try { if (srcDoc != null) srcDoc.close(); } catch (Exception ignore) {}
        srcDoc = null; renderer = null;
    }
}
