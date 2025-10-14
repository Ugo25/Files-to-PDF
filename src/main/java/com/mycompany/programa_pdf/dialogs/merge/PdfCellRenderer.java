package com.mycompany.programa_pdf.dialogs.merge;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.File;

/** Renderer de la lista: miniatura + nombre + “N pág.” */
public class PdfCellRenderer extends JPanel implements ListCellRenderer<File> {
    private final JLabel thumb = new JLabel();
    private final JLabel name  = new JLabel();
    private final JLabel sub   = new JLabel();
    private final PdfPreviewer previewer;

    public PdfCellRenderer(PdfPreviewer previewer) {
        this.previewer = previewer;

        setOpaque(true);
        setLayout(new BorderLayout(8, 6));
        setBorder(new EmptyBorder(6, 6, 6, 6));

        thumb.setPreferredSize(new Dimension(64, 64));
        name.setFont(name.getFont().deriveFont(Font.BOLD, 12f));
        sub.setFont(sub.getFont().deriveFont(Font.PLAIN, 11f));
        sub.setForeground(new Color(130, 130, 130));

        JPanel center = new JPanel();
        center.setOpaque(false);
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        center.add(name);
        center.add(Box.createVerticalStrut(2));
        center.add(sub);

        add(thumb, BorderLayout.WEST);
        add(center, BorderLayout.CENTER);
    }

    @Override
    public Component getListCellRendererComponent(JList<? extends File> list, File value, int index, boolean isSelected, boolean cellHasFocus) {
        if (value != null) {
            name.setText(value.getName());
            try {
                int p = previewer.getPageCount(value);
                sub.setText(p + " pág.");
                Image t = previewer.getThumb(value);
                thumb.setIcon(t != null ? new ImageIcon(t) : null);
            } catch (Exception e) {
                sub.setText("—");
                thumb.setIcon(null);
            }
        } else {
            name.setText("");
            sub.setText("");
            thumb.setIcon(null);
        }

        if (isSelected) {
            setBackground(list.getSelectionBackground());
            setForeground(list.getSelectionForeground());
        } else {
            setBackground(list.getBackground());
            setForeground(list.getForeground());
        }
        return this;
    }
}
