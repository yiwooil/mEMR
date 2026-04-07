package com.metrosoft.smart.emr.emrdroid.gt101.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class EmrSettingsUtil {
	
	public static final String PACKAGE_MEMR = "com.metrosoft.smart.emr.emrdroid.gt101";
	public static final String PACKAGE_TEMR = "com.metrosoft.temr";
	//public static final String PACKAGE_MEMRPHONE = "com.metrosoft.memrphone";
	//public static final String PACKAGE_TEMRDEV = "com.metrosoft.temrdev";
	
	public static final String EMR_COMPANY_METROSOFT = "metrosoft";
	public static final String EMR_COMPANY_JAINCOM = "jaincom";
	
	// -----------------------------------------------------------------------------------------------------------------------

	// 로그인 정보 - 병원id
	public static String getHospitalId(Context context) {
		return context.getSharedPreferences("emrdroid", Context.MODE_PRIVATE).getString("hospitalId", "");
	}

	// 로그인 정보 - 병원명
	public static String getHospitalName(Context context) {
		return context.getSharedPreferences("emrdroid", Context.MODE_PRIVATE).getString("hospitalName", "");
	}

	// 로그인 정보 - 사용자id
	public static String getUserId(Context context) {
		return context.getSharedPreferences("emrdroid", Context.MODE_PRIVATE).getString("userId", "");
	}

	// 로그인 정보 - 사용자이름
	public static String getUserName(Context context) {
		return context.getSharedPreferences("emrdroid", Context.MODE_PRIVATE).getString("userName", "");
	}

	public static String getCollapseYn(Context context) {
		return context.getSharedPreferences("emrdroid", Context.MODE_PRIVATE).getString("collapseYn", "");
	}

	// 로그인 정보 - 타 서버 사용여부
	public static String getServletUseYn(Context context) {
		return context.getSharedPreferences("emrdroid", Context.MODE_PRIVATE).getString("servletUseYn", "");
	}

	// 로그인 정보 - 타 서버 ip
	public static String getServletIp(Context context) {
		return context.getSharedPreferences("emrdroid", Context.MODE_PRIVATE).getString("servletIp", "");
	}

	// 로그인 정보 - emr 회사
	public static String getEmrCompany(Context context) {
		return context.getSharedPreferences("emrdroid", Context.MODE_PRIVATE).getString("emrCompany", "");
	}

	// 로그인 정보 - 마스킹여부
	public static boolean getMaskYn(Context context) {
		return context.getSharedPreferences("emrdroid", Context.MODE_PRIVATE).getString("maskYn", "").equalsIgnoreCase("Y");
	}

	// 로그인 정보 - 재원환자리스트 조회시 의사명 대신 진료과명을 출력하지 여부
	public static boolean getInPatientListDoctDeptnm(Context context){
		return context.getSharedPreferences("emrdroid", Context.MODE_PRIVATE).getString("inPatientListDoctDeptnm", "").equalsIgnoreCase("Y");
	}

	// 로그인 정보 - 재원환자리스트 조회화면의 의사선택 버튼 숨김 여부
	public static boolean getInPatientListDoctPopupButtonHideYn(Context context){
		return context.getSharedPreferences("emrdroid", Context.MODE_PRIVATE).getString("inPatientListDoctPopupButtonHideYn", "").equalsIgnoreCase("Y");
	}

	// 로그인 정보 - TPR 등록버튼 숨김 여부
	public static boolean getTprButtonHideYn(Context context){
		return context.getSharedPreferences("emrdroid", Context.MODE_PRIVATE).getString("tprEditButtonHideYn", "").equalsIgnoreCase("Y");
	}

	// 로그인 정보 - 동의서이미지파일 포맷(기본:PNG,...)
	public static String getCcfImageFormat(Context context){
		return context.getSharedPreferences("emrdroid", Context.MODE_PRIVATE).getString("ccfImageFormat", "");
	}

	// 로그인 정보 - 환자안전관료 사용 여부
	public static boolean getPatientSafeCheckYn(Context context){
		return context.getSharedPreferences("emrdroid", Context.MODE_PRIVATE).getString("patientSafeCheckYn", "").equalsIgnoreCase("Y");
	}

	// 로그인 정보 - 환자안전관료 사용 여부
	public static boolean getCertificateHideYn(Context context){
		return context.getSharedPreferences("emrdroid", Context.MODE_PRIVATE).getString("certificateHideYn", "").equalsIgnoreCase("Y");
	}

	// 로그인 정보 - 환자안전관리료 화면에서 내장 카메라를 스캐너로 사용할지 여무
	public static boolean getBarcodeScannerYn(Context context){
		return context.getSharedPreferences("emrdroid", Context.MODE_PRIVATE).getString("barcodeScannerYn", "").equalsIgnoreCase("Y");
	}

	// 로그인 정보 - 간호기록지 화면에서 AI 기능을 사용할지 여무
	public static boolean getNrChartAiYn(Context context){
		return context.getSharedPreferences("emrdroid", Context.MODE_PRIVATE).getString("nrChartAiYn", "").equalsIgnoreCase("Y");
	}

	// -----------------------------------------------------------------------------------------------------------------------

	// 재원환자리스트 - 조회병동 저장
	public static void setWardCode(Context context,String wardCode,String wardCodeName) {
		SharedPreferences prefs = context.getSharedPreferences("emrdroid", context.MODE_PRIVATE);
		SharedPreferences.Editor ed = prefs.edit();
		//ed.clear(); 이전정보를 다시 사용하기 위하여 정보를 저장함.
		ed.putString("wardCode", wardCode);
		ed.putString("wardCodeName", wardCodeName);
		ed.commit();
	}

	// 재원환자리스트 - 조회진료과 저장
	public static void setDeptCode(Context context,String deptCode,String deptCodeName) {
		SharedPreferences prefs = context.getSharedPreferences("emrdroid", context.MODE_PRIVATE);
		SharedPreferences.Editor ed = prefs.edit();
		//ed.clear(); 이전정보를 다시 사용하기 위하여 정보를 저장함.
		ed.putString("deptCode", deptCode);
		ed.putString("deptCodeName", deptCodeName);
		ed.commit();
	}

	// 재원환자리스트 - 조회의사 저장
	public static void setPdridCode(Context context,String pdridCode,String pdridCodeName) {
		SharedPreferences prefs = context.getSharedPreferences("emrdroid", context.MODE_PRIVATE);
		SharedPreferences.Editor ed = prefs.edit();
		//ed.clear(); 이전정보를 다시 사용하기 위하여 정보를 저장함.
		ed.putString("pdridCode", pdridCode);
		ed.putString("pdridCodeName", pdridCodeName);
		ed.commit();
	}

	// 재원환자리스트 - 조회병동코드
	public static String getWardCode(Context context) {
		return context.getSharedPreferences("emrdroid", Context.MODE_PRIVATE).getString("wardCode", "");
	}

	// 재원환자리스트 - 조회병동명칭
	public static String getWardCodeName(Context context, String defaultValue) {
		return context.getSharedPreferences("emrdroid", Context.MODE_PRIVATE).getString("wardCodeName", defaultValue);
	}

	// 재원환자리스트 - 조회진료과코드
	public static String getDeptCode(Context context) {
		return context.getSharedPreferences("emrdroid", Context.MODE_PRIVATE).getString("deptCode", "");
	}

	// 재원환자리스트 - 조회진료과명칭
	public static String getDeptCodeName(Context context, String defaultValue) {
		return context.getSharedPreferences("emrdroid", Context.MODE_PRIVATE).getString("deptCodeName", defaultValue);
	}

	// 재원환자리스트 - 조회의사코드
	public static String getPdridCode(Context context) {
		return context.getSharedPreferences("emrdroid", Context.MODE_PRIVATE).getString("pdridCode", "");
	}

	// 재원환자리스트 - 조회의사명칭
	public static String getPdridCodeName(Context context, String defaultValue) {
		return context.getSharedPreferences("emrdroid", Context.MODE_PRIVATE).getString("pdridCodeName", defaultValue);
	}

	// 재원환자리스트 - 재원환자 정렬순서
	public static String getSortOrder(Context context, String defaultValue) {
		return context.getSharedPreferences("emrdroid", Context.MODE_PRIVATE).getString("sortOrder", defaultValue);
	}
	public static void setSortOrder(Context context, String sortOrder) {
		SharedPreferences prefs = context.getSharedPreferences("emrdroid", context.MODE_PRIVATE);
		SharedPreferences.Editor ed = prefs.edit();
		//ed.clear(); 이전정보를 다시 사용하기 위하여 정보를 저장함.
		ed.putString("sortOrder", sortOrder);
		ed.commit();
	}

	// 임시저장동의서목록 - 정렬순서
	public static String getPresavedConsentFormListSortOrder(Context context, String defaultValue) {
		return context.getSharedPreferences("emrdroid", Context.MODE_PRIVATE).getString("presavedConsentFormListsortOrder", defaultValue);
	}
	public static void setPresavedConsentFormListSortOrder(Context context, String sortOrder) {
		SharedPreferences prefs = context.getSharedPreferences("emrdroid", context.MODE_PRIVATE);
		SharedPreferences.Editor ed = prefs.edit();
		//ed.clear(); 이전정보를 다시 사용하기 위하여 정보를 저장함.
		ed.putString("presavedConsentFormListsortOrder", sortOrder);
		ed.commit();
	}

	// 환자검색 - 외래+입원, 외래, 입원
	public static String getSearchIofg(Context context, String defaultValue) {
		return context.getSharedPreferences("emrdroid", Context.MODE_PRIVATE).getString("searchIofg", defaultValue);
	}
	public static void setSearchIofg(Context context, String searchIofg) {
		SharedPreferences prefs = context.getSharedPreferences("emrdroid", context.MODE_PRIVATE);
		SharedPreferences.Editor ed = prefs.edit();
		//ed.clear(); 이전정보를 다시 사용하기 위하여 정보를 저장함.
		ed.putString("searchIofg", searchIofg);
		ed.commit();
	}

	// 사용자가 이전에 선택한 탭 정보(0.재원환자 1.외래환자 2.환자검색)
	public static String getCurrentTabId(Context context) {
		return context.getSharedPreferences("emrdroid", Context.MODE_PRIVATE).getString("currentTabId", "0");
	}
	public static void setCurrentTabId(Context context, String currentTabId) {
		SharedPreferences prefs = context.getSharedPreferences("emrdroid", context.MODE_PRIVATE);
		SharedPreferences.Editor ed = prefs.edit();
		//ed.clear(); 이전정보를 다시 사용하기 위하여 정보를 저장함.
		ed.putString("currentTabId", currentTabId);
		ed.commit();
	}

	// -----------------------------------------------------------------------------------------------------------------------

	// 외래환자리스트 - 조회진료과 저장
	public static void setOutDeptCode(Context context,String deptCode,String deptCodeName) {
		SharedPreferences prefs = context.getSharedPreferences("emrdroid", context.MODE_PRIVATE);
		SharedPreferences.Editor ed = prefs.edit();
		//ed.clear(); 이전정보를 다시 사용하기 위하여 정보를 저장함.
		ed.putString("outDeptCode", deptCode);
		ed.putString("outDeptCodeName", deptCodeName);
		ed.commit();
	}

	// 외래환자리스트 - 조회의사 저장
	public static void setOutPdridCode(Context context,String pdridCode,String pdridCodeName) {
		SharedPreferences prefs = context.getSharedPreferences("emrdroid", context.MODE_PRIVATE);
		SharedPreferences.Editor ed = prefs.edit();
		//ed.clear(); 이전정보를 다시 사용하기 위하여 정보를 저장함.
		ed.putString("outPdridCode", pdridCode);
		ed.putString("outPdridCodeName", pdridCodeName);
		ed.commit();
	}

	// 외래환자리스트 - 조회진료과코드
	public static String getOutDeptCode(Context context) {
		return context.getSharedPreferences("emrdroid", Context.MODE_PRIVATE).getString("outDeptCode", "");
	}

	// 외래환자리스트 - 조회진료과명칭
	public static String getOutDeptCodeName(Context context, String defaultValue) {
		return context.getSharedPreferences("emrdroid", Context.MODE_PRIVATE).getString("outDeptCodeName", defaultValue);
	}

	// 외래환자리스트 - 조회의사코드
	public static String getOutPdridCode(Context context) {
		return context.getSharedPreferences("emrdroid", Context.MODE_PRIVATE).getString("outPdridCode", "");
	}

	// 외래환자리스트 - 조회의사명칭
	public static String getOutPdridCodeName(Context context, String defaultValue) {
		return context.getSharedPreferences("emrdroid", Context.MODE_PRIVATE).getString("outPdridCodeName", defaultValue);
	}

	// 외래환자리스트 - 외래환자 정렬순서
	public static String getOutSortOrder(Context context, String defaultValue) {
		return context.getSharedPreferences("emrdroid", Context.MODE_PRIVATE).getString("outSortOrder", defaultValue);
	}
	public static void setOutSortOrder(Context context, String outSortOrder) {
		SharedPreferences prefs = context.getSharedPreferences("emrdroid", context.MODE_PRIVATE);
		SharedPreferences.Editor ed = prefs.edit();
		//ed.clear(); 이전정보를 다시 사용하기 위하여 정보를 저장함.
		ed.putString("outSortOrder", outSortOrder);
		ed.commit();
	}

	// -----------------------------------------------------------------------------------------------------------------------

	// 동의서 해상도(너비)
	public static int getPicWidth(Context context){
		return context.getSharedPreferences("emrdroid", Context.MODE_PRIVATE).getInt("ccfPicWidth", 0);
	}

	public static void setPicWidth(Context context, int picWidth){
		SharedPreferences prefs = context.getSharedPreferences("emrdroid", context.MODE_PRIVATE);
		SharedPreferences.Editor ed = prefs.edit();
		ed.putInt("ccfPicWidth", picWidth);
		ed.commit();
	}

	// 동의서 해상도(높이)
	public static int getPicHeight(Context context){
		return context.getSharedPreferences("emrdroid", Context.MODE_PRIVATE).getInt("ccfPicHeight", 0);
	}

	public static void setPicHeight(Context context, int picHeight){
		SharedPreferences prefs = context.getSharedPreferences("emrdroid", context.MODE_PRIVATE);
		SharedPreferences.Editor ed = prefs.edit();
		ed.putInt("ccfPicHeight", picHeight);
		ed.commit();
	}
	// 동의서에 사용될 펜 두께
	public static int getCcfPenWidth(Context context){
		return context.getSharedPreferences("emrdroid", Context.MODE_PRIVATE).getInt("ccfPenWidth", 2);
	}

	public static void setCcfPenWidth(Context context, int penWidth) {
		SharedPreferences prefs = context.getSharedPreferences("emrdroid", context.MODE_PRIVATE);
		SharedPreferences.Editor ed = prefs.edit();
		ed.putInt("ccfPenWidth", penWidth);
		ed.commit();
	}

	// 동의서에 사용될 재우개 두께
	public static int getCcfEraserWidth(Context context){
		return context.getSharedPreferences("emrdroid", Context.MODE_PRIVATE).getInt("ccfEraserWidth", 2);
	}

	public static void setCcfEraserWidth(Context context, int eraserWidth) {
		SharedPreferences prefs = context.getSharedPreferences("emrdroid", context.MODE_PRIVATE);
		SharedPreferences.Editor ed = prefs.edit();
		ed.putInt("ccfEraserWidth", eraserWidth);
		ed.commit();
	}

	// 동의서에 사용될 펜 색
	public static String getCcfPenColor(Context context){
		return context.getSharedPreferences("emrdroid", Context.MODE_PRIVATE).getString("ccfPenColor", "검정");
	}

	public static void setCcfPenColor(Context context, String penColor) {
		SharedPreferences prefs = context.getSharedPreferences("emrdroid", context.MODE_PRIVATE);
		SharedPreferences.Editor ed = prefs.edit();
		ed.putString("ccfPenColor", penColor);
		ed.commit();
	}

	public static String getUncaughtExceptionMessage(Context context){
		return context.getSharedPreferences("emrdroid", Context.MODE_PRIVATE).getString("uncagtedExceptionMessage", "");
	}

	public static void setUncaughtExceptionMessage(Context context, String message){
		SharedPreferences prefs = context.getSharedPreferences("emrdroid", context.MODE_PRIVATE);
		SharedPreferences.Editor ed = prefs.edit();
		ed.putString("uncagtedExceptionMessage", message);
		ed.commit();
	}

	// 개발자 디바이스인지
	public static boolean isDeveloper(Context context){
		String wifiMacAddress = Device.getWifiMacAddress(context);
		return "CC:F9:E8:A3:9B:A7".equalsIgnoreCase(wifiMacAddress);
	}

}
