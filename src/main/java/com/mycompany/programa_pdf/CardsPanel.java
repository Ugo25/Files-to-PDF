    package com.mycompany.programa_pdf;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.mycompany.programa_pdf.dialogs.*;
import com.mycompany.programa_pdf.io.FxFileDialogs;
import com.mycompany.programa_pdf.ui.RoundedCard;
import com.mycompany.programa_pdf.ui.WrapLayout;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import static com.mycompany.programa_pdf.io.FileTypeUtils.*;

public class CardsPanel extends JPanel {

    // ============================== Constantes ==============================
    private static final String ICON_SPLIT     = "/icons/G_Dividir_PDF_rango.svg";
    private static final String ICON_EXTRACT   = "/icons/H_Extraer_texto.svg";
    private static final String ICON_WATERMARK = "/icons/I_Marca_de_agua_texto.svg";
    private static final String ICON_ADDIMG    = "/icons/Z_Agregar_imagen.svg";
    private static final String ICON_ROTATE    = "/icons/rotate.svg";
    private static final String ICON_MERGE     = "/icons/E_Unir_PDFs.svg";
    private static final String ICON_PDF2IMG   = "/icons/D_PDF_a_Imagenes_ZIP.svg";
    private static final String ICON_OFFICE    = "/icons/C_Office_mas_PDF.svg";

    private static final int ICON_SIZE = 128;
    private static final Dimension CARD_SIZE = new Dimension(420, 300);

    // ================================ Campos ================================
    private final MainWindow mw;
    private final JPanel grid = new JPanel(new WrapLayout(FlowLayout.CENTER, 18, 18));
    private final JLabel fileLbl = new JLabel("", SwingConstants.LEFT);
    private final JLabel pathLbl = new JLabel("", SwingConstants.LEFT);
    private final JButton btnChange = new JButton("Cambiar archivo…");
    private JScrollPane scroll;
    private File current = null;

    // ============================== Constructor =============================
    public CardsPanel(MainWindow mw) {
        super(new BorderLayout(0, 10));
        this.mw = mw;

        setOpaque(false);
        setBorder(new EmptyBorder(10, 16, 0, 16));

        fileLbl.setFont(fileLbl.getFont().deriveFont(Font.BOLD, 12f));
        Color pathColor = UIManager.getColor("Label.infoForeground");
        if (pathColor == null) pathColor = deriveSecondaryForeground();
        pathLbl.setForeground(pathColor);
        pathLbl.setFont(pathLbl.getFont().deriveFont(Font.PLAIN, 11f));

        JPanel headerLeft = new JPanel();
        headerLeft.setOpaque(false);
        headerLeft.setLayout(new BoxLayout(headerLeft, BoxLayout.Y_AXIS));
        headerLeft.add(fileLbl);
        headerLeft.add(pathLbl);

        btnChange.addActionListener(e -> mw.goHome());

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.add(headerLeft, BorderLayout.WEST);
        header.add(btnChange, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        grid.setOpaque(false);
        JPanel outer = new JPanel(new BorderLayout());
        outer.setOpaque(false);
        outer.add(grid, BorderLayout.NORTH);

        scroll = new JScrollPane(
                outer,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
        );
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        add(scroll, BorderLayout.CENTER);
    }

    // ================================ API ==================================
    public void refreshFor(File single) {
        current = single;
        rebuildGrid();
    }

    public void setGridEnabled(boolean enabled) {
        for (Component c : grid.getComponents()) c.setEnabled(enabled);
        btnChange.setEnabled(enabled);
    }

    // ============================ Construcción UI ===========================
    private void rebuildGrid() {
        grid.removeAll();

        if (current == null) {
            fileLbl.setText("Ningún PDF seleccionado");
            pathLbl.setText("");
            revalidate(); repaint();
            scrollToTop();
            return;
        }

        List<File> sel = null;
        try { sel = mw.getSelectedFiles(); } catch (Throwable ignore) {}

        int pdfCount = 0, imgCount = 0, offCount = 0;
        if (sel != null && !sel.isEmpty()) {
            for (File f : sel) {
                if (isPdf(f))    pdfCount++;
                else if (isImage(f))  imgCount++;
                else if (isOffice(f)) offCount++;
            }
        }

        if (pdfCount >= 2 && imgCount == 0 && offCount == 0) {
            fileLbl.setText("Varios PDFs");
            pathLbl.setText(commonParent(sel));

            final List<File> selCopy = (sel == null) ? new ArrayList<>() : new ArrayList<>(sel);

            grid.add(makeCard(
                    "Unir PDFs (seleccionados)",
                    "Combinar los " + pdfCount + " archivos en un único PDF (puedes reordenar y quitar antes de guardar).",
                    ICON_MERGE,
                    e -> actionMergeSelected(selCopy)
            ));

            revalidate(); repaint(); scrollToTop();
            return;
        }

        fileLbl.setText("Archivo: " + current.getName());
        File p = current.getParentFile();
        pathLbl.setText((p != null) ? p.getAbsolutePath() : "");

        if (isOffice(current)) {
            grid.add(makeCard("Office → PDF", "Convierte el documento de Office a PDF.", ICON_OFFICE, this::actionOfficeToPdf));
            revalidate(); repaint(); scrollToTop();
            return;
        }

        if (isImage(current)) {
            try {
                List<File> s2 = mw.getSelectedFiles();
                if (s2 != null) {
                    long imgs = s2.stream().filter(FileType -> isImage(FileType)).count();
                    fileLbl.setText(imgs > 1 ? "Varias imágenes" : "Archivo: " + current.getName());
                }
            } catch (Throwable ignore) {}

            grid.add(makeCard(
                    "Imágenes → PDF",
                    "Convierte la(s) imagen(es) seleccionadas a un único PDF (vista previa para ordenar/rotar/imprimir).",
                    ICON_PDF2IMG,
                    this::actionImagesToPdfPreview
            ));
            revalidate(); repaint(); scrollToTop();
            return;
        }

        if (isPdf(current)) {
            grid.add(makeCard("Dividir PDF (rango)", "Separar en uno o varios PDFs según rango de páginas.", ICON_SPLIT, this::actionSplitRange));
            grid.add(makeCard("Extraer texto", "Exportar texto del documento a TXT.", ICON_EXTRACT, this::actionExtractText));
            grid.add(makeCard("Marca de agua (texto)", "Añadir marca de agua de texto al PDF.", ICON_WATERMARK, this::actionWatermarkText));
            grid.add(makeCard("Agregar imagen", "Insertar una imagen en todas las páginas.", ICON_ADDIMG, this::actionAddImage));
            grid.add(makeCard("Rotar páginas", "Rotar páginas seleccionadas (90°/180°/270°).", ICON_ROTATE, this::actionRotatePages));
            grid.add(makeCard("Unir con otros PDFs", "Combinar el archivo con otros PDFs.", ICON_MERGE, this::actionMergeWithOthers));
            grid.add(makeCard("PDF → Imágenes (ZIP)", "Exportar todas las páginas como imágenes (ZIP).", ICON_PDF2IMG, this::actionPdfToImagesZip));
        }

        revalidate(); repaint();
        scrollToTop();
    }

    private RoundedCard makeCard(String title, String desc, String iconPath,
                                 java.util.function.Consumer<java.awt.event.MouseEvent> onClick) {

        RoundedCard card = new RoundedCard();
        card.setLayout(new GridBagLayout());
        card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        card.setPreferredSize(CARD_SIZE);
        card.setMinimumSize(CARD_SIZE);
        card.setOpaque(false);

        JPanel contentBox = new JPanel(new GridBagLayout());
        contentBox.setOpaque(false);

        JPanel inner = new JPanel();
        inner.setOpaque(false);
        inner.setLayout(new BoxLayout(inner, BoxLayout.Y_AXIS));
        inner.setAlignmentX(Component.CENTER_ALIGNMENT);
        inner.setBorder(new EmptyBorder(6, 16, 6, 16));

        JComponent icon = svgOrPngIcon(iconPath, ICON_SIZE);
        JPanel iconWrapper = new JPanel(new GridBagLayout());
        iconWrapper.setOpaque(false);
        iconWrapper.setPreferredSize(new Dimension(ICON_SIZE, ICON_SIZE));
        int bias = iconVerticalBias(iconPath);
        iconWrapper.setBorder(new EmptyBorder(Math.max(0, bias), 0, Math.max(0, -bias), 0));
        iconWrapper.add(icon);

        JLabel titleLbl = new JLabel(title, SwingConstants.CENTER);
        titleLbl.setFont(titleLbl.getFont().deriveFont(Font.BOLD, 15f));
        titleLbl.setForeground(uiColorOr("Label.foreground", new Color(24, 25, 26)));
        titleLbl.setAlignmentX(Component.CENTER_ALIGNMENT);

        int wrapW = CARD_SIZE.width - 64;
        JTextPane descPane = new JTextPane();
        descPane.setEditable(false);
        descPane.setFocusable(false);
        descPane.putClientProperty("JTextPane.honorDisplayProperties", Boolean.TRUE);
        descPane.setOpaque(false);
        descPane.setText(desc);
        descPane.setForeground(deriveInfoForeground());
        descPane.setFont(descPane.getFont().deriveFont(Font.PLAIN, 12f));
        descPane.setMargin(new Insets(0, 0, 0, 0));
        descPane.setBorder(new EmptyBorder(0, 6, 0, 6));

        // 👇 CLAVE: decirle el ancho y bloquear su alto calculado
        descPane.setSize(new Dimension(wrapW, Short.MAX_VALUE));
        Dimension pref = descPane.getPreferredSize();
        descPane.setPreferredSize(new Dimension(wrapW, pref.height));
        descPane.setMaximumSize(new Dimension(wrapW, pref.height));
        descPane.setMinimumSize(new Dimension(wrapW, 10));

        StyledDocument sd = descPane.getStyledDocument();
        SimpleAttributeSet center = new SimpleAttributeSet();
        StyleConstants.setAlignment(center, StyleConstants.ALIGN_CENTER);
        sd.setParagraphAttributes(0, sd.getLength(), center, false);

        inner.add(Box.createVerticalGlue());
        inner.add(iconWrapper);
        inner.add(Box.createVerticalStrut(12));
        inner.add(titleLbl);
        inner.add(Box.createVerticalStrut(6));
        inner.add(descPane);
        inner.add(Box.createVerticalGlue());

        GridBagConstraints gbcInner = new GridBagConstraints();
        gbcInner.gridx = 0; gbcInner.gridy = 0; gbcInner.anchor = GridBagConstraints.CENTER;
        contentBox.add(inner, gbcInner);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 1.0; gbc.weighty = 1.0; gbc.fill = GridBagConstraints.BOTH;
        card.add(contentBox, gbc);

        card.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) { if (onClick != null) onClick.accept(e); }
            @Override public void mouseEntered(java.awt.event.MouseEvent e) { card.setHover(true); }
            @Override public void mouseExited (java.awt.event.MouseEvent e) { card.setHover(false); }
        });

        card.setFocusable(true);
        card.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override public void keyPressed(java.awt.event.KeyEvent e) {
                if (onClick == null) return;
                int k = e.getKeyCode();
                if (k == java.awt.event.KeyEvent.VK_ENTER || k == java.awt.event.KeyEvent.VK_SPACE) onClick.accept(null);
            }
        });

        return card;
    }

    // =============================== Acciones ===============================
    private void actionSplitRange(MouseEvent e) {
        if (current == null) return;
        try { new SplitRangeDialog(mw, current).setVisible(true); }
        catch (Exception ex) { mw.fail(ex); }
    }

    private void actionExtractText(MouseEvent e) {
        if (current == null) return;
        try { new ExtractTextDialog(mw, current).setVisible(true); }
        catch (Exception ex) { mw.fail(ex); }
    }

    private void actionOfficeToPdf(java.awt.event.MouseEvent e) {
       if (current == null) return;
       try {
           new OfficeToPdfPreviewDialog(mw, current).setVisible(true);
       } catch (Exception ex) {
           mw.fail(ex);
       }
   }

    private void actionImagesToPdfPreview(MouseEvent e) {
        List<File> imgs = null;
        try {
            List<File> sel = mw.getSelectedFiles();
            if (sel != null && !sel.isEmpty()) {
                imgs = new ArrayList<>();
                for (File f : sel) if (isImage(f)) imgs.add(f);
            }
        } catch (Throwable ignore) {}

        if (imgs == null || imgs.isEmpty()) {
            imgs = FxFileDialogs.pickOpenMulti(
                    this, "Selecciona una o varias imágenes",
                    current != null ? current.getParentFile() : new File("."),
                    "png", "jpg", "jpeg", "bmp", "gif", "webp"
            );
            if (imgs == null || imgs.isEmpty()) return;
        }

        ImagesToPdfDialog dlg = new ImagesToPdfDialog(
                mw,
                current != null ? current.getParentFile() : new File("."),
                imgs
        );
        dlg.setVisible(true);
    }

    private void actionWatermarkText(MouseEvent e) {
        if (current == null) return;
        try { new WatermarkPreviewDialog(mw, current).setVisible(true); }
        catch (Exception ex) { mw.fail(ex); }
    }

    private void actionAddImage(MouseEvent e) {
        if (current == null) return;
        try { new InsertImagesAsPagesDialog(mw, current).setVisible(true); }
        catch (Exception ex) { mw.fail(ex); }
    }

    private void actionRotatePages(MouseEvent e) {
        if (current == null) return;
        try { new RotatePreviewDialog(mw, current).setVisible(true); }
        catch (Exception ex) { mw.fail(ex); }
    }

    private void actionMergeWithOthers(MouseEvent e) {
        if (current == null) return;

        List<File> others = FxFileDialogs.pickOpenMulti(
                this,
                "Selecciona 1+ PDFs para unir con " + current.getName(),
                current.getParentFile(),
                "pdf"
        );
        if (others == null || others.isEmpty()) return;

        List<File> initial = new ArrayList<>();
        initial.add(current);
        initial.addAll(others);

        new MergePreviewDialog(mw, initial).setVisible(true);
    }

    private void actionPdfToImagesZip(MouseEvent e) {
        if (current == null || !isPdf(current)) return;

        PdfToImagesOptionsDialog opts = new PdfToImagesOptionsDialog(mw);
        opts.setVisible(true);
        if (!opts.isOk()) return;

        String fmt = opts.getFormat();
        float dpi  = opts.getDpi();

        String suggested = suggestedName(current.getName(), "_imgs", "zip");

        File out = FxFileDialogs.pickSave(
                this,
                "Guardar imágenes (ZIP)…",
                current.getParentFile(),
                suggested,
                "zip"
        );
        if (out == null) return;

        final File outZip = out;

        mw.runAsync(() -> {
            try {
                mw.getSvc().pdfToImagesAsZip(current, outZip, fmt, dpi);
                mw.ok("Imágenes exportadas a: " + outZip.getAbsolutePath());
                mw.getHistory().add(
                        "PDF → Imágenes (ZIP)",
                        List.of(current.getAbsolutePath()),
                        outZip.getAbsolutePath()
                );
                mw.openIfWanted(outZip);
            } catch (Exception ex) { mw.fail(ex); }
        });
    }

    private void actionMergeSelected(List<File> selectedPdfs) {
        if (selectedPdfs == null || selectedPdfs.size() < 2) return;
        try { new MergePreviewDialog(mw, selectedPdfs).setVisible(true); }
        catch (Exception ex) { mw.fail(ex); }
    }

    // ============================== Iconos/UI ===============================
    private JComponent svgOrPngIcon(String path, int size) {
        String p = (path == null) ? "" : path.trim();
        if (p.isEmpty()) return placeholderIcon(size);

        try {
            if (p.toLowerCase().endsWith(".svg")) {
                String cp = p.startsWith("/") ? p.substring(1) : p;
                FlatSVGIcon svg = new FlatSVGIcon(cp, size, size);
                JLabel icon = new JLabel(svg, SwingConstants.CENTER);
                icon.setPreferredSize(new Dimension(size, size));
                return icon;
            }
        } catch (Throwable t) {}

        try {
            URL url = getClass().getResource(p);
            if (url == null && !p.startsWith("/")) url = getClass().getResource("/" + p);
            if (url != null) {
                BufferedImage img = ImageIO.read(url);
                if (img != null) {
                    Image scaled = img.getScaledInstance(size, size, Image.SCALE_SMOOTH);
                    JLabel icon = new JLabel(new ImageIcon(scaled), SwingConstants.CENTER);
                    icon.setPreferredSize(new Dimension(size, size));
                    return icon;
                }
            }
        } catch (Throwable ignore) {}

        return placeholderIcon(size);
    }

    private JComponent placeholderIcon(int size) {
        return new JComponent() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth(), h = getHeight();
                int s = Math.min(w, h) - 6;
                int x = (w - s) / 2, y = (h - s) / 2;
                g2.setColor(new Color(0xD32F2F));
                g2.fillRoundRect(x, y, s, s, s / 6, s / 6);
                g2.setColor(new Color(255, 255, 255, 230));
                g2.setFont(getFont().deriveFont(Font.BOLD, s * 0.28f));
                String txt = "PDF";
                FontMetrics fm = g2.getFontMetrics();
                int tw = fm.stringWidth(txt), th = fm.getAscent();
                g2.drawString(txt, x + (s - tw) / 2, y + (s + th) / 2 - s / 6);
                g2.dispose();
            }
            @Override public Dimension getPreferredSize() { return new Dimension(size, size); }
        };
    }

    // ============================== Helpers ================================
    private void scrollToTop() {
        SwingUtilities.invokeLater(() -> {
            if (scroll != null) {
                JViewport vp = scroll.getViewport();
                if (vp != null) vp.setViewPosition(new Point(0, 0));
                JScrollBar vbar = scroll.getVerticalScrollBar();
                if (vbar != null) vbar.setValue(vbar.getMinimum());
            }
        });
    }

    private static String suggestedName(String original, String suffixOrNull, String targetExtNoDot) {
        String base = original;
        int i = base.lastIndexOf('.');
        if (i > 0) base = base.substring(0, i);
        if (suffixOrNull != null && !suffixOrNull.isBlank()) base += suffixOrNull;
        return base + "." + targetExtNoDot;
    }

    private int iconVerticalBias(String path) {
        if (path == null) return 0;
        String p = path.toLowerCase();
        if (p.contains("g_dividir_pdf_rango"))  return -4;
        if (p.contains("i_marca_de_agua"))      return -6;
        if (p.contains("z_agregar_imagen"))     return -2;
        if (p.contains("e_unir_pdfs"))          return -2;
        if (p.contains("d_pdf_a_imagenes_zip")) return -2;
        if (p.endsWith("app_128.png"))          return -6;
        return 0;
    }

    private static Color deriveSecondaryForeground() {
        Color fg = UIManager.getColor("Label.foreground");
        Color bg = UIManager.getColor("Panel.background");
        if (fg == null) fg = new Color(30, 30, 30);
        if (bg == null) bg = Color.WHITE;
        return mix(fg, bg, 0.35);
    }

    private static Color deriveInfoForeground() {
        Color c = UIManager.getColor("Label.infoForeground");
        if (c != null) return c;
        Color fg = UIManager.getColor("Label.foreground");
        if (fg == null) fg = new Color(24, 25, 26);
        return new Color(fg.getRed(), fg.getGreen(), fg.getBlue(), 180);
    }

    private static Color uiColorOr(String key, Color fallback) {
        Color c = UIManager.getColor(key);
        return c != null ? c : fallback;
    }

    private static Color mix(Color a, Color b, double t) {
        int r = (int) Math.round(a.getRed() * (1 - t) + b.getRed() * t);
        int g = (int) Math.round(a.getGreen() * (1 - t) + b.getGreen() * t);
        int bl = (int) Math.round(a.getBlue() * (1 - t) + b.getBlue() * t);
        return new Color(r, g, bl);
    }

    private static String commonParent(List<File> files) {
        if (files == null || files.isEmpty()) return "";
        File first = files.get(0);
        File parent = first != null ? first.getParentFile() : null;
        if (parent == null) return "";
        String base = parent.getAbsolutePath();
        for (File f : files) {
            File p = (f == null ? null : f.getParentFile());
            if (p == null || !base.equals(p.getAbsolutePath())) return base + " + …";
        }
        return base;
    }
}
