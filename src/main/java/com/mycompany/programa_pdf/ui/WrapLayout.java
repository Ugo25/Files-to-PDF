package com.mycompany.programa_pdf.ui;

import java.awt.*;

public class WrapLayout extends FlowLayout {
    public WrapLayout(int align, int hgap, int vgap) { super(align, hgap, vgap); }

    @Override public Dimension preferredLayoutSize(Container target) { return layoutSize(target, true); }
    @Override public Dimension minimumLayoutSize(Container target)   {
        Dimension minimum = layoutSize(target, false);
        minimum.width -= (getHgap() + 1); return minimum;
    }

    private Dimension layoutSize(Container target, boolean preferred) {
        synchronized (target.getTreeLock()) {
            int targetWidth = target.getSize().width;
            if (targetWidth == 0) {
                Container parent = target.getParent();
                if (parent != null) targetWidth = parent.getSize().width;
            }
            int hgap = getHgap(), vgap = getVgap();
            Insets in = target.getInsets();
            int maxWidth = targetWidth > 0 ? targetWidth - (in.left + in.right + hgap * 2) : Integer.MAX_VALUE;

            Dimension dim = new Dimension(0, 0);
            int rowW = 0, rowH = 0;

            int n = target.getComponentCount();
            for (int i = 0; i < n; i++) {
                Component m = target.getComponent(i);
                if (!m.isVisible()) continue;
                Dimension d = preferred ? m.getPreferredSize() : m.getMinimumSize();

                if (rowW + d.width > maxWidth) {
                    dim.width = Math.max(dim.width, rowW);
                    dim.height += rowH == 0 ? 0 : vgap;
                    dim.height += rowH;
                    rowW = 0; rowH = 0;
                }
                if (rowW != 0) rowW += hgap;
                rowW += d.width;
                rowH = Math.max(rowH, d.height);
            }
            dim.width = Math.max(dim.width, rowW);
            dim.height += (rowH == 0 ? 0 : vgap) + rowH;

            dim.width  += in.left + in.right + hgap * 2;
            dim.height += in.top  + in.bottom + vgap * 2;

            if (targetWidth > 0) dim.width = Math.max(dim.width, targetWidth);
            return dim;
        }
    }
}
