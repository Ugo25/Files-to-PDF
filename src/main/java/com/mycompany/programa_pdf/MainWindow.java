package com.mycompany.programa_pdf;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.mycompany.programa_pdf.dialogs.HistoryDialog;
import com.mycompany.programa_pdf.pdf.Servicio_Convertir;
import com.mycompany.programa_pdf.state.HistoryStore;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

public class MainWindow extends JFrame {

    // ============================ Preferencias/Tema =========================
    private static final String PREF_NODE  = "com.mycompany.programa_pdf";
    private static final String PREF_THEME = "theme"; // "light" | "dark"

    private static void applySavedTheme() {
        String theme = Preferences.userRoot().node(PREF_NODE).get(PREF_THEME, "light");
        try {
            if ("dark".equalsIgnoreCase(theme)) FlatDarkLaf.setup();
            else FlatLightLaf.setup();
        } catch (Exception ignore) {}
    }
    private static void saveTheme(String theme) {
        Preferences.userRoot().node(PREF_NODE).put(PREF_THEME, theme);
    }

    // ========================= Servicios & Estado App =======================
    private final Servicio_Convertir svc = new Servicio_Convertir();
    private final HistoryStore history = new HistoryStore();

    private File selectedFile = null;
    private File lastDir = new File(System.getProperty("user.home"), "Documents");
    private File libreOfficeHome = null;

    private final JTextArea log = new JTextArea(8, 70);
    private final JProgressBar progress = new JProgressBar();
    private final JCheckBox chkAbrir = new JCheckBox("Abrir al terminar", true);

    // =============================== Navegación =============================
    private final CardLayout rootLayout = new CardLayout();
    private final JPanel rootCards = new JPanel(rootLayout);

    private HomePanel homePanel;
    private CardsPanel cardsPanel;
    private JComponent cardsBottom;

    // Selección múltiple
    private List<File> selectedFiles = new ArrayList<>();

    // Modo de operación
    private String mode = "imagesToPdf"; // imagesToPdf | mergePdf | officeToPdf
    public void   setMode(String mode) { this.mode = (mode == null ? "imagesToPdf" : mode); }
    public String getMode() { return mode; }

    // Full-screen
    private boolean fullScreen = false;
    private final GraphicsDevice device =
            GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();

    // =============================== Constructor ===========================
    public MainWindow() {
        super("Files to PDF");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1000, 680));
        setLocationByPlatform(true);

        setJMenuBar(buildMenuBar());

        homePanel  = new HomePanel(this);
        cardsPanel = new CardsPanel(this);

        JPanel cardsScreen = new JPanel(new BorderLayout());
        cardsScreen.add(cardsPanel, BorderLayout.CENTER);
        cardsBottom = buildBottom();
        cardsScreen.add(cardsBottom, BorderLayout.SOUTH);

        rootCards.add(homePanel,  "home");
        rootCards.add(cardsScreen,"cards");
        add(rootCards, BorderLayout.CENTER);

        List<Image> appIcons = loadAppIcons();
        if (!appIcons.isEmpty()) {
            setIconImages(appIcons);
            try { Taskbar.getTaskbar().setIconImage(appIcons.get(appIcons.size() - 1)); } catch (Exception ignore) {}
        }

        // F11: pantalla completa
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke("F11"), "toggleFS");
        getRootPane().getActionMap().put("toggleFS", new AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { toggleFullScreen(); }
        });

        SwingUtilities.invokeLater(() ->
                setExtendedState(getExtendedState() | JFrame.MAXIMIZED_BOTH)
        );
    }

    // ================================= Menú ================================
    private JMenuBar buildMenuBar() {
        JMenuBar mb = new JMenuBar();
        JMenu mVer = new JMenu("Ver");

        JMenuItem miHist = new JMenuItem("Historial…");
        miHist.setAccelerator(KeyStroke.getKeyStroke("control H"));
        miHist.addActionListener(e -> new HistoryDialog(this, history).setVisible(true));
        mVer.add(miHist);

        JMenu mTema = new JMenu("Tema");
        String saved = Preferences.userRoot().node(PREF_NODE).get(PREF_THEME, "light");
        boolean dark = "dark".equalsIgnoreCase(saved);

        JRadioButtonMenuItem miClaro  = new JRadioButtonMenuItem("Claro",  !dark);
        JRadioButtonMenuItem miOscuro = new JRadioButtonMenuItem("Oscuro",  dark);
        ButtonGroup g = new ButtonGroup(); g.add(miClaro); g.add(miOscuro);

        miClaro.addActionListener(e -> {
            try { FlatLightLaf.setup(); saveTheme("light"); SwingUtilities.updateComponentTreeUI(this); }
            catch (Exception ignored) {}
        });
        miOscuro.addActionListener(e -> {
            try { FlatDarkLaf.setup();  saveTheme("dark");  SwingUtilities.updateComponentTreeUI(this); }
            catch (Exception ignored) {}
        });

        mTema.add(miClaro); mTema.add(miOscuro);
        mVer.add(mTema);

        mb.add(mVer);
        return mb;
    }

    // ========================== Pie (log + progreso) =======================
    private JComponent buildBottom() {
        JPanel bottom = new JPanel(new BorderLayout(10, 10));
        bottom.setOpaque(false);

        JPanel opts = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        opts.setOpaque(false);
        opts.add(chkAbrir);
        bottom.add(opts, BorderLayout.NORTH);

        log.setEditable(false);
        log.setLineWrap(true);
        log.setWrapStyleWord(true);
        log.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));

        JScrollPane sp = new JScrollPane(log);
        sp.setPreferredSize(new Dimension(100, 140));
        bottom.add(sp, BorderLayout.CENTER);

        log.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            private void scroll() { SwingUtilities.invokeLater(() -> log.setCaretPosition(log.getDocument().getLength())); }
            public void insertUpdate(javax.swing.event.DocumentEvent e) { scroll(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) {}
            public void changedUpdate(javax.swing.event.DocumentEvent e) {}
        });

        progress.setIndeterminate(false);
        bottom.add(progress, BorderLayout.SOUTH);
        return bottom;
    }

    // ============================== Navegación ==============================
    public void goHome()  { rootLayout.show(rootCards, "home"); }

    public void goCards() {
        try {
            // Nuevo signature (CardsPanel con modos y selección múltiple)
            var m = cardsPanel.getClass().getMethod("refreshFor", java.util.List.class, String.class);
            m.invoke(cardsPanel, getSelectedFiles(), getMode());
        } catch (Exception ignore) {
            // Compatibilidad hacia atrás (firma antigua)
            cardsPanel.refreshFor(getSelectedFile());
        }
        rootLayout.show(rootCards, "cards");
    }

    // ================================ Helpers ==============================
    public void runAsync(Runnable job) {
        progress.setIndeterminate(true);
        setControlsEnabled(false);
        new SwingWorker<Void, Void>() {
            @Override protected Void doInBackground() { job.run(); return null; }
            @Override protected void done() {
                progress.setIndeterminate(false);
                setControlsEnabled(true);
            }
        }.execute();
    }

    private void setControlsEnabled(boolean enabled) {
        chkAbrir.setEnabled(enabled);
        if (cardsPanel != null) cardsPanel.setGridEnabled(enabled);
    }

    public void openIfWanted(File f) {
        if (chkAbrir.isSelected() && Desktop.isDesktopSupported()) {
            try { Desktop.getDesktop().open(f); } catch (Exception ignore) {}
        }
    }

    public void ok  (String m){ log.append("✔ " + m + "\n"); }
    public void warn(String m){ log.append("⚠ " + m + "\n"); }
    public void info(String m){ log.append("ℹ " + m + "\n"); }
    public void fail(Exception e){
        log.append("✖ " + e.getClass().getSimpleName() + ": " + (e.getMessage()==null?"":e.getMessage()) + "\n");
        e.printStackTrace();
    }

    public File suggestSame(File in, String newExtNoDot) {
        String name = in.getName();
        int i = name.lastIndexOf('.');
        String base = (i > 0) ? name.substring(0, i) : name;
        return new File(in.getParentFile(), base + "." + newExtNoDot);
    }

    public File suggestWithSuffix(File in, String suffix, String extNoDot) {
        String name = in.getName();
        int i = name.lastIndexOf('.');
        String base = (i > 0) ? name.substring(0, i) : name;
        return new File(in.getParentFile(), base + suffix + "." + extNoDot);
    }

    private List<Image> loadAppIcons() {
        int[] sizes = {16, 32, 48, 64, 128, 256};
        List<Image> images = new ArrayList<>();
        for (int sz : sizes) {
            String path = "/icons/app_" + sz + ".png";
            try {
                var url = MainWindow.class.getResource(path);
                if (url != null) {
                    var img = javax.imageio.ImageIO.read(url);
                    if (img != null) images.add(img);
                }
            } catch (Exception ignore) {}
        }
        return images;
    }

    // ============================ Selección múltiple ========================
    public List<File> getSelectedFiles() { return selectedFiles; }

    public void setSelectedFiles(List<File> files) {
        selectedFiles = (files == null) ? new ArrayList<>() : new ArrayList<>(files);
        selectedFile = selectedFiles.isEmpty() ? null : selectedFiles.get(0);
        if (homePanel != null) homePanel.externalSetSelectedList(new ArrayList<>(selectedFiles));
    }

    // ============================= Getters/Setters ==========================
    public Servicio_Convertir getSvc() { return svc; }
    public HistoryStore getHistory() { return history; }

    public File getSelectedFile() { return selectedFile; }
    public void setSelectedFile(File f) {
        selectedFile = f;
        if (f != null) lastDir = f.getParentFile();
        if (homePanel != null) homePanel.updateSelected(f);
    }

    public File getLastDir() { return lastDir; }
    public void setLastDir(File d){ lastDir = d; }

    public File getLibreOfficeHome() { return libreOfficeHome; }
    public void setLibreOfficeHome(File home) { this.libreOfficeHome = home; }

    // ============================ Pantalla completa =========================
    private void toggleFullScreen() {
        dispose();
        setUndecorated(!fullScreen);
        if (!fullScreen) {
            device.setFullScreenWindow(this);
        } else {
            device.setFullScreenWindow(null);
            setVisible(true);
            setExtendedState(getExtendedState() | JFrame.MAXIMIZED_BOTH);
        }
        fullScreen = !fullScreen;
    }

    // ================================= Main =================================
    public static void main(String[] args) {
        applySavedTheme();
        SwingUtilities.invokeLater(() -> new MainWindow().setVisible(true));
    }
}
