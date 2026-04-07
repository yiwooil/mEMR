package com.metrosoft.smart.emr.emrdroid.gt101.utils;

import java.text.SimpleDateFormat;
import java.util.Date;

public class DateUtil {
	public static String getFormattedDate(String v) {
		if (v == null) return "";
		if (v.equals("")) return "";
		if (v.length() < 8)	return v;
		return v.substring(0, 4) + "." + v.substring(4, 6) + "." + v.substring(6, 8);
	}
	
	public static String getFormattedTime(String v) {
		if (v==null) return "";
		if (v.equals("")) return "";
		if (v.length()==4){
			return v.substring(0, 2) + ":" + v.substring(2, 4);
		}else if(v.length()==6){
			return v.substring(0, 2) + ":" + v.substring(2, 4) + ":" + v.substring(4, 6);
		}else{
			return v;
		}
	}
	
	public static Date addDate(Date date, long n){
		return new Date(date.getTime() + (n*1000*60*60*24));
	}
	
	public static int getAgeYear(String birthDate) {
		// 일자인지 체크
		if(birthDate.length()!=8) return 0;
		String today="";//오늘 날짜
		int ageYear=0;//만 나이
		SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd");
		today=formatter.format(new Date()); // 시스템 날짜
		// 현재일자
		int todayYear = Integer.parseInt(today.substring(0,4));
		int todayMonth = Integer.parseInt(today.substring(4,6));
		int todayDay = Integer.parseInt(today.substring(6,8));
		// 생년월일
		int birthYear = 0;
		int birthMonth = 0;
		int birthDay = 0;
		try{
			birthYear = Integer.parseInt(birthDate.substring(0,4));
			birthMonth = Integer.parseInt(birthDate.substring(4,6));
			birthDay = Integer.parseInt(birthDate.substring(6,8));
		}catch(NumberFormatException e){
			// 숫자로 변환 오류. 일자형식이 아니다.
			return 0;
		}
		
		ageYear = todayYear - birthYear;

		if(todayMonth<birthMonth){ // 생년월일 "월" 이 지났는지 체크
			ageYear--;
		}else if(todayMonth==birthMonth){ // 생년월일 "일"이 지났는지 체크
			if(todayDay<birthDay){
				ageYear--; // 생일이 안지났으면 (만나이 -1)
			}
		}
		
		return ageYear;
	}

}
