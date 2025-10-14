package com.mycompany.programa_pdf.io;


import java.io.File;
import java.util.ArrayList;
import java.util.List;

import java.awt.Component;
import java.awt.Dialog;
import java.awt.FileDialog;
import java.awt.Frame;
import java.awt.Window;
import javax.swing.SwingUtilities;


public final class NativeFileDialogs {

    private NativeFileDialogs() {}

    /** Abre el diálogo nativo de “Abrir” con selección múltiple. */
    public static File[] pickOpenMulti(Component parent, String title, File initialDir, String... allowedExts) {
        Window w = parent != null ? SwingUtilities.getWindowAncestor(parent) : null;

        FileDialog fd;
        if (w instanceof Frame f) {
            fd = new FileDialog(f, title, FileDialog.LOAD);
        } else if (w instanceof Dialog d) {
            fd = new FileDialog(d, title, FileDialog.LOAD);
        } else {
            fd = new FileDialog((Frame) null, title, FileDialog.LOAD);
        }

        if (initialDir != null && initialDir.isDirectory())
            fd.setDirectory(initialDir.getAbsolutePath());

        fd.setMultipleMode(true); // permitir varias selecciones
        fd.setVisible(true);

        File[] chosen = fd.getFiles();
        if (chosen == null || chosen.length == 0) return null;

        // Validación final de extensiones (el filtro del SO puede ser laxo)
        if (allowedExts == null || allowedExts.length == 0) return chosen;

        String[] allowed = toLower(allowedExts);
        List<File> ok = new ArrayList<>();
        for (File f : chosen) {
            if (hasAnyExt(f.getName(), allowed)) ok.add(f);
        }
        return ok.isEmpty() ? null : ok.toArray(new File[0]);
    }

    /** Abre el diálogo nativo de “Guardar”. */
    public static File pickSave(Component parent, String title, File initialFile, String defaultExtNoDot) {
        Window w = parent != null ? SwingUtilities.getWindowAncestor(parent) : null;

        FileDialog fd;
        if (w instanceof Frame f) {
            fd = new FileDialog(f, title, FileDialog.SAVE);
        } else if (w instanceof Dialog d) {
            fd = new FileDialog(d, title, FileDialog.SAVE);
        } else {
            fd = new FileDialog((Frame) null, title, FileDialog.SAVE);
        }

        if (initialFile != null) {
            if (initialFile.getParentFile() != null)
                fd.setDirectory(initialFile.getParentFile().getAbsolutePath());
            fd.setFile(initialFile.getName());
        }

        fd.setVisible(true);
        String dir  = fd.getDirectory();
        String file = fd.getFile();
        if (dir == null || file == null) return null;

        File out = new File(dir, file);
        if (defaultExtNoDot != null && !defaultExtNoDot.isBlank() && !file.contains(".")) {
            out = new File(dir, file + "." + defaultExtNoDot);
        }
        return out;
    }

    // ===== helpers =====
    private static boolean hasAnyExt(String name, String... exts) {
        String n = name.toLowerCase();
        int i = n.lastIndexOf('.');
        if (i < 0) return false;
        String ext = n.substring(i + 1);
        for (String e : exts) if (ext.equals(e)) return true;
        return false;
    }

    private static String[] toLower(String[] arr) {
        String[] r = new String[arr.length];
        for (int i = 0; i < arr.length; i++) r[i] = arr[i].toLowerCase();
        return r;
    }
}
