package com.metrosoft.smart.emr.emrdroid.gt101.pdf;

public class PdfFormTextFieldSpec {
    public int pageIndex;
    public String fieldName;
    public float x;
    public float y;
    public float width;
    public float height;
    public String defaultValue;
    public int fontSize = 10;

    public PdfFormTextFieldSpec() {
    }

    public PdfFormTextFieldSpec(int pageIndex, String fieldName,
                                float x, float y, float width, float height,
                                String defaultValue, int fontSize) {
        this.pageIndex = pageIndex;
        this.fieldName = fieldName;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.defaultValue = defaultValue;
        this.fontSize = fontSize;
    }
}