package com.metrosoft.smart.emr.emrdroid.gt101.pdf;

public class PdfFormTextFieldSpec {
    public int pageNo;
    public String fieldName;
    public float x;
    public float y;
    public float width;
    public float height;
    public String defaultValue;
    public int fontSize = 10;
    public String typeName;
    public boolean readOnly;
    public String value;

    public PdfFormTextFieldSpec() {
    }

    public PdfFormTextFieldSpec(int pageNo, String fieldName,
                                float x, float y, float width, float height,
                                String defaultValue, int fontSize) {
        this.pageNo = pageNo;
        this.fieldName = fieldName;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.defaultValue = defaultValue;
        this.fontSize = fontSize;
        this.typeName = "";
        this.readOnly = false;
        this.value = "";
    }
}