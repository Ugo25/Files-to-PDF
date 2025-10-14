package com.mycompany.programa_pdf.io;

import java.io.File;
import java.net.URI;
import java.util.*;

public final class FileTypeUtils {
    private FileTypeUtils() {}

    private static final Set<String> IMG = unmodifiable("png","jpg","jpeg","webp","bmp","gif");
    private static final Set<String> PDF = unmodifiable("pdf");
    private static final Set<String> OFF = unmodifiable("doc","docx","xls","xlsx","ppt","pptx","odt","ods","odp");

    public static boolean isImage (File f) { return IMG.contains(ext(f)); }
    public static boolean isPdf   (File f) { return PDF.contains(ext(f)); }
    public static boolean isOffice(File f) { return OFF.contains(ext(f)); }
    public static boolean isSupported(File f) { return isImage(f) || isPdf(f) || isOffice(f); }

    public static List<File> parseUriList(String data) {
        List<File> out = new ArrayList<>();
        if (data == null) return out;
        for (String line : data.split("\\r?\\n")) {
            String s = line.trim();
            if (s.isEmpty() || s.startsWith("#")) continue;
            try { if (s.startsWith("file:/")) out.add(new File(URI.create(s))); } catch (Exception ignore) {}
        }
        return out;
    }

    public static List<File> flattenSupported(List<File> in) {
        List<File> out = new ArrayList<>();
        if (in == null) return out;
        for (File f : in) {
            if (f == null || !f.exists()) continue;
            if (f.isDirectory()) {
                File[] arr = f.listFiles();
                if (arr != null) for (File x : arr) if (isSupported(x)) out.add(x);
            } else if (isSupported(f)) {
                out.add(f);
            }
        }
        return out;
    }

    public static String extension(File f) { return ext(f); }

    private static String ext(File f) {
        if (f == null || !f.isFile()) return "";
        String n = f.getName().toLowerCase(Locale.ROOT);
        int i = n.lastIndexOf('.');
        return (i >= 0) ? n.substring(i + 1) : "";
    }

    private static Set<String> unmodifiable(String... s) {
        return Collections.unmodifiableSet(new HashSet<>(Arrays.asList(s)));
    }
}
