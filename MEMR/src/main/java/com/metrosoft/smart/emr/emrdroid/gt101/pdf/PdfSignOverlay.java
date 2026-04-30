package com.metrosoft.smart.emr.emrdroid.gt101.pdf;

import android.graphics.Bitmap;
import android.graphics.RectF;

public class PdfSignOverlay {
    public Bitmap bitmap;
    public RectF pdfRect = new RectF();
    public boolean visible = false;

    public boolean autoImageSign = false; // true이면 sign_AA10011 같은 자동 이미지 사인

    public PdfSignOverlay copyShallow() {
        PdfSignOverlay copied = new PdfSignOverlay();
        copied.bitmap = this.bitmap;
        copied.pdfRect = new RectF(this.pdfRect);
        copied.visible = this.visible;
        copied.autoImageSign = this.autoImageSign;
        return copied;
    }
}