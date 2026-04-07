package com.metrosoft.smart.emr.emrdroid.gt101.data;

import java.util.ArrayList;
import java.util.List;

public class CcfValues {
	private List<CcfValue> m_List = null;
	
	public CcfValues(){
		m_List = new ArrayList<CcfValue>();
		m_List.clear();
	}
	
	public void clear(){
		m_List.clear();
	}
	
	public void addCcfValue(String field, float x, float y, float w, float h, boolean autoFit, String value){
		m_List.add(new CcfValue(field, x, y, w, h, autoFit, value));
	}

	public int getCount(){
		return m_List.size();
	}

	public String getField(int idx) { return m_List.get(idx).getField(); }
	
	public float getX(int idx){
		return m_List.get(idx).getX();
	}
	
	public float getY(int idx){
		return m_List.get(idx).getY();
	}
	
	public float getW(int idx){
		return m_List.get(idx).getW();
	}
	
	public float getH(int idx){
		return m_List.get(idx).getH();
	}

	public boolean getAutoFit(int idx){
		return m_List.get(idx).getAutoFit();
	}

	public String getValue(int idx){
		return m_List.get(idx).getValue();
	}

	public void putValue(int idx, String value) { m_List.get(idx).putValue(value); }

	public CcfValue getCcfValue(int idx) { return m_List.get(idx); }
}
