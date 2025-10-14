package com.mycompany.programa_pdf.ui;

import javax.swing.*;
import java.awt.datatransfer.DataFlavor;
import java.io.File;
import java.util.*;

import static com.mycompany.programa_pdf.io.FileTypeUtils.*;

public class FileDropHandler extends TransferHandler {

    @FunctionalInterface
    public interface Listener {
        void onFilesDropped(List<File> accepted, Set<String> unsupportedExts);
    }

    private final JComponent highlightTarget;
    private final Listener listener;

    public FileDropHandler(JComponent highlightTarget, Listener listener) {
        this.highlightTarget = highlightTarget;
        this.listener = listener;
    }

    @Override
    public boolean canImport(TransferSupport s) {
        boolean ok = s.isDataFlavorSupported(DataFlavor.javaFileListFlavor)
                  || s.isDataFlavorSupported(DataFlavor.stringFlavor);
        s.setDropAction(COPY);
        s.setShowDropLocation(true);
        setHighlighted(ok);
        return ok;
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean importData(TransferSupport s) {
        final List<File> raw = new ArrayList<>();
        try {
            if (s.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                raw.addAll((List<File>) s.getTransferable().getTransferData(DataFlavor.javaFileListFlavor));
            } else if (s.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                String uris = (String) s.getTransferable().getTransferData(DataFlavor.stringFlavor);
                raw.addAll(parseUriList(uris));
            } else {
                return false;
            }
        } catch (Exception ex) {
            return false;
        } finally {
            setHighlighted(false);
        }

        List<File> accepted = new ArrayList<>();
        Set<String> unsupported = new LinkedHashSet<>();

        for (File f : raw) {
            if (f == null || !f.exists()) continue;
            if (f.isDirectory()) {
                File[] arr = f.listFiles();
                if (arr != null) {
                    for (File x : arr) classify(x, accepted, unsupported);
                }
            } else {
                classify(f, accepted, unsupported);
            }
        }

        if (listener != null) {
            listener.onFilesDropped(accepted, unsupported);
        }
        return !accepted.isEmpty();
    }

    private void classify(File f, List<File> accepted, Set<String> unsupported) {
        if (isSupported(f)) {
            accepted.add(f);
        } else {
            String n = f.getName().toLowerCase(Locale.ROOT);
            int i = n.lastIndexOf('.');
            if (i >= 0 && i + 1 < n.length()) unsupported.add(n.substring(i + 1));
        }
    }

    private void setHighlighted(boolean value) {
       
        try { highlightTarget.putClientProperty("drop.highlight", value); } catch (Exception ignore) {}
       
        try {
            var m = highlightTarget.getClass().getMethod("setHighlighted", boolean.class);
            m.invoke(highlightTarget, value);
        } catch (Exception ignore) {}
    }
}
