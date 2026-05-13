package com.metrosoft.smart.emr.emrdroid.gt101.pdf;

import android.graphics.Bitmap;
import android.graphics.Path;
import android.graphics.RectF;

import java.util.ArrayList;
import java.util.List;

public class PdfSignOverlay {

    // 벡터 싸인
    public List<Path> paths = new ArrayList<Path>();

    public RectF pdfRect = new RectF();

    public boolean visible = false;

    // pen 정보
    public float strokeWidth = 5f;
    public int strokeColor = 0xFF000000;

    // 어떤 필드인지
    public String fieldName;
    public String ccfField;
    public String groupName;

    // 싸인 필요 여부
    public boolean pendingSign = false;

    public PdfSignOverlay copyShallow() {
        PdfSignOverlay copied = new PdfSignOverlay();

        copied.paths = new ArrayList<Path>(this.paths);

        copied.pdfRect = new RectF(this.pdfRect);
        copied.visible = this.visible;

        copied.strokeWidth = this.strokeWidth;
        copied.strokeColor = this.strokeColor;

        copied.fieldName = this.fieldName;
        copied.ccfField = this.ccfField;
        copied.groupName = this.groupName;

        copied.pendingSign = this.pendingSign;

        return copied;
    }
}