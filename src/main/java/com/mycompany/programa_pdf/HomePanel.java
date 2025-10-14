package com.mycompany.programa_pdf;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.mycompany.programa_pdf.io.FxFileDialogs;
import com.mycompany.programa_pdf.ui.RoundedDropPanel;
import com.mycompany.programa_pdf.ui.FileDropHandler;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static com.mycompany.programa_pdf.io.FileTypeUtils.*; // isImage / isPdf / isOffice

public class HomePanel extends JPanel {

    // ===== Ajustes rápidos =====
    private static final Dimension DROP_SIZE = new Dimension(900, 380);
    private static final int HEADER_TOP_MARGIN = 56;
    private static final int HEADER_BOTTOM_GAP = 0;

    private final MainWindow mw;

    private final List<File> selectedFiles = new ArrayList<>();

    private JLabel selIconLbl;
    private JLabel selNameLbl;
    private JLabel selPathLbl;
    private JButton btnContinuar;
    private RoundedDropPanel drop;

    public HomePanel(MainWindow mw) {
        super(new BorderLayout());
        this.mw = mw;
        setBorder(new EmptyBorder(64, 28, 28, 28));
        setBackground(UIManager.getColor("Panel.background"));

        add(buildHeader(), BorderLayout.NORTH);
        add(buildCenter(), BorderLayout.CENTER);
        add(buildFooter(), BorderLayout.SOUTH);
    }

    private JComponent buildHeader() {
        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setOpaque(false);

        JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        titlePanel.setOpaque(false);

        JLabel logoLbl = new JLabel();
        try {
            logoLbl.setIcon(new FlatSVGIcon("icons/icon.svg", 50, 50));
        } catch (Throwable t) {
            java.net.URL png = HomePanel.class.getResource("/icons/icon.png");
            if (png != null) logoLbl.setIcon(new ImageIcon(png));
        }

        JLabel title = new JLabel("Files to PDF");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 26f));

        titlePanel.add(logoLbl);
        titlePanel.add(title);

        JLabel subtitle = new JLabel("Selecciona el archivo que quieres convertir");
        subtitle.setFont(subtitle.getFont().deriveFont(Font.PLAIN, 14f));
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        header.add(titlePanel);
        header.add(Box.createVerticalStrut(6));
        header.add(subtitle);
        header.add(Box.createVerticalStrut(HEADER_BOTTOM_GAP));

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.add(Box.createVerticalStrut(HEADER_TOP_MARGIN), BorderLayout.NORTH);
        wrapper.add(header, BorderLayout.CENTER);
        return wrapper;
    }

    private JComponent buildCenter() {
        JPanel center = new JPanel(new GridBagLayout());
        center.setOpaque(false);

        drop = new RoundedDropPanel();
        drop.setPreferredSize(DROP_SIZE);
        drop.setMinimumSize(DROP_SIZE);
        drop.setMaximumSize(new Dimension(Integer.MAX_VALUE, DROP_SIZE.height));
        drop.setLayout(new BorderLayout(10, 10));

        JLabel big = new JLabel("Arrastra y suelta aquí tu archivo", SwingConstants.CENTER);
        big.setFont(big.getFont().deriveFont(Font.BOLD, 18f));

        JButton btnPick = new JButton("Seleccionar archivo…");
        btnPick.addActionListener(e -> {
            File baseDir = (!selectedFiles.isEmpty())
                    ? selectedFiles.get(0).getParentFile()
                    : (mw.getSelectedFile() != null ? mw.getSelectedFile().getParentFile() : mw.getLastDir());

            List<File> chosen = FxFileDialogs.pickOpenMulti(
                    this,
                    "Selecciona archivo(s) (PDF/imagen/Office)",
                    baseDir,
                    "pdf", "png", "jpg", "jpeg", "webp", "bmp", "gif",
                    "doc", "docx", "xls", "xlsx", "ppt", "pptx", "odt", "ods", "odp"
            );
            if (chosen == null || chosen.isEmpty()) return;

            setSelected(chosen);
            mw.setLastDir(chosen.get(0).getParentFile());
        });

        drop.add(big, BorderLayout.CENTER);
        JPanel bottomBar = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 8));
        bottomBar.setOpaque(false);
        bottomBar.add(btnPick);
        drop.add(bottomBar, BorderLayout.SOUTH);

        // DnD reutilizable
        FileDropHandler th = new FileDropHandler(drop, (accepted, unsupported) -> {
            if (unsupported != null && !unsupported.isEmpty()) {
                String msg = "Formato no soportado para convertir: " + String.join(", ", unsupported);
                JOptionPane.showMessageDialog(HomePanel.this, msg, "Aviso", JOptionPane.INFORMATION_MESSAGE);
            }
            if (accepted != null && !accepted.isEmpty()) setSelected(accepted);
        });
        propagateTransferHandler(drop, th);

        JPanel wrapper = new JPanel();
        wrapper.setOpaque(false);
        wrapper.setLayout(new BoxLayout(wrapper, BoxLayout.Y_AXIS));
        wrapper.add(drop);

        btnContinuar = new JButton("Continuar");
        btnContinuar.setEnabled(false);
        btnContinuar.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnContinuar.addActionListener(e -> mw.goCards());

        wrapper.add(Box.createVerticalStrut(14));
        wrapper.add(btnContinuar);

        center.add(wrapper, new GridBagConstraints());
        return center;
    }

    private JComponent buildFooter() {
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        footer.setOpaque(false);

        selIconLbl = new JLabel();
        selNameLbl = new JLabel("Ningún archivo seleccionado");
        selNameLbl.setFont(selNameLbl.getFont().deriveFont(Font.BOLD, 13f));
        selPathLbl = new JLabel("");
        selPathLbl.setForeground(new Color(120, 120, 120));

        JPanel textBox = new JPanel();
        textBox.setOpaque(false);
        textBox.setLayout(new BoxLayout(textBox, BoxLayout.Y_AXIS));
        textBox.add(selNameLbl);
        textBox.add(selPathLbl);

        footer.add(selIconLbl);
        footer.add(textBox);
        return footer;
    }

    // ======== Gestión de selección ========
    public void updateSelected(File f) {
        selectedFiles.clear();
        if (f != null) selectedFiles.add(f);
        updateFooterAndButton();
    }

    private void setSelected(List<File> files) {
        selectedFiles.clear();
        for (File f : files) if (f != null && f.exists()) selectedFiles.add(f);
        mw.setSelectedFiles(new ArrayList<>(selectedFiles));
        updateFooterAndButton();
    }

    public void externalSetSelectedList(List<File> files) {
        selectedFiles.clear();
        if (files != null) {
            for (File f : files) {
                if (f != null && f.exists() && (isImage(f) || isPdf(f) || isOffice(f))) {
                    selectedFiles.add(f);
                }
            }
        }
        updateFooterAndButton();
    }

    private void updateFooterAndButton() {
        if (selectedFiles.isEmpty()) {
            selIconLbl.setIcon(null);
            selNameLbl.setText("Ningún archivo seleccionado");
            selPathLbl.setText("");
            btnContinuar.setEnabled(false);
            return;
        }
        File first = selectedFiles.get(0);
        Icon sys = javax.swing.filechooser.FileSystemView.getFileSystemView().getSystemIcon(first);
        selIconLbl.setIcon(sys);

        long imgCount = selectedFiles.stream().filter(f -> isImage(f)).count();

        if (imgCount >= 2) {
            selNameLbl.setText("Varias imágenes");
            selPathLbl.setText(commonParentOf(selectedFiles));
        } else if (selectedFiles.size() == 1) {
            selNameLbl.setText(first.getName());
            selPathLbl.setText(first.getParent() == null ? "" : first.getParent());
        } else {
            selNameLbl.setText(selectedFiles.size() + " archivos seleccionados");
            selPathLbl.setText(commonParentOf(selectedFiles));
        }
        btnContinuar.setEnabled(true);
    }

    // ======== Helpers ========
    private String commonParentOf(List<File> files) {
        if (files == null || files.isEmpty()) return "";
        File parent = files.get(0).getParentFile();
        if (parent == null) return "";
        for (File f : files) {
            File p = (f == null ? null : f.getParentFile());
            if (p == null || !p.equals(parent)) return parent.getAbsolutePath() + " + …";
        }
        return parent.getAbsolutePath();
    }

    private void propagateTransferHandler(JComponent root, TransferHandler h) {
        root.setTransferHandler(h);
        if (root instanceof Container c) {
            for (Component child : c.getComponents()) {
                if (child instanceof JComponent jc) propagateTransferHandler(jc, h);
            }
        }
    }
}
