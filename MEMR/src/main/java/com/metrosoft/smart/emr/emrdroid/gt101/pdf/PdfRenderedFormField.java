package com.metrosoft.smart.emr.emrdroid.gt101.pdf;

import android.graphics.Color;
import android.graphics.RectF;

/**
 * PDF 안의 AcroForm text field를 화면에 다시 그리기 위한 모델
 */
public class PdfRenderedFormField {
    public int pageIndex;
    public String name; // PDF 폴 필드 이믈
    public String ccfField; // 원래 ccf_field
    public String value;
    public String type;      // 추가
    public float fontSizePdf;
    public int colorArgb;
    public RectF pdfRect;
    public boolean readOnly = false;
    public String groupName; // radio 버튼 그룹용도

    public boolean isValid() {
        return pdfRect != null;
    }
}