package com.mycompany.programa_pdf.dialogs;

import com.mycompany.programa_pdf.MainWindow;
import com.mycompany.programa_pdf.io.FxFileDialogs;
import com.mycompany.programa_pdf.pdf.PrintUtils;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;

public class SplitRangeDialog extends JDialog {

    private final MainWindow mw;
    private final File pdfFile;

    // PDF
    private PDDocument doc;
    private PDFRenderer renderer;
    private int pageCount;

    // UI
    private final JSpinner spFrom = new JSpinner(new SpinnerNumberModel(1, 1, 1, 1));
    private final JSpinner spTo   = new JSpinner(new SpinnerNumberModel(1, 1, 1, 1));
    private final JLabel pageImg  = new JLabel("", SwingConstants.CENTER);
    private final JList<Integer> pageList = new JList<>();
    private final DefaultListModel<Integer> pageModel = new DefaultListModel<>();
    private final JButton btnSave = new JButton("Guardar…");

    public SplitRangeDialog(MainWindow mw, File pdf) throws Exception {
        super(mw, "Dividir PDF (vista previa)", true);
        this.mw = mw;
        this.pdfFile = pdf;

        setMinimumSize(new Dimension(980, 680));
        setLocationRelativeTo(mw);

        // Cargar PDF
        doc = Loader.loadPDF(pdfFile);
        renderer = new PDFRenderer(doc);
        pageCount = doc.getNumberOfPages();

        ((SpinnerNumberModel) spFrom.getModel()).setMaximum(pageCount);
        ((SpinnerNumberModel) spTo.getModel()).setMaximum(pageCount);
        spTo.setValue(pageCount);

        buildUI();
        wireEvents();

        fillPageList();
        pageList.setSelectedIndex(0);
        renderPreview(0);

        addWindowListener(new WindowAdapter() {
            @Override public void windowClosed(WindowEvent e) { closeDoc(); }
            @Override public void windowClosing(WindowEvent e) { closeDoc(); }
        });
    }

    private void buildUI() {
        JPanel root = new JPanel(new BorderLayout(10,10));
        root.setBorder(new EmptyBorder(10,10,10,10));
        setContentPane(root);

        // Top
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 6));
        top.add(new JLabel("Archivo: " + pdfFile.getName() + " (" + pageCount + " pág.)"));
        top.add(Box.createHorizontalStrut(20));
        top.add(new JLabel("Desde:"));
        spFrom.setPreferredSize(new Dimension(70, spFrom.getPreferredSize().height));
        top.add(spFrom);
        top.add(new JLabel("Hasta:"));
        spTo.setPreferredSize(new Dimension(70, spTo.getPreferredSize().height));
        top.add(spTo);
        root.add(top, BorderLayout.NORTH);

        // Centro
        pageList.setModel(pageModel);
        pageList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        pageList.setFixedCellHeight(28);
        pageList.setVisibleRowCount(-1);
        pageList.setBorder(new EmptyBorder(4, 4, 4, 4));
        JScrollPane left = new JScrollPane(pageList);
        left.setPreferredSize(new Dimension(130, 200));

        pageImg.setOpaque(true);
        pageImg.setBackground(new Color(245,245,245));
        JScrollPane right = new JScrollPane(pageImg);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, right);
        split.setResizeWeight(0.15);
        root.add(split, BorderLayout.CENTER);

        // Bottom
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 8));
        JButton btnPrint = new JButton("Imprimir…");
        JButton btnClose = new JButton("Cerrar");

        btnSave.addActionListener(e -> onSave());
        btnPrint.addActionListener(e -> onPrint());
        btnClose.addActionListener(e -> dispose());

        bottom.add(btnSave);
        bottom.add(btnPrint);
        bottom.add(btnClose);
        root.add(bottom, BorderLayout.SOUTH);

        validateRange();
    }

    private void wireEvents() {
        spFrom.addChangeListener(e -> { clampFromTo(); validateRange(); });
        spTo.addChangeListener(e -> { clampFromTo(); validateRange(); });
        pageList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int idx = pageList.getSelectedIndex();
                if (idx >= 0) renderPreview(idx);
            }
        });
    }

    private void onPrint() {
        try {
            int from = (Integer) spFrom.getValue();
            int to   = (Integer) spTo.getValue();
            if (from < 1 || to < from || to > pageCount) {
                JOptionPane.showMessageDialog(this, "Rango inválido.", "Imprimir", JOptionPane.WARNING_MESSAGE);
                return;
            }
            PrintUtils.printPdf(doc, from, to);
        } catch (Exception ex) {
            mw.fail(ex);
        }
    }

    private void fillPageList() {
        pageModel.clear();
        for (int i = 1; i <= pageCount; i++) pageModel.addElement(i);
    }

    private void clampFromTo() {
        int from = (Integer) spFrom.getValue();
        int to   = (Integer) spTo.getValue();
        if (from < 1) from = 1;
        if (to < 1) to = 1;
        if (from > pageCount) from = pageCount;
        if (to > pageCount) to = pageCount;
        if (from > to) {
            if (spFrom.hasFocus()) to = from;
            else from = to;
        }
        spFrom.setValue(from);
        spTo.setValue(to);
    }

    private void validateRange() {
        int from = (Integer) spFrom.getValue();
        int to   = (Integer) spTo.getValue();
        btnSave.setEnabled(from >= 1 && to >= from && to <= pageCount);
    }

    private void renderPreview(int zeroBasedPage) {
        new SwingWorker<Image,Void>() {
            @Override protected Image doInBackground() throws Exception {
                BufferedImage bi = renderer.renderImage(zeroBasedPage, 1.25f);
                return bi;
            }
            @Override protected void done() {
                try { pageImg.setIcon(new ImageIcon(get())); }
                catch (Exception ignore) { pageImg.setIcon(null); }
            }
        }.execute();
    }

    private void onSave() {
        int from = (Integer) spFrom.getValue();
        int to   = (Integer) spTo.getValue();
        if (from < 1 || to < from || to > pageCount) {
            JOptionPane.showMessageDialog(this, "Rango inválido.", "Dividir PDF", JOptionPane.WARNING_MESSAGE);
            return;
        }

        File out = FxFileDialogs.pickSave(
                this,
                "Guardar PDF resultante…",
                pdfFile.getParentFile(),
                "resultado.pdf",
                "pdf"
        );
        if (out == null) return;

        final int fFrom = from, fTo = to;
        final File outPdf = out;

        mw.runAsync(() -> {
            try {
                File res = mw.getSvc().splitRange(pdfFile, outPdf, fFrom, fTo);
                mw.ok("PDF generado: " + res.getAbsolutePath());
                mw.getHistory().add(
                        "Dividir PDF (rango " + fFrom + "-" + fTo + ")",
                        java.util.List.of(pdfFile.getAbsolutePath()),
                        res.getAbsolutePath()
                );
                PdfPreviewDialog.showFile(mw, res);
                mw.openIfWanted(res);
            } catch (Exception ex) {
                mw.fail(ex);
            }
        });

        dispose();
    }

    private void closeDoc() {
        try { if (doc != null) doc.close(); } catch (Exception ignore) {}
    }
}
