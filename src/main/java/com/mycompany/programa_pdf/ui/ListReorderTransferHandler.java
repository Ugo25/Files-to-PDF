package com.mycompany.programa_pdf.ui;

import javax.swing.*;
import javax.swing.TransferHandler.TransferSupport;
import java.awt.datatransfer.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * TransferHandler para reordenar elementos de un JList mediante drag & drop.
 * Sin javax/jakarta.activation. Solo usa java.awt.datatransfer.
 *
 * Uso:
 *   JList<MyPage> list = ...;
 *   list.setDragEnabled(true);
 *   list.setDropMode(DropMode.INSERT);
 *   list.setTransferHandler(new ListReorderTransferHandler<>(list));
 */
public class ListReorderTransferHandler<T> extends TransferHandler {

    private final JList<T> list;
    private final DataFlavor localObjectFlavor;

    private int[] indices = null; // indices seleccionados al empezar el drag
    private int addIndex  = -1;   // índice donde se insertó en el drop
    private int addCount  = 0;    // cuántos elementos se insertaron

    public ListReorderTransferHandler(JList<T> list) {
        this.list = list;
        // Flavor local para pasar una List<?> dentro del mismo proceso/VM
        try {
            this.localObjectFlavor =
                new DataFlavor(DataFlavor.javaJVMLocalObjectMimeType + ";class=java.util.List");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    // ---- arranque del drag ----
    @Override
    protected Transferable createTransferable(JComponent c) {
        indices = list.getSelectedIndices();
        final List<T> selected = list.getSelectedValuesList();

        // Transferable sin DataHandler:
        return new Transferable() {
            @Override
            public DataFlavor[] getTransferDataFlavors() {
                return new DataFlavor[]{ localObjectFlavor };
            }
            @Override
            public boolean isDataFlavorSupported(DataFlavor flavor) {
                return localObjectFlavor.equals(flavor);
            }
            @SuppressWarnings("unchecked")
            @Override
            public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
                if (!isDataFlavorSupported(flavor)) throw new UnsupportedFlavorException(flavor);
                return selected;
            }
        };
    }

    @Override
    public int getSourceActions(JComponent c) {
        return MOVE;
    }

    // ---- validación del drop ----
    @Override
    public boolean canImport(TransferSupport info) {
        return info.isDrop() && info.isDataFlavorSupported(localObjectFlavor)
               && list.getModel() instanceof DefaultListModel;
    }

    // ---- realizar el drop ----
    @SuppressWarnings("unchecked")
    @Override
    public boolean importData(TransferSupport info) {
        if (!canImport(info)) return false;

        JList.DropLocation dl = (JList.DropLocation) info.getDropLocation();
        int index = dl.getIndex();

        List<T> data;
        try {
            data = (List<T>) info.getTransferable().getTransferData(localObjectFlavor);
        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }

        DefaultListModel<T> model = (DefaultListModel<T>) list.getModel();

        // Ajuste si se arrastra hacia adelante dentro de la misma lista
        int max = model.getSize();
        index = Math.max(0, Math.min(index, max));

        addIndex = index;
        addCount = data.size();

        // Insertar copias en el modelo
        for (T value : data) {
            model.add(index++, value);
        }
        return true;
    }

    // ---- al terminar el DnD (borrar originales si fue MOVE) ----
    @Override
    protected void exportDone(JComponent c, Transferable t, int action) {
        if (action != MOVE || indices == null) {
            indices = null; addIndex = -1; addCount = 0;
            return;
        }
        if (!(list.getModel() instanceof DefaultListModel)) {
            indices = null; addIndex = -1; addCount = 0;
            return;
        }

        DefaultListModel<?> model = (DefaultListModel<?>) list.getModel();

        // Si insertaste después del origen, corrige desplazamiento
        if (addCount > 0 && addIndex <= indices[0]) {
            for (int i = 0; i < indices.length; i++) {
                indices[i] += addCount;
            }
        }

        // Borra los originales empezando desde el final
        for (int i = indices.length - 1; i >= 0; i--) {
            model.remove(indices[i]);
        }

        indices = null; addIndex = -1; addCount = 0;
    }
}
