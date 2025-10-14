package com.mycompany.programa_pdf.dialogs;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.Locale;

public class PdfToImagesOptionsDialog extends JDialog {

    // ================================ UI ===================================
    private final JComboBox<String> cbFormat = new JComboBox<>(new String[]{"PNG", "JPG"});
    private final JSpinner spDpi = new JSpinner(new SpinnerNumberModel(200, 72, 600, 10));
    private boolean ok = false;

    // ============================== Constructor =============================
    public PdfToImagesOptionsDialog(Window owner) {
        super(owner, "PDF → Imágenes (ZIP)", ModalityType.APPLICATION_MODAL);
        setLayout(new BorderLayout(10, 10));
        ((JComponent) getContentPane()).setBorder(new EmptyBorder(10, 10, 10, 10));

        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(6, 6, 6, 6);
        gc.anchor = GridBagConstraints.WEST;

        gc.gridx = 0; gc.gridy = 0;
        form.add(new JLabel("Formato:"), gc);
        gc.gridx = 1;
        form.add(cbFormat, gc);

        gc.gridx = 0; gc.gridy = 1;
        form.add(new JLabel("DPI:"), gc);
        gc.gridx = 1;
        form.add(spDpi, gc);

        add(form, BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 8));
        JButton okBtn = new JButton("Exportar");
        JButton cancelBtn = new JButton("Cancelar");
        buttons.add(okBtn);
        buttons.add(cancelBtn);
        add(buttons, BorderLayout.SOUTH);

        okBtn.addActionListener(e -> { ok = true; dispose(); });
        cancelBtn.addActionListener(e -> dispose());

        pack();
        setResizable(false);
        setLocationRelativeTo(owner);
    }

    // ================================= API =================================
    public boolean isOk() { return ok; }

    public String getFormat() {
        String s = String.valueOf(cbFormat.getSelectedItem()).toLowerCase(Locale.ROOT);
        return s.startsWith("png") ? "png" : "jpg";
    }

    public float getDpi() {
        return ((Number) spDpi.getValue()).floatValue();
    }
}
