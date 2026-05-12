package com.metrosoft.smart.emr.emrdroid.gt101.pdf;

import android.graphics.RectF;

/**
 * PDF 표시/편집용 overlay field 모델
 */
public class PdfRenderedFormField {
    public int pageIndex;

    public String name;
    public String ccfField;
    public String value;
    public String type;
    public boolean autoFit = false;

    public float fontSizePdf;
    public int colorArgb;

    public RectF pdfRect;

    public String groupName;

    // 저장된 PDF만 다시 열었을 때, 아직 서명받아야 하는 sign 영역인지 여부
    public boolean pendingSign;
    // 저장 후에도 사용자가 수정 가능한지 여부
    public boolean editable;

    public boolean isValid() {
        return pdfRect != null;
    }
}