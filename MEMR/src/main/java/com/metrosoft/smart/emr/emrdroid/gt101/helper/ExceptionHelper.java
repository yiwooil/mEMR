package com.metrosoft.smart.emr.emrdroid.gt101.helper;

public class ExceptionHelper {
	public static String toJSONString(Exception ex) {
		String returnString="";
		returnString += "[[{";
		returnString += "\"return_desc\":\"" + ex.getMessage() + "\"";
		returnString += ",";
		returnString += "\"return_code\":-1";
		returnString += "}]]";

		return returnString;
	}
	
	public static String toJSONString(String errorMessage) {
		String returnString="";
		returnString += "[[{";
		returnString += "\"return_desc\":\"" + errorMessage + "\"";
		returnString += ",";
		returnString += "\"return_code\":-1";
		returnString += "}]]";

		return returnString;
	}

}
