package com.mycompany.programa_pdf.io;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.File;
import java.util.Locale;

public class Dialogs {

    // ================================ Open ================================
    public static File pickOpen(java.awt.Component parent, String title, File initialDir, FileNameExtensionFilter filter) {
        JFileChooser c = baseChooser(initialDir, title);
        if (filter != null) c.setFileFilter(filter);
        return c.showOpenDialog(parent) == JFileChooser.APPROVE_OPTION ? c.getSelectedFile() : null;
    }

    public static File pickOpen(java.awt.Component parent, String title, File initialDir, String... exts) {
        return pickOpen(parent, title, initialDir, toFilter(title, exts));
    }

    // ============================== Open Multi ==============================
    public static File[] pickOpenMulti(java.awt.Component parent, String title, File initialDir, FileNameExtensionFilter filter) {
        JFileChooser c = baseChooser(initialDir, title);
        c.setMultiSelectionEnabled(true);
        if (filter != null) c.setFileFilter(filter);
        return c.showOpenDialog(parent) == JFileChooser.APPROVE_OPTION ? c.getSelectedFiles() : null;
    }

    public static File[] pickOpenMulti(java.awt.Component parent, String title, File initialDir, String... exts) {
        return pickOpenMulti(parent, title, initialDir, toFilter(title, exts));
    }

    // ================================ Save =================================
    public static File pickSave(java.awt.Component parent, String title, File defaultFile, FileNameExtensionFilter filter) {
        JFileChooser c = baseChooser(defaultFile != null ? defaultFile.getParentFile() : null, title);
        if (defaultFile != null) c.setSelectedFile(defaultFile);
        if (filter != null) c.setFileFilter(filter);

        if (c.showSaveDialog(parent) != JFileChooser.APPROVE_OPTION) return null;

        File sel = c.getSelectedFile();
        if (sel == null) return null;

        String ensured = ensureExtension(sel.getName(), filter);
        File out = ensured.equals(sel.getName()) ? sel : new File(sel.getParentFile(), ensured);

        if (!confirmOverwriteIfExists(parent, out)) return null;
        if (!validateParentDir(parent, out)) return null;
        return out;
    }

    public static File pickSave(java.awt.Component parent, String title, File defaultFile, String... exts) {
        return pickSave(parent, title, defaultFile, toFilter(title, exts));
    }

    // ============================= Internals ================================
    private static JFileChooser baseChooser(File initialDir, String title) {
        JFileChooser c = new JFileChooser(initialDir != null ? initialDir : new File("."));
        c.setDialogTitle(title != null ? title : "");
        c.setFileSelectionMode(JFileChooser.FILES_ONLY);
        return c;
    }

    private static FileNameExtensionFilter toFilter(String title, String... exts) {
        if (exts == null || exts.length == 0) return null;
        String desc = (title == null || title.isBlank()) ? "Archivos" : title;
        String[] low = new String[exts.length];
        for (int i = 0; i < exts.length; i++) low[i] = cleanExt(exts[i]);
        return new FileNameExtensionFilter(desc, low);
    }

    private static String cleanExt(String ext) {
        String e = ext == null ? "" : ext.trim().toLowerCase(Locale.ROOT);
        return e.startsWith(".") ? e.substring(1) : e;
    }

    private static String ensureExtension(String name, FileNameExtensionFilter filter) {
        if (filter == null) return name;
        String[] exts = filter.getExtensions();
        if (exts == null || exts.length == 0) return name;

        String lower = name.toLowerCase(Locale.ROOT);
        for (String e : exts) {
            String dot = "." + e.toLowerCase(Locale.ROOT);
            if (lower.endsWith(dot)) return name;
        }
        return name + "." + exts[0].toLowerCase(Locale.ROOT);
    }

    private static boolean confirmOverwriteIfExists(java.awt.Component parent, File f) {
        if (!f.exists()) return true;
        int resp = JOptionPane.showConfirmDialog(
                parent,
                "El archivo ya existe.\n¿Deseas reemplazarlo?",
                "Confirmar sobrescritura",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );
        return resp == JOptionPane.YES_OPTION;
    }

    private static boolean validateParentDir(java.awt.Component parent, File f) {
        File dir = f.getParentFile();
        if (dir != null && dir.isDirectory()) return true;
        JOptionPane.showMessageDialog(parent,
                "La carpeta destino no existe. Elige una carpeta válida.",
                "Carpeta inválida",
                JOptionPane.ERROR_MESSAGE);
        return false;
    }
}
