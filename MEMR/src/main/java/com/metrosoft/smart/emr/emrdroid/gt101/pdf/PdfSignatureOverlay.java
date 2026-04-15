package com.metrosoft.smart.emr.emrdroid.gt101.pdf;

import android.graphics.Bitmap;
import android.graphics.RectF;

public class PdfSignatureOverlay {
    public Bitmap bitmap;
    public RectF pdfRect = new RectF();
    public boolean visible = false;

    public PdfSignatureOverlay copyShallow() {
        PdfSignatureOverlay copied = new PdfSignatureOverlay();
        copied.bitmap = this.bitmap;
        copied.pdfRect = new RectF(this.pdfRect);
        copied.visible = this.visible;
        return copied;
    }
}