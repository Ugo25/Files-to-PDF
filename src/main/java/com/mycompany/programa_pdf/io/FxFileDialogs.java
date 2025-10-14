package com.mycompany.programa_pdf.io;

import java.awt.Component;
import java.awt.FileDialog;
import java.awt.Frame;
import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.List;

/**
 * Diálogos nativos:
 * - Preferente: SWT (Common Item Dialog moderno de Windows).
 * - Fallback: AWT FileDialog.
 *
 * Importante: si el usuario CANCELA en SWT devolvemos null y NO disparamos
 * el fallback. Solo se usa AWT si SWT no está en classpath o falla con excepción.
 */
public final class FxFileDialogs {

    private FxFileDialogs() {}

    /** Abrir múltiples archivos (devuelve null si el usuario cancela). */
    public static List<File> pickOpenMulti(Component parent, String title, File initialDir, String... allowedExts) {
        if (swtAvailable() && isWindows()) {
            try {
                // Si el usuario cancela, retorna null y NO hay fallback.
                return swtOpenMulti(title, initialDir, allowedExts);
            } catch (Throwable ignore) {
                // SWT falló -> usamos AWT como plan B.
            }
        }
        return awtOpenMulti(parent, title, initialDir, allowedExts);
    }

    /** Guardar archivo (devuelve null si el usuario cancela). */
    public static File pickSave(Component parent, String title, File initialDir, String suggestedName, String... allowedExts) {
        if (swtAvailable() && isWindows()) {
            try {
                // Si el usuario cancela, retorna null y NO hay fallback.
                return swtSave(title, initialDir, suggestedName, allowedExts);
            } catch (Throwable ignore) {
                // SWT falló -> usamos AWT como plan B.
            }
        }
        return awtSave(parent, title, initialDir, suggestedName, allowedExts);
    }

    /* ========================== SWT ========================== */

    private static boolean swtAvailable() {
        try {
            Class.forName("org.eclipse.swt.widgets.FileDialog");
            Class.forName("org.eclipse.swt.widgets.Display");
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    private static boolean isWindows() {
        return System.getProperty("os.name","").toLowerCase().contains("win");
    }

    private static List<File> swtOpenMulti(String title, File initialDir, String... allowedExts) {
        org.eclipse.swt.widgets.Display display = org.eclipse.swt.widgets.Display.getCurrent();
        boolean createdDisplay = false;
        if (display == null) {
            display = new org.eclipse.swt.widgets.Display();
            createdDisplay = true;
        }
        org.eclipse.swt.widgets.Shell shell = new org.eclipse.swt.widgets.Shell(display);
        try {
            int style = org.eclipse.swt.SWT.OPEN | org.eclipse.swt.SWT.MULTI;
            org.eclipse.swt.widgets.FileDialog dlg = new org.eclipse.swt.widgets.FileDialog(shell, style);
            dlg.setText((title != null && !title.isBlank()) ? title : "Abrir");
            if (initialDir != null && initialDir.isDirectory()) {
                dlg.setFilterPath(initialDir.getAbsolutePath());
            }

            String[] clean = normalizeExts(allowedExts);
            if (clean.length > 0) {
                dlg.setFilterExtensions(new String[]{ joinAsPattern(clean), "*.*" });
                dlg.setFilterNames(new String[]{ "Todos los soportados", "Todos los archivos (*.*)" });
                dlg.setFilterIndex(0);
            }

            String first = dlg.open();                 // null si cancelan
            if (first == null) return null;

            String base = dlg.getFilterPath();
            String[] names = dlg.getFileNames();
            List<File> out = new ArrayList<>();
            if (names != null && names.length > 0) {
                for (String n : names) out.add(new File(base, n));
            } else {
                out.add(new File(base, first));
            }
            return out.isEmpty() ? null : out;
        } finally {
            try { shell.dispose(); } catch (Throwable ignore) {}
            if (createdDisplay) {
                try { display.dispose(); } catch (Throwable ignore) {}
            }
        }
    }

    private static File swtSave(String title, File initialDir, String suggestedName, String... allowedExts) {
        org.eclipse.swt.widgets.Display display = org.eclipse.swt.widgets.Display.getCurrent();
        boolean createdDisplay = false;
        if (display == null) {
            display = new org.eclipse.swt.widgets.Display();
            createdDisplay = true;
        }
        org.eclipse.swt.widgets.Shell shell = new org.eclipse.swt.widgets.Shell(display);
        try {
            int style = org.eclipse.swt.SWT.SAVE;
            org.eclipse.swt.widgets.FileDialog dlg = new org.eclipse.swt.widgets.FileDialog(shell, style);
            dlg.setText((title != null && !title.isBlank()) ? title : "Guardar");
            if (initialDir != null && initialDir.isDirectory()) dlg.setFilterPath(initialDir.getAbsolutePath());
            if (suggestedName != null && !suggestedName.isBlank()) dlg.setFileName(suggestedName);

            String[] clean = normalizeExts(allowedExts);
            if (clean.length > 0) {
                dlg.setFilterExtensions(new String[]{ joinAsPattern(clean), "*.*" });
                dlg.setFilterNames(new String[]{ "Todos los soportados", "Todos los archivos (*.*)" });
                dlg.setFilterIndex(0);
            }

            String chosen = dlg.open();                // null si cancelan
            if (chosen == null) return null;

            File file = new File(dlg.getFilterPath(), dlg.getFileName());
            if (clean.length == 1 && !hasAnyExtension(file.getName())) {
                file = new File(file.getParentFile(), file.getName() + "." + clean[0]);
            }
            return file;
        } finally {
            try { shell.dispose(); } catch (Throwable ignore) {}
            if (createdDisplay) {
                try { display.dispose(); } catch (Throwable ignore) {}
            }
        }
    }

    private static String joinAsPattern(String[] clean) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < clean.length; i++) {
            if (i > 0) sb.append(';');
            sb.append("*.").append(clean[i]);
        }
        return sb.toString();
    }

    /* ========================== AWT fallback ========================== */

    private static List<File> awtOpenMulti(Component parent, String title, File initialDir, String... allowedExts) {
        boolean[] createdOwner = new boolean[1];
        Frame owner = ownerFrame(parent, createdOwner);
        FileDialog fd = new FileDialog(owner, title != null ? title : "Abrir", FileDialog.LOAD);

        forceWinCommonDialog();

        if (initialDir != null && initialDir.isDirectory()) {
            fd.setDirectory(initialDir.getAbsolutePath());
        }

        String[] clean = normalizeExts(allowedExts);
        if (clean.length > 0) {
            fd.setFilenameFilter(strictExtFilter(clean));
            fd.setFile(defaultPattern(clean)); // pista visual en Windows
        }

        try { fd.setMultipleMode(true); } catch (Throwable ignore) {}

        fd.setVisible(true);

        File[] chosen = null;
        try { chosen = fd.getFiles(); } catch (Throwable ignore) {}

        List<File> out = new ArrayList<>();
        if (chosen != null && chosen.length > 0) {
            for (File f : chosen) if (f != null) out.add(f);
        } else {
            String dir = fd.getDirectory(), file = fd.getFile();
            if (dir != null && file != null) out.add(new File(dir, file));
        }

        // Cierra el owner temporal si lo creamos nosotros
        if (createdOwner[0]) {
            try { owner.dispose(); } catch (Throwable ignore) {}
        }
        return out.isEmpty() ? null : out;
    }

    private static File awtSave(Component parent, String title, File initialDir, String suggestedName, String... allowedExts) {
        boolean[] createdOwner = new boolean[1];
        Frame owner = ownerFrame(parent, createdOwner);
        FileDialog fd = new FileDialog(owner, title != null ? title : "Guardar", FileDialog.SAVE);

        forceWinCommonDialog();

        if (initialDir != null && initialDir.isDirectory()) {
            fd.setDirectory(initialDir.getAbsolutePath());
        }

        String[] clean = normalizeExts(allowedExts);
        if (suggestedName != null && !suggestedName.isBlank()) {
            fd.setFile(suggestedName);
        } else if (clean.length > 0) {
            fd.setFile(defaultPattern(clean));
        }
        if (clean.length > 0) fd.setFilenameFilter(strictExtFilter(clean));

        fd.setVisible(true);

        String dir = fd.getDirectory(), file = fd.getFile();
        if (createdOwner[0]) {
            try { owner.dispose(); } catch (Throwable ignore) {}
        }
        if (dir == null || file == null) return null;

        File chosen = new File(dir, file);
        if (clean.length == 1 && !hasAnyExtension(chosen.getName())) {
            chosen = new File(chosen.getParentFile(), chosen.getName() + "." + clean[0].toLowerCase());
        }
        return chosen;
    }

    /* ========================== utilidades ========================== */

    private static void forceWinCommonDialog() {
        try {
            if (isWindows()) System.setProperty("sun.awt.windows.useCommonItemDialog", "true");
        } catch (Throwable ignore) {}
    }

    /**
     * Devuelve un Frame dueño. Si no existe en la jerarquía, crea uno temporal
     * (invisible) y marca createdOwner[0] = true para poder cerrarlo al final.
     */
    private static Frame ownerFrame(Component c, boolean[] createdOwner) {
        Frame f = null; Component cur = c;
        while (cur != null) {
            if (cur instanceof Frame) { f = (Frame) cur; break; }
            cur = cur.getParent();
        }
        if (f == null) {
            f = new Frame();
            f.setUndecorated(true);
            createdOwner[0] = true;
        } else {
            createdOwner[0] = false;
        }
        return f;
    }

    private static String[] normalizeExts(String... exts) {
        if (exts == null) return new String[0];
        List<String> out = new ArrayList<>();
        for (String e : exts) {
            if (e == null) continue;
            String s = e.trim().toLowerCase();
            if (s.isEmpty()) continue;
            if (s.startsWith("*.")) s = s.substring(2);
            if (s.startsWith("."))  s = s.substring(1);
            if (!s.isEmpty()) out.add(s);
        }
        return out.toArray(new String[0]);
    }

    private static boolean hasAnyExtension(String name) {
        int dot = name.lastIndexOf('.');
        return dot > 0 && dot < name.length() - 1;
    }

    private static FilenameFilter strictExtFilter(String[] clean) {
        if (clean == null || clean.length == 0) return (dir, name) -> true;
        return (dir, name) -> {
            String n = name.toLowerCase();
            for (String ext : clean) if (n.endsWith("." + ext)) return true;
            return false;
        };
    }

    private static String defaultPattern(String[] clean) {
        if (isWindows()) {
            if (clean.length == 1) return "*." + clean[0];
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < clean.length; i++) {
                if (i > 0) sb.append(';');
                sb.append("*.").append(clean[i]);
            }
            return sb.toString();
        }
        return clean.length > 0 ? "*." + clean[0] : "*.*";
    }
}
