package com.mycompany.programa_pdf.dialogs.merge;

import javax.swing.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** DnD para la lista: reordenamiento interno + drop de PDFs externos. */
public class PdfListTransferHandler extends TransferHandler {
    private final JList<File> list;
    private final DefaultListModel<File> model;
    private int[] dragIndices = null;

    public PdfListTransferHandler(JList<File> list, DefaultListModel<File> model) {
        this.list = list;
        this.model = model;
    }

    @Override public int getSourceActions(JComponent c) { return MOVE; }

    @Override protected Transferable createTransferable(JComponent c) {
        dragIndices = list.getSelectedIndices();
        return new java.awt.datatransfer.StringSelection("reorder");
    }

    @Override public boolean canImport(TransferSupport s) {
        if (s.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) return true;
        s.setDropAction(MOVE);
        return s.isDrop() && s.isDataFlavorSupported(DataFlavor.stringFlavor);
    }

    @SuppressWarnings("unchecked")
    @Override public boolean importData(TransferSupport s) {
        try {
            // (1) Drop de archivos externos
            if (s.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                List<File> files = (List<File>) s.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
                for (File f : files) if (isPdf(f)) model.addElement(f);
                return true;
            }
            // (2) Reordenamiento interno (usamos stringFlavor como señal)
            if (s.isDataFlavorSupported(DataFlavor.stringFlavor) && dragIndices != null) {
                JList.DropLocation dl = (JList.DropLocation) s.getDropLocation();
                int dropIndex = Math.max(0, dl.getIndex());

                List<File> dragged = new ArrayList<>();
                for (int idx : dragIndices) dragged.add(model.get(idx));
                for (int i = dragIndices.length - 1; i >= 0; i--) model.remove(dragIndices[i]);

                int insertAt = Math.min(dropIndex, model.size());
                for (File t : dragged) model.add(insertAt++, t);

                int[] newSel = new int[dragged.size()];
                for (int i = 0; i < dragged.size(); i++) newSel[i] = dropIndex + i;
                list.setSelectedIndices(newSel);
                list.ensureIndexIsVisible(newSel[0]);
                return true;
            }
            return false;
        } catch (Exception ignore) {
            return false;
        } finally {
            dragIndices = null;
        }
    }

    private static boolean isPdf(File f) {
        if (f == null || !f.isFile()) return false;
        String n = f.getName().toLowerCase(Locale.ROOT);
        return n.endsWith(".pdf");
    }
}

