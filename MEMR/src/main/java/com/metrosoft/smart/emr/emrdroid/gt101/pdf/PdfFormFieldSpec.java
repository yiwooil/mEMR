package com.metrosoft.smart.emr.emrdroid.gt101.pdf;

public class PdfFormFieldSpec {
    public int pageNo;
    public String fieldName;
    public String ccfField;
    public float x;
    public float y;
    public float width;
    public float height;
    public String defaultValue;
    public int fontSize = 10;
    public String typeName;
    public String value;

    public String groupName; // 2026.05.06 WOOIL - radio 버튼용

    public PdfFormFieldSpec() {
    }

}