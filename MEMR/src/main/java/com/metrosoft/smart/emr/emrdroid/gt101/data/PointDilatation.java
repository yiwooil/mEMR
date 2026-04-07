package com.metrosoft.smart.emr.emrdroid.gt101.data;

public class PointDilatation {
    private int idx;
    private String exdt;
    private String extm;
    private float value;

    public PointDilatation(int idx, String exdt, String extm, float value) {
        this.idx = idx;
        this.exdt = exdt;
        this.extm = extm;
        this.value = value;
    }

    public int getIdx() {
        return this.idx;
    }
    public String getExdt() {
        return this.exdt;
    }

    public String getExtm() {
        return this.extm;
    }

    public float getValue() {
        return this.value;
    }
}
