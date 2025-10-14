package com.mycompany.programa_pdf.state;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/** Historial simple persistido en TSV: fecha | accion | entradas(semi) | salida */
public class HistoryStore {
    public static record Entry(String dateIso, String action, List<String> inputs, String output) {}

    private final Path dir = Path.of(System.getProperty("user.home"), ".programa_pdf");
    private final Path file = dir.resolve("history.tsv");
    private static final DateTimeFormatter FMT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    public HistoryStore() {
        try { if (!Files.exists(dir)) Files.createDirectories(dir); }
        catch (IOException ignored) {}
    }

    public synchronized void add(String action, List<String> inputs, String output) {
        String date = LocalDateTime.now().format(FMT);
        String inJoined = String.join(";", inputs);
        String line = escape(date) + "\t" + escape(action) + "\t" + escape(inJoined) + "\t" + escape(output) + "\n";
        try {
            Files.writeString(file, line, StandardCharsets.UTF_8,
                    Files.exists(file) ? StandardOpenOption.APPEND : StandardOpenOption.CREATE);
        } catch (IOException ignored) {}
    }

    public synchronized List<Entry> loadAll() {
        if (!Files.exists(file)) return new ArrayList<>();
        List<Entry> list = new ArrayList<>();
        try (BufferedReader br = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            String s;
            while ((s = br.readLine()) != null) {
                String[] p = split4(s);
                String date = unescape(p[0]);
                String action = unescape(p[1]);
                String inputs = unescape(p[2]);
                String output = unescape(p[3]);
                list.add(new Entry(date, action, inputs.isBlank() ? List.of() : Arrays.asList(inputs.split(";")), output));
            }
        } catch (IOException ignored) {}
        return list;
    }

    public synchronized void clearAll() {
        try { Files.deleteIfExists(file); } catch (IOException ignored) {}
    }

    public synchronized void removeAt(int index) {
        List<Entry> all = loadAll();
        if (index < 0 || index >= all.size()) return;
        all.remove(index);
        rewrite(all);
    }

    private void rewrite(List<Entry> entries) {
        try (BufferedWriter bw = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            for (Entry e : entries) {
                String line = escape(e.dateIso()) + "\t" + escape(e.action()) + "\t" +
                        escape(String.join(";", e.inputs())) + "\t" + escape(e.output()) + "\n";
                bw.write(line);
            }
        } catch (IOException ignored) {}
    }

    private static String escape(String s) { return s.replace("\\", "\\\\").replace("\t", "\\t").replace("\n", "\\n"); }
    private static String unescape(String s) { return s.replace("\\t", "\t").replace("\\n", "\n").replace("\\\\", "\\"); }

    private static String[] split4(String s) {
        List<String> out = new ArrayList<>(4);
        StringBuilder cur = new StringBuilder();
        boolean esc = false;
        for (char c : s.toCharArray()) {
            if (esc) { cur.append(c=='t'?'\t':(c=='n'?'\n':c)); esc = false; continue; }
            if (c == '\\') { esc = true; continue; }
            if (c == '\t') { out.add(cur.toString()); cur.setLength(0); continue; }
            cur.append(c);
        }
        out.add(cur.toString());
        while (out.size() < 4) out.add("");
        return out.toArray(new String[0]);
    }
}
