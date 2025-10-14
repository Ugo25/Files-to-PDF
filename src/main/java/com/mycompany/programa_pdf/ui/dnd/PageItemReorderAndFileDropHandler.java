package com.mycompany.programa_pdf.ui.dnd;

import com.mycompany.programa_pdf.dialogs.images.PageItem;
import com.mycompany.programa_pdf.io.FileTypeUtils;

import javax.swing.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/** TransferHandler para reordenamiento interno + drop de imágenes externas. */
public class PageItemReorderAndFileDropHandler extends TransferHandler {

    private final JList<PageItem> list;
    private final DefaultListModel<PageItem> model;
    private int[] dragIndices = null;

    public PageItemReorderAndFileDropHandler(JList<PageItem> list, DefaultListModel<PageItem> model) {
        this.list = list; this.model = model;
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
            // 1) Archivos externos
            if (s.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                List<File> files = (List<File>) s.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
                for (File f : files) if (f != null && f.isFile() && FileTypeUtils.isImage(f)) model.addElement(new PageItem(f));
                return true;
            }
            // 2) Reordenamiento interno o text/uri-list
            if (s.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                String txt = (String) s.getTransferable().getTransferData(DataFlavor.stringFlavor);

                // Reordenamiento interno
                if (dragIndices != null) {
                    JList.DropLocation dl = (JList.DropLocation) s.getDropLocation();
                    int dropIndex = Math.max(0, dl.getIndex());

                    List<PageItem> dragged = new ArrayList<>();
                    for (int idx : dragIndices) dragged.add(model.get(idx));
                    for (int i = dragIndices.length - 1; i >= 0; i--) model.remove(dragIndices[i]);

                    int insertAt = Math.min(dropIndex, model.size());
                    for (PageItem t : dragged) model.add(insertAt++, t);

                    int[] newSel = new int[dragged.size()];
                    for (int i = 0; i < dragged.size(); i++) newSel[i] = dropIndex + i;
                    list.setSelectedIndices(newSel);
                    list.ensureIndexIsVisible(newSel[0]);
                    return true;
                }

                // URIs externas (algunos exploradores)
                List<File> uris = FileTypeUtils.parseUriList(txt);
                if (!uris.isEmpty()) {
                    for (File f : uris) if (f != null && f.isFile() && FileTypeUtils.isImage(f)) model.addElement(new PageItem(f));
                    return true;
                }
            }
            return false;
        } catch (Exception ignore) {
            return false;
        } finally {
            dragIndices = null;
        }
    }
}
