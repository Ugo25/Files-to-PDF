package com.mycompany.programa_pdf.dialogs;

import com.mycompany.programa_pdf.MainWindow;
import com.mycompany.programa_pdf.io.FxFileDialogs;
import com.mycompany.programa_pdf.pdf.PrintUtils;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;

public class ExtractTextDialog extends JDialog {

    // ============================== Estado =================================
    private final MainWindow mw;
    private final File pdfFile;

    private PDDocument doc;
    private PDFRenderer renderer;

    // ================================ UI ===================================
    private final JRadioButton rbAll  = new JRadioButton("Todo el PDF", true);
    private final JRadioButton rbPage = new JRadioButton("Solo página:");
    private final JSpinner spPage     = new JSpinner(new SpinnerNumberModel(1, 1, 1, 1));

    private final JLabel pageImageLbl = new JLabel("", SwingConstants.CENTER);
    private final JTextArea textArea  = new JTextArea();

    private JButton btnCopy, btnSave, btnPrint, btnClose;
    private JSplitPane split;

    // ============================ Constructor ===============================
    public ExtractTextDialog(MainWindow mw, File pdf) throws Exception {
        super(mw, "Extraer texto", true);
        this.mw = mw;
        this.pdfFile = pdf;

        if (pdf == null || !pdf.isFile()) {
            throw new IllegalArgumentException("Archivo PDF inválido.");
        }

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setMinimumSize(new Dimension(980, 680));
        setLocationRelativeTo(mw);

        // Carga PDF
        doc = Loader.loadPDF(pdfFile);
        renderer = new PDFRenderer(doc);

        int pages = doc.getNumberOfPages();
        ((SpinnerNumberModel) spPage.getModel()).setMaximum(pages);

        buildUI(pages);
        wireEvents();
        refreshPreview();

        // Cierre seguro
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosed (WindowEvent e) { closeDoc(); }
            @Override public void windowClosing(WindowEvent e) { closeDoc(); }
        });
    }

    // ============================== UI Build ================================
    private void buildUI(int pages) {
        JPanel root = new JPanel(new BorderLayout(10, 10));
        root.setBorder(new EmptyBorder(10, 10, 10, 10));
        setContentPane(root);

        // Header
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 6));
        top.add(new JLabel("Archivo: " + pdfFile.getName() + "  (" + pages + " pág.)"));

        ButtonGroup g = new ButtonGroup(); g.add(rbAll); g.add(rbPage);
        top.add(Box.createHorizontalStrut(18));
        top.add(rbAll);
        top.add(rbPage);
        top.add(spPage);
        spPage.setEnabled(false);
        root.add(top, BorderLayout.NORTH);

        // Centro (previa + texto)
        pageImageLbl.setOpaque(true);
        pageImageLbl.setBackground(new Color(245, 245, 245));
        JScrollPane imgScroll = new JScrollPane(pageImageLbl);

        textArea.setEditable(false);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        JScrollPane txtScroll = new JScrollPane(textArea);

        split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, imgScroll, txtScroll);
        split.setResizeWeight(0.55);
        root.add(split, BorderLayout.CENTER);

        // Footer (acciones)
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 8));
        btnCopy  = new JButton("Copiar");
        btnSave  = new JButton("Guardar…");
        btnPrint = new JButton("Imprimir…");
        btnClose = new JButton("Cerrar");

        btnCopy.addActionListener(e -> copyToClipboard());
        btnSave.addActionListener(e -> saveToTxt());
        btnPrint.addActionListener(e -> printSelection());
        btnClose.addActionListener(e -> dispose());

        bottom.add(btnCopy);
        bottom.add(btnSave);
        bottom.add(btnPrint);
        bottom.add(btnClose);
        root.add(bottom, BorderLayout.SOUTH);

        // Atajos
        getRootPane().setDefaultButton(btnSave);
        // Esc = cerrar
        getRootPane().registerKeyboardAction(
                e -> dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW
        );
        // Ctrl+C / Ctrl+S / Ctrl+P
        getRootPane().registerKeyboardAction(
                e -> copyToClipboard(),
                KeyStroke.getKeyStroke(KeyEvent.VK_C, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()),
                JComponent.WHEN_IN_FOCUSED_WINDOW
        );
        getRootPane().registerKeyboardAction(
                e -> saveToTxt(),
                KeyStroke.getKeyStroke(KeyEvent.VK_S, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()),
                JComponent.WHEN_IN_FOCUSED_WINDOW
        );
        getRootPane().registerKeyboardAction(
                e -> printSelection(),
                KeyStroke.getKeyStroke(KeyEvent.VK_P, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()),
                JComponent.WHEN_IN_FOCUSED_WINDOW
        );

        // Ajuste inicial del divisor y foco
        SwingUtilities.invokeLater(() -> {
            split.setDividerLocation(0.55);
            textArea.requestFocusInWindow();
        });

        updateButtonsEnabled();
    }

    private void wireEvents() {
        rbAll.addActionListener(e -> { spPage.setEnabled(false); refreshPreview(); });
        rbPage.addActionListener(e -> { spPage.setEnabled(true);  refreshPreview(); });
        spPage.addChangeListener(e -> { if (rbPage.isSelected()) refreshPreview(); });
    }

    // ============================ Lógica/Acciones ===========================
    private void refreshPreview() {
        new SwingWorker<Void, Void>() {
            String text = "";
            Image img = null;
            int pageIdx = currentPageIndex();

            @Override protected Void doInBackground() {
                try {
                    PDFTextStripper stripper = new PDFTextStripper();
                    if (rbAll.isSelected()) {
                        stripper.setStartPage(1);
                        stripper.setEndPage(doc.getNumberOfPages());
                    } else {
                        int p = pageIdx + 1; // 1-based
                        stripper.setStartPage(p);
                        stripper.setEndPage(p);
                    }
                    text = stripper.getText(doc);

                    int renderIndex = rbAll.isSelected() ? 0 : pageIdx;
                    BufferedImage bi = renderer.renderImage(renderIndex, 1.3f);
                    img = bi;
                } catch (Exception ex) {
                    text = "[Error al extraer/renderizar: " + ex.getMessage() + "]";
                }
                return null;
            }

            @Override protected void done() {
                textArea.setText(text);
                pageImageLbl.setIcon(img != null ? new ImageIcon(img) : null);
                updateButtonsEnabled();
                pageImageLbl.revalidate();
                pageImageLbl.repaint();
            }
        }.execute();
    }

    private void copyToClipboard() {
        String t = textArea.getText();
        Toolkit.getDefaultToolkit().getSystemClipboard()
                .setContents(new StringSelection(t == null ? "" : t), null);
        mw.ok("Texto copiado al portapapeles.");
    }

    private void printSelection() {
        try {
            if (rbAll.isSelected()) {
                PrintUtils.printPdf(doc);
            } else {
                int p = currentPageIndex() + 1; // 1-based
                PrintUtils.printPdf(doc, p, p);
            }
        } catch (Exception ex) {
            mw.fail(ex);
        }
    }

    private void saveToTxt() {
        try {
            String suffix = rbAll.isSelected()
                    ? "_texto"
                    : "_p" + (currentPageIndex() + 1) + "_texto";

            // nombre sugerido y carpeta separados (bugfix)
            File suggested = mw.suggestWithSuffix(pdfFile, suffix, "txt");
            File baseDir   = suggested.getParentFile();
            String suggestedName = suggested.getName();

            File out = FxFileDialogs.pickSave(
                    this,
                    "Guardar texto como…",
                    baseDir,
                    suggestedName,
                    "txt"
            );
            if (out == null) return;

            String t = textArea.getText();
            try (Writer w = new OutputStreamWriter(new java.io.FileOutputStream(out), StandardCharsets.UTF_8)) {
                w.write(t != null ? t : "");
            }

            mw.ok("Texto guardado en: " + out.getAbsolutePath());
            String desc = rbAll.isSelected()
                    ? "Extraer texto (todo)"
                    : "Extraer texto (pág. " + (currentPageIndex() + 1) + ")";
            mw.getHistory().add(desc,
                    java.util.List.of(pdfFile.getAbsolutePath()),
                    out.getAbsolutePath());
            mw.openIfWanted(out);
        } catch (Exception ex) {
            mw.fail(ex);
        }
    }

    private int currentPageIndex() {
        int v = (Integer) spPage.getValue();
        return Math.max(1, v) - 1;
    }

    private void updateButtonsEnabled() {
        boolean hasText = textArea.getText() != null && !textArea.getText().isEmpty();
        btnCopy.setEnabled(hasText);
        btnSave.setEnabled(true);  // se puede guardar vacío si quieres, pero puedes cambiarlo a hasText
        btnPrint.setEnabled(true);
    }

    private void closeDoc() {
        try { if (doc != null) doc.close(); } catch (Exception ignore) {}
    }
}
