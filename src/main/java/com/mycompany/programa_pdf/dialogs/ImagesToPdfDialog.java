package com.mycompany.programa_pdf.dialogs;

import com.mycompany.programa_pdf.MainWindow;
import com.mycompany.programa_pdf.io.FxFileDialogs;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.printing.PDFPageable;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.awt.print.PrinterJob;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.mycompany.programa_pdf.images.ImageUtils.rotate90s;
import static com.mycompany.programa_pdf.images.ImageUtils.scale;
import static com.mycompany.programa_pdf.io.FileTypeUtils.isImage;

/** Diálogo: Imágenes → PDF (ordenar, rotar, preview, guardar/imprimir). */
public class ImagesToPdfDialog extends JDialog {

    private static class ImageEntry {
        final File file;
        int rotation = 0;                // múltiplos de 90
        BufferedImage thumb, thumbRot;   // caché de miniaturas
        ImageEntry(File f) { file = f; }
        void rotate(int delta) { rotation = ((rotation + delta) % 360 + 360) % 360; thumbRot = null; }
        @Override public String toString() { return file.getName(); }
    }

    private final DefaultListModel<ImageEntry> model = new DefaultListModel<>();
    private final JList<ImageEntry> list = new JList<>(model);
    private final JLabel bigPreview = new JLabel("", SwingConstants.CENTER);
    private final JSlider zoom = new JSlider(10, 300, 100);

    private final JButton btnRemove= new JButton("Eliminar");
    private final JButton btnRotL  = new JButton("↺ 90°");
    private final JButton btnRotR  = new JButton("↻ 90°");
    private final JButton btnUp    = new JButton("↑");
    private final JButton btnDown  = new JButton("↓");
    private final JButton btnPrint = new JButton("Imprimir…");
    private final JButton btnSave  = new JButton("Guardar…");
    private final JButton btnClose = new JButton("Cerrar");

    private final MainWindow mw;
    private final File workingDir;

    public ImagesToPdfDialog(MainWindow mw, File initialDir, List<File> initialImages) {
        super(mw, "Imágenes → PDF", ModalityType.MODELESS);
        this.mw = mw;
        this.workingDir = (initialDir != null) ? initialDir : new File(System.getProperty("user.home"));

        buildUI();
        wireActions();

        if (initialImages != null && !initialImages.isEmpty()) addImages(initialImages);

        setSize(1120, 680);
        setLocationRelativeTo(mw);
    }

    private void buildUI() {
        JPanel root = new JPanel(new BorderLayout(8,8));
        root.setBorder(new EmptyBorder(8,8,8,8));
        setContentPane(root);

        // Toolbar superior (sin ↑/↓ aquí; van debajo de miniaturas)
        JToolBar tb = new JToolBar();
        tb.setFloatable(false);
        tb.add(btnRemove);
        tb.addSeparator();
        tb.add(btnRotL); tb.add(btnRotR);
        tb.addSeparator();
        tb.add(btnPrint); tb.add(btnSave);
        tb.add(Box.createHorizontalGlue()); tb.add(btnClose);
        root.add(tb, BorderLayout.NORTH);

        // Lista de miniaturas
        list.setLayoutOrientation(JList.HORIZONTAL_WRAP);
        list.setVisibleRowCount(-1);
        list.setFixedCellWidth(200);
        list.setFixedCellHeight(260);
        list.setCellRenderer(new ThumbRenderer());
        list.setBorder(new EmptyBorder(6,6,6,6));
        list.setDragEnabled(true);
        list.setDropMode(DropMode.INSERT);
        list.setTransferHandler(new ImagesDnDHandler(list, model));
        list.addListSelectionListener(e -> { refreshBigPreview(); updateButtons(); });

        JScrollPane leftScroll = new JScrollPane(list);
        leftScroll.setBorder(new EmptyBorder(4,4,4,4));

        // Barra de reordenar debajo (↑/↓)
        JPanel reorderBar = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 8));
        Dimension d = new Dimension(36, 36);
        btnUp.setPreferredSize(d);
        btnDown.setPreferredSize(d);
        reorderBar.add(btnUp);
        reorderBar.add(btnDown);

        JPanel leftCol = new JPanel(new BorderLayout());
        leftCol.add(leftScroll, BorderLayout.CENTER);
        leftCol.add(reorderBar, BorderLayout.SOUTH);

        // Derecha: preview + zoom
        JPanel right = new JPanel(new BorderLayout());
        right.add(new JScrollPane(bigPreview), BorderLayout.CENTER);
        JPanel southR = new JPanel(new BorderLayout());
        JPanel zoomPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        zoomPanel.add(new JLabel("Zoom:"));
        zoom.setPreferredSize(new Dimension(260, zoom.getPreferredSize().height));
        zoomPanel.add(zoom);
        southR.add(zoomPanel, BorderLayout.WEST);
        right.add(southR, BorderLayout.SOUTH);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftCol, right);
        split.setResizeWeight(0.36);
        root.add(split, BorderLayout.CENTER);

        JLabel hint = new JLabel("<html>Tips: arrastra para reordenar • Supr = eliminar • Ctrl+↑/↓ = mover • R/L = rotar</html>");
        hint.setForeground(UIManager.getColor("Label.disabledForeground"));
        hint.setBorder(new EmptyBorder(0, 2, 4, 2));
        root.add(hint, BorderLayout.SOUTH);
    }

    private void wireActions() {
        btnRemove.addActionListener(e -> removeSelected());
        btnRotL.addActionListener(e -> rotateSelected(-90));
        btnRotR.addActionListener(e -> rotateSelected(+90));
        btnUp.addActionListener(e -> moveSelected(-1));
        btnDown.addActionListener(e -> moveSelected(+1));
        btnSave.addActionListener(e -> onSave());
        btnPrint.addActionListener(e -> onPrint());
        btnClose.addActionListener(e -> dispose());
        zoom.addChangeListener(e -> refreshBigPreview());

        // Atajos
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

    // ==== Acciones ====
    private void addImages(List<File> files) {
        for (File f : files) if (f != null && f.isFile() && isImage(f)) model.addElement(new ImageEntry(f));
        if (list.getSelectedIndex() < 0 && model.size() > 0) list.setSelectedIndex(0);
        updateButtons();
    }

    private void removeSelected() {
        int[] sel = list.getSelectedIndices();
        if (sel == null || sel.length == 0) return;
        for (int i = sel.length - 1; i >= 0; i--) model.remove(sel[i]);
        if (!model.isEmpty()) list.setSelectedIndex(Math.min(model.size()-1, Math.max(0, sel[0]-1)));
        updateButtons();
    }

    private void rotateSelected(int delta) {
        int[] idx = list.getSelectedIndices();
        if (idx.length == 0) { Toolkit.getDefaultToolkit().beep(); return; }
        for (int i : idx) { ImageEntry it = model.get(i); it.rotate(delta); model.set(i, it); }
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

    /** Intercambia dos elementos del modelo (con límites seguros). */
    private void swap(int i, int j) {
        if (i < 0 || j < 0 || i >= model.size() || j >= model.size()) return;
        ImageEntry a = model.get(i);
        ImageEntry b = model.get(j);
        model.set(i, b);
        model.set(j, a);
    }

    private void onSave() {
        if (model.isEmpty()) return;
        File out = FxFileDialogs.pickSave(this, "Guardar PDF…", workingDir, "imagenes.pdf", "pdf");
        if (out == null) return;

        setUIEnabled(false);
        if (mw != null) {
            mw.runAsync(() -> {
                buildAndWrite(out);
                SwingUtilities.invokeLater(() -> setUIEnabled(true));
            });
        } else {
            buildAndWrite(out);
            setUIEnabled(true);
        }
    }

    private void onPrint() {
        if (model.isEmpty()) return;
        setUIEnabled(false);
        if (mw != null) {
            mw.runAsync(() -> {
                try (PDDocument doc = buildPdfInMemory()) {
                    PrinterJob job = PrinterJob.getPrinterJob();
                    job.setPageable(new PDFPageable(doc));
                    if (job.printDialog()) job.print();
                } catch (Exception ignore) {}
                SwingUtilities.invokeLater(() -> setUIEnabled(true));
            });
        } else {
            try (PDDocument doc = buildPdfInMemory()) {
                PrinterJob job = PrinterJob.getPrinterJob();
                job.setPageable(new PDFPageable(doc));
                if (job.printDialog()) job.print();
            } catch (Exception ignore) {}
            setUIEnabled(true);
        }
    }

    // ==== Build PDF ====
    private void buildAndWrite(File outFile) {
        try (PDDocument doc = buildPdfInMemory()) {
            doc.save(outFile);
            try { if (mw != null) mw.openIfWanted(outFile); } catch (Exception ignore) {}
        } catch (Exception ex) {
            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this,
                    "No se pudo generar el PDF:\n" + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE));
            ex.printStackTrace();
        }
    }

    private PDDocument buildPdfInMemory() throws Exception {
        PDDocument out = new PDDocument();
        for (int i = 0; i < model.size(); i++) {
            ImageEntry it = model.get(i);
            BufferedImage img = ImageIO.read(it.file);
            if (img == null) continue;
            if (it.rotation != 0) img = rotate90s(img, it.rotation);

            PDPage page = new PDPage(new PDRectangle(img.getWidth(), img.getHeight()));
            out.addPage(page);

            var pdImg = LosslessFactory.createFromImage(out, img);
            float marginX = (float)(img.getWidth() * 0.05);
            float marginY = (float)(img.getHeight() * 0.05);
            float w = img.getWidth()  - 2*marginX;
            float h = img.getHeight() - 2*marginY;
            try (PDPageContentStream cs = new PDPageContentStream(out, page)) {
                cs.drawImage(pdImg, marginX, marginY, w, h);
            }
        }
        return out;
    }

    // ==== Estado/UI ====
    private void updateButtons() {
        int[] sel = list.getSelectedIndices();
        boolean hasSel = sel != null && sel.length > 0;
        boolean canUp = hasSel && sel[0] > 0;
        boolean canDown = hasSel && sel[sel.length - 1] < model.size() - 1;

        btnRemove.setEnabled(hasSel);
        btnUp.setEnabled(canUp);
        btnDown.setEnabled(canDown);
        btnRotL.setEnabled(hasSel);
        btnRotR.setEnabled(hasSel);
        btnSave.setEnabled(model.size() > 0);
        btnPrint.setEnabled(model.size() > 0);
    }

    private void setUIEnabled(boolean en) {
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

    // ==== Preview / thumbs ====
    private BufferedImage thumbFor(ImageEntry it) {
        try {
            if (it.thumb == null) {
                BufferedImage img = ImageIO.read(it.file);
                it.thumb = scale(img, 180, 230);
                it.thumbRot = null;
            }
            if (it.rotation == 0) return it.thumb;
            if (it.thumbRot == null) it.thumbRot = scale(rotate90s(it.thumb, it.rotation), 180, 230);
            return it.thumbRot;
        } catch (Exception ex) {
            BufferedImage err = new BufferedImage(180, 230, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = err.createGraphics();
            g.setColor(new Color(200,80,80));
            g.fillRoundRect(0,0,180,230,16,16);
            g.setColor(Color.WHITE);
            g.drawString("Error", 70, 120);
            g.dispose();
            return err;
        }
    }

    private void refreshBigPreview() {
        int idx = list.getSelectedIndex();
        if (idx < 0) { bigPreview.setIcon(null); return; }
        ImageEntry it = model.get(idx);
        try {
            double z = zoom.getValue()/100.0;
            BufferedImage src = ImageIO.read(it.file);
            if (src == null) throw new Exception("No se pudo leer " + it.file);
            if (it.rotation != 0) src = rotate90s(src, it.rotation);
            int w = Math.max(1, (int)Math.round(src.getWidth()*z));
            int h = Math.max(1, (int)Math.round(src.getHeight()*z));
            BufferedImage scaled = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = scaled.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(src, 0, 0, w, h, null);
            g.dispose();
            bigPreview.setIcon(new ImageIcon(scaled));
        } catch (Exception ex) {
            bigPreview.setIcon(null);
        }
    }

    // ==== Renderers / DnD ====
    private class ThumbRenderer extends JPanel implements ListCellRenderer<ImageEntry> {
        private final JLabel pic = new JLabel("", SwingConstants.CENTER);
        private final JLabel txt = new JLabel("", SwingConstants.CENTER);
        ThumbRenderer() {
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
        @Override public Component getListCellRendererComponent(JList<? extends ImageEntry> list, ImageEntry value, int index, boolean isSelected, boolean cellHasFocus) {
            pic.setIcon(new ImageIcon(thumbFor(value)));
            txt.setText("Imagen" + (value.rotation != 0 ? (" (" + value.rotation + "°)") : ""));
            setBackground(isSelected ? list.getSelectionBackground() : list.getBackground());
            setForeground(isSelected ? list.getSelectionForeground() : list.getForeground());
            return this;
        }
    }

    /** DnD: archivos externos + reordenamiento interno con corrección de índices. */
    private static class ImagesDnDHandler extends TransferHandler {
        private final JList<ImageEntry> list;
        private final DefaultListModel<ImageEntry> model;
        private int[] dragIndices = null;

        ImagesDnDHandler(JList<ImageEntry> list, DefaultListModel<ImageEntry> model) { this.list = list; this.model = model; }

        @Override public int getSourceActions(JComponent c) { return MOVE; }

        @Override protected Transferable createTransferable(JComponent c) {
            dragIndices = list.getSelectedIndices();
            return new java.awt.datatransfer.StringSelection("reorder");
        }

        @Override public boolean canImport(TransferSupport s) {
            if (s.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) return true;
            s.setDropAction(MOVE);
            return s.isDrop() && s.isDataFlavorSupported(DataFlavor.stringFlavor);
        }

        @SuppressWarnings("unchecked")
        @Override public boolean importData(TransferSupport s) {
            try {
                if (s.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                    List<File> files = (List<File>) s.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
                    for (File f : files) if (f != null && f.isFile() && isImage(f)) model.addElement(new ImageEntry(f));
                    return true;
                }
                if (s.isDataFlavorSupported(DataFlavor.stringFlavor) && dragIndices != null) {
                    JList.DropLocation dl = (JList.DropLocation) s.getDropLocation();
                    int dropIndex = Math.max(0, dl.getIndex());

                    List<ImageEntry> dragged = new ArrayList<>();
                    for (int idx : dragIndices) dragged.add(model.get(idx));

                    int removedBefore = (int) Arrays.stream(dragIndices).filter(i -> i < dropIndex).count();
                    for (int i = dragIndices.length - 1; i >= 0; i--) model.remove(dragIndices[i]);

                    int insertAt = Math.min(dropIndex - removedBefore, model.size());
                    for (ImageEntry t : dragged) model.add(insertAt++, t);

                    int base = dropIndex - removedBefore;
                    int[] newSel = new int[dragged.size()];
                    for (int i = 0; i < dragged.size(); i++) newSel[i] = base + i;
                    list.setSelectedIndices(newSel);
                    list.ensureIndexIsVisible(newSel[0]);
                    return true;
                }
                return false;
            } catch (Exception ignore) {
                return false;
            } finally {
                dragIndices = null;
            }
        }
    }
}
