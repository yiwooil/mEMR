package com.metrosoft.smart.emr.emrdroid.gt101.pdf;

import android.graphics.Bitmap;

public class PdfSignatureSpec {
    public int pageIndex;
    public Bitmap bitmap;
    public float x;
    public float y;
    public float width;
    public float height;

    public PdfSignatureSpec() {
    }

    public PdfSignatureSpec(int pageIndex, Bitmap bitmap,
                            float x, float y, float width, float height) {
        this.pageIndex = pageIndex;
        this.bitmap = bitmap;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }
}