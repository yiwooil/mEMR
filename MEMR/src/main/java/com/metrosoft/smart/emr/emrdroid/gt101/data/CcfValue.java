package com.metrosoft.smart.emr.emrdroid.gt101.data;

public class CcfValue {
	private String field;
	private float x;
	private float y;
	private float w;
	private float h;
	private boolean autoFit; // 2024.04.26 WOOIL
	private String value;
	
	public CcfValue(String field, float x, float y, float w, float h, boolean autoFit, String value){
		this.field = field;
		this.x = x;
		this.y = y;
		this.w = w;
		this.h = h;
		this.autoFit = autoFit;
		this.value = value;
	}

	public String getField(){ return this.field; }

	public float getX(){
		return this.x;
	}
	
	public float getY(){
		return this.y;
	}
	
	public float getH(){
		return this.h;
	}
	
	public float getW(){
		return this.w;
	}

	public boolean getAutoFit() { return this.autoFit; } // 2024.04.26 WOOIL

	public String getValue(){
		return this.value;
	}

	public void putValue(String value) { this.value = value; }
}
