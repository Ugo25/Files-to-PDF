package com.mycompany.programa_pdf.dialogs;

import com.mycompany.programa_pdf.state.HistoryStore;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.List;

public class HistoryDialog extends JDialog {

    // ======= Estado =======
    private final HistoryStore history;

    // ======= UI =======
    private JTable table;
    private DefaultTableModel model;
    private JButton btnAbrir, btnCarpeta, btnEliminar, btnVaciar, btnCopiar, btnCerrar;

    public HistoryDialog(Window owner, HistoryStore history) {
        super(owner, "Historial de archivos", ModalityType.DOCUMENT_MODAL);
        this.history = history;

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setMinimumSize(new Dimension(900, 520));
        setLocationRelativeTo(owner);

        buildUI();
        loadData();
        wireEvents();
    }

    // ================== UI ==================
    private void buildUI() {
        JPanel root = new JPanel(new BorderLayout(10, 10));
        root.setBorder(new EmptyBorder(10, 10, 10, 10));
        setContentPane(root);

        String[] cols = {"Fecha", "Acción", "Entradas", "Salida"};
        model = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(model);
        table.setRowHeight(22);
        table.setAutoCreateRowSorter(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // Columnas un poco más anchas para paths
        table.getColumnModel().getColumn(0).setPreferredWidth(130);
        table.getColumnModel().getColumn(1).setPreferredWidth(180);
        table.getColumnModel().getColumn(2).setPreferredWidth(400);
        table.getColumnModel().getColumn(3).setPreferredWidth(400);

        root.add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 6));
        btnAbrir    = new JButton("Abrir");
        btnCarpeta  = new JButton("Mostrar en carpeta");
        btnEliminar = new JButton("Eliminar seleccionado");
        btnVaciar   = new JButton("Vaciar historial");
        btnCopiar   = new JButton("Copiar ruta(s)");
        btnCerrar   = new JButton("Cerrar");

        buttons.add(btnAbrir);
        buttons.add(btnCarpeta);
        buttons.add(btnCopiar);
        buttons.add(btnEliminar);
        buttons.add(btnVaciar);
        buttons.add(btnCerrar);
        root.add(buttons, BorderLayout.SOUTH);

        // UX
        getRootPane().setDefaultButton(btnAbrir);
        getRootPane().registerKeyboardAction(
                e -> dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW
        );

        setButtonsEnabled(false);
    }

    // =============== Datos ===============
    private void loadData() {
        model.setRowCount(0);
        List<HistoryStore.Entry> all = history.loadAll();
        for (HistoryStore.Entry e : all) {
            model.addRow(new Object[]{
                    e.dateIso(),
                    e.action(),
                    String.join(" ; ", e.inputs()),
                    e.output()
            });
        }
        if (model.getRowCount() > 0) table.setRowSelectionInterval(0, 0);
        setButtonsEnabled(table.getSelectedRow() >= 0);
    }

    // =============== Eventos ===============
    private void wireEvents() {
        btnAbrir.addActionListener(e -> abrirSalidaSeleccionada(false));
        btnCarpeta.addActionListener(e -> abrirSalidaSeleccionada(true));
        btnEliminar.addActionListener(e -> eliminarSeleccionado());
        btnVaciar.addActionListener(e -> vaciarHistorial());
        btnCopiar.addActionListener(e -> copiarRutas());
        btnCerrar.addActionListener(e -> dispose());

        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) setButtonsEnabled(table.getSelectedRow() >= 0);
        });

        table.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) abrirSalidaSeleccionada(false);
            }
        });

        table.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "open");
        table.getActionMap().put("open", new AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) {
                abrirSalidaSeleccionada(false);
            }
        });
    }

    private void setButtonsEnabled(boolean hasSel) {
        btnAbrir.setEnabled(hasSel);
        btnCarpeta.setEnabled(hasSel);
        btnEliminar.setEnabled(hasSel);
        btnCopiar.setEnabled(hasSel);
    }

    // =============== Acciones ===============
    private void abrirSalidaSeleccionada(boolean abrirCarpeta) {
        int rView = table.getSelectedRow();
        if (rView < 0) return;
        int r = table.convertRowIndexToModel(rView);

        String outPath = asString(model.getValueAt(r, 3));
        if (outPath.isBlank()) {
            JOptionPane.showMessageDialog(this, "No hay salida asociada.", "Aviso",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        File f = new File(outPath);
        if (!f.exists()) {
            JOptionPane.showMessageDialog(this, "No se encuentra:\n" + f.getAbsolutePath(),
                    "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (!Desktop.isDesktopSupported()) return;

        try {
            if (abrirCarpeta) {
                Desktop.getDesktop().open(f.getParentFile());
            } else {
                Desktop.getDesktop().open(f);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "No se pudo abrir:\n" + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void eliminarSeleccionado() {
        int rView = table.getSelectedRow();
        if (rView < 0) return;
        int r = table.convertRowIndexToModel(rView);

        int ans = JOptionPane.showConfirmDialog(
                this,
                "¿Eliminar este registro del historial?",
                "Confirmar",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );
        if (ans == JOptionPane.YES_OPTION) {
            history.removeAt(r);
            loadData();
        }
    }

    private void vaciarHistorial() {
        int ans = JOptionPane.showConfirmDialog(
                this,
                "¿Vaciar todo el historial?",
                "Confirmar",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );
        if (ans == JOptionPane.YES_OPTION) {
            history.clearAll();
            loadData();
        }
    }

    private void copiarRutas() {
        int rView = table.getSelectedRow();
        if (rView < 0) return;
        int r = table.convertRowIndexToModel(rView);

        String entradas = asString(model.getValueAt(r, 2));
        String salida   = asString(model.getValueAt(r, 3));
        String text = (entradas.isBlank() ? "" : entradas.replace(" ; ", "\n"))
                    + (salida.isBlank() ? "" : (entradas.isBlank() ? "" : "\n") + salida);

        Toolkit.getDefaultToolkit().getSystemClipboard()
                .setContents(new StringSelection(text), null);
    }

    // =============== Utils ===============
    private static String asString(Object o) { return o == null ? "" : o.toString(); }
}
