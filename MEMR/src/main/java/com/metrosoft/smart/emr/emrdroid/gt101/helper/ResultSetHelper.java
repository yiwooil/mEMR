package com.metrosoft.smart.emr.emrdroid.gt101.helper;

import org.json.*;

import com.metrosoft.smart.emr.emrdroid.gt101.utils.EmrSettingsUtil;

import android.content.Context;
import android.util.Log;

public class ResultSetHelper {
	final static int MASKING_DEFAULT=0;
	final static int MASKING_NONE=1;
	final static int MASKING_FORCE=2;
	
	private JSONArray main;
	private JSONArray control;
	private JSONArray data;
	private int returnCode;
	private String returnDesc;
	private boolean masking;
	
	public ResultSetHelper(String jsonString,boolean masking) throws JSONException {
		this.masking=masking;
		if("".equalsIgnoreCase(jsonString)){
			this.main = null;
			this.control = null;
			this.returnCode = 0;
			this.returnDesc = "";
			this.data=null;
		}else{
			this.main = new JSONArray(jsonString);
			this.control = main.getJSONArray(0);
			this.returnCode = control.getJSONObject(0).getInt("return_code");
			this.returnDesc = control.getJSONObject(0).getString("return_desc");
			if (this.returnCode>0) {
				this.data = main.getJSONArray(1);
			}
			else {
				this.data=null;
			}
		}
	}
//	public ResultSetHelper(String jsonString) throws JSONException {
//		this(jsonString,true);
//	}
	
	public int getReturnCode() {
		return returnCode;
	}
	
	public String getReturnDesc() {
		return returnDesc;		
	}
	
	public int getRecordCount() {
		return data==null ? 0 : data.length();
	}
	
	public boolean getBoolean(int index,String key) throws JSONException {
		return data.getJSONObject(index).getBoolean(key);
	}
	
	public double getDouble(int index,String key) throws JSONException {
		return data.getJSONObject(index).getDouble(key);
	}

	public int getInt(int index,String key) throws JSONException {
		return data.getJSONObject(index).getInt(key);
	}
	
	public long getLong(int index,String key) throws JSONException {
		return data.getJSONObject(index).getLong(key);
	}

	public String getString(int index,String key) throws JSONException {
		return getString(index,key,ResultSetHelper.MASKING_DEFAULT);
	}
	
	public String getString(int index,String key,int maskAction) throws JSONException {
		boolean bMasking=false;
		String returnString="";
		if (maskAction==ResultSetHelper.MASKING_FORCE) {
			bMasking=true;
		}
		else if (maskAction==ResultSetHelper.MASKING_NONE) {
			bMasking=false;
		}
		else {
			bMasking=this.masking;
		}
		returnString=data.getJSONObject(index).getString(key);
		// 2013.08.08 WOOIL - null 이면 빈문자열로 변환
		//                    oracle은 빈문자열을 null로 처리한다.
		if(returnString.equals("null")) returnString="";
		if (bMasking==true) {
			if (key.equals("pnm")) {
				returnString+="  "; // String index out of range: 2 오류 방지용
				returnString=returnString.substring(0, 2) + "*";
			}
		}
		return returnString;
	}
	
	public static String concateResultSet(String jsonString1, String jsonString2) throws JSONException {
		JSONArray newResultSet = new JSONArray();
		JSONArray newControl = new JSONArray();
		JSONArray newData = new JSONArray();
		
		// jsonString1 처리
		JSONArray main1 = new JSONArray(jsonString1);
		//int mainCount1=main1.length();
		JSONArray control1 = main1.getJSONArray(0);
		int count1=control1.getJSONObject(0).getInt("return_code");
		JSONArray data1 = main1.getJSONArray(1);
		for(int i=0;i<count1;i++){
			newData.put(data1.getJSONObject(i));
		}
		
		// jsonString2 처리
		JSONArray main2 = new JSONArray(jsonString2);
		//int mainCount2=main2.length();
		JSONArray control2 = main2.getJSONArray(0);
		int count2=control2.getJSONObject(0).getInt("return_code");
		JSONArray data2 = main2.getJSONArray(1);
		for(int i=0;i<count2;i++){
			newData.put(data2.getJSONObject(i));
		}
		
		//Log.d("EmrDroid","count1=" + count1 + ", count2=" + count2);
		
		// 건수
		JSONObject returnDesc = new JSONObject();
		returnDesc.put("return_desc", "ok");
		returnDesc.put("return_code", count1 + count2);
		newControl.put(returnDesc);
		// result set
		newResultSet.put(newControl);
		newResultSet.put(newData);
		
		return newResultSet.toString();
	}
	
	public static int getRecordCount(String jsonString) throws JSONException{
		JSONArray main = new JSONArray(jsonString);
		JSONArray control = main.getJSONArray(0);
		int count=control.getJSONObject(0).getInt("return_code");
		return count;
	}
}
