package com.mycompany.programa_pdf.ui;

import java.io.File;
import java.util.List;

public final class SelectionUtils {
    private SelectionUtils(){}

    public static String commonParentOf(List<File> files) {
        if (files == null || files.isEmpty()) return "";
        File parent = files.get(0).getParentFile();
        if (parent == null) return "";
        for (File f : files) {
            File p = (f == null ? null : f.getParentFile());
            if (p == null || !p.equals(parent)) return parent.getAbsolutePath() + " + …";
        }
        return parent.getAbsolutePath();
    }
}
