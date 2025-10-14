package com.mycompany.programa_pdf.ui.dnd;

import com.mycompany.programa_pdf.images.ImageEntry;

import javax.swing.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.util.*;
import java.io.File;

import static com.mycompany.programa_pdf.io.FileTypeUtils.isImage;

public class ImagesReorderHandler extends TransferHandler {
    private final JList<ImageEntry> list;
    private final DefaultListModel<ImageEntry> model;
    private int[] dragIndices;

    public ImagesReorderHandler(JList<ImageEntry> list, DefaultListModel<ImageEntry> model) {
        this.list = list; this.model = model;
    }

    @Override public int getSourceActions(JComponent c) { return MOVE; }

    @Override protected java.awt.datatransfer.Transferable createTransferable(JComponent c) {
        dragIndices = list.getSelectedIndices();
        return new StringSelection("reorder");
    }

    @Override public boolean canImport(TransferSupport s) {
        if (s.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) return true;
        s.setDropAction(MOVE);
        return s.isDrop() && s.isDataFlavorSupported(DataFlavor.stringFlavor);
    }

    @SuppressWarnings("unchecked")
    @Override public boolean importData(TransferSupport s) {
        try {
            if (s.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                for (File f : (List<File>) s.getTransferable().getTransferData(DataFlavor.javaFileListFlavor))
                    if (f != null && f.isFile() && isImage(f)) model.addElement(new ImageEntry(f));
                return true;
            }
            if (s.isDataFlavorSupported(DataFlavor.stringFlavor) && dragIndices != null) {
                JList.DropLocation dl = (JList.DropLocation) s.getDropLocation();
                int dropIndex = Math.max(0, dl.getIndex());

                List<ImageEntry> dragged = new ArrayList<>();
                for (int idx : dragIndices) dragged.add(model.get(idx));

                int removedBefore = (int) Arrays.stream(dragIndices).filter(i -> i < dropIndex).count();
                for (int i = dragIndices.length - 1; i >= 0; i--) model.remove(dragIndices[i]);

                int insertAt = Math.min(dropIndex - removedBefore, model.size());
                for (ImageEntry t : dragged) model.add(insertAt++, t);

                int base = dropIndex - removedBefore;
                int[] newSel = new int[dragged.size()];
                for (int i = 0; i < dragged.size(); i++) newSel[i] = base + i;
                list.setSelectedIndices(newSel);
                list.ensureIndexIsVisible(newSel[0]);
                return true;
            }
            return false;
        } catch (Exception ignored) {
            return false;
        } finally {
            dragIndices = null;
        }
    }
}
