package com.mycompany.programa_pdf.images;

import java.io.File;
import java.awt.image.BufferedImage;

public class ImageEntry {
    public final File file;
    public int rotation = 0;
    public BufferedImage thumb, thumbRot;

    public ImageEntry(File f) { this.file = f; }

    public void rotate(int delta) {
        rotation = ((rotation + delta) % 360 + 360) % 360;
        thumbRot = null;
    }

    @Override public String toString() { return file.getName(); }
}
