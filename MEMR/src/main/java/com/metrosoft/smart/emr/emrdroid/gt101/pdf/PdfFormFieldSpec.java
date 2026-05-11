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

    // 저장 후 다시 열었을 때도 sign 입력 가능 영역을 복원하기 위한 값
    public boolean pendingSign;

    // 저장 후 수정 가능 여부.
    // text/label/checkbox/radio/sign_image는 저장 후 false,
    // sign은 미서명 상태이면 true로 유지 가능
    public boolean editable;


    public PdfFormFieldSpec() {
    }

}