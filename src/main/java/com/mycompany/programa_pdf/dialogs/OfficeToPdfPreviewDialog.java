package com.mycompany.programa_pdf.dialogs;

import com.mycompany.programa_pdf.MainWindow;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.File;
import java.nio.file.Files;

/**
 * Convierte un archivo de Office a PDF en un archivo temporal y abre PdfPreviewDialog.
 * Muestra progreso mientras convierte y permite cancelar.
 */
public class OfficeToPdfPreviewDialog extends JDialog {

    private final MainWindow mw;
    private final File officeFile;

    private final JLabel lblMsg = new JLabel("Convirtiendo a PDF…", SwingConstants.LEFT);
    private final JProgressBar bar = new JProgressBar();
    private final JButton btnCancelar = new JButton("Cancelar");

    private volatile File tempPdf;       // PDF temporal resultante
    private SwingWorker<File, Void> task;

    public OfficeToPdfPreviewDialog(MainWindow mw, File officeFile) {
        super(mw, "Vista previa (Office → PDF)", true);
        this.mw = mw;
        this.officeFile = officeFile;

        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        buildUI();

        pack();
        setSize(460, 140);
        setLocationRelativeTo(mw);

        startConversion();
    }

    private void buildUI() {
        JPanel root = new JPanel(new BorderLayout(10,10));
        root.setBorder(new EmptyBorder(12,12,12,12));
        setContentPane(root);

        JPanel center = new JPanel(new BorderLayout(8,8));
        center.add(lblMsg, BorderLayout.NORTH);
        bar.setIndeterminate(true);
        center.add(bar, BorderLayout.CENTER);
        root.add(center, BorderLayout.CENTER);

        JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btnCancelar.addActionListener(e -> onCancel());
        south.add(btnCancelar);
        root.add(south, BorderLayout.SOUTH);
    }

    private void startConversion() {
        // Archivo temporal
        try {
            tempPdf = Files.createTempFile("office_preview_", ".pdf").toFile();
            tempPdf.deleteOnExit();
        } catch (Exception ex) {
            mw.fail(ex);
            dispose();
            return;
        }

        // Lanza conversión en background usando tu servicio
        task = new SwingWorker<>() {
            @Override protected File doInBackground() throws Exception {
                File loHome = mw.getLibreOfficeHome(); // puede ser null si auto-detectas dentro del servicio
                mw.getSvc().officeToPdf(officeFile, tempPdf, loHome);
                return tempPdf;
            }

            @Override protected void done() {
                try {
                    if (isCancelled()) {
                        cleanupTemp();
                        dispose();
                        return;
                    }
                    File pdf = get();
                    if (pdf == null || !pdf.isFile()) throw new RuntimeException("No se generó PDF temporal.");
                    // Cerrar este diálogo y abrir la vista previa del PDF
                    setVisible(false);
                    openPreview(pdf);
                } catch (Exception ex) {
                    cleanupTemp();
                    mw.fail(ex);
                } finally {
                    // ya no necesitamos tener visible este diálogo
                    dispose();
                }
            }
        };
        task.execute();
    }

    private void onCancel() {
        try {
            if (task != null && !task.isDone()) task.cancel(true);
        } catch (Exception ignore) {}
        cleanupTemp();
        dispose();
    }

    private void cleanupTemp() {
        try {
            if (tempPdf != null && tempPdf.isFile()) tempPdf.delete();
        } catch (Exception ignore) {}
    }

    private void openPreview(File pdfFile) {
    PDDocument d = null;
    try {
        d = Loader.loadPDF(pdfFile);
        // antes: new PdfPreviewDialog(mw, d, pdfFile.getName());
        // ahora pasamos officeFile como sourceFile:
        PdfPreviewDialog dlg = new PdfPreviewDialog(mw, d, pdfFile.getName(), officeFile);
        dlg.setLocationRelativeTo(mw);
        dlg.setVisible(true);
    } catch (Exception ex) {
        if (d != null) try { d.close(); } catch (Exception ignore) {}
        mw.fail(ex);
    } finally {
        cleanupTemp();
    }
}
}
