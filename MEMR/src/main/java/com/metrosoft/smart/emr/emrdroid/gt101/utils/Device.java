package com.metrosoft.smart.emr.emrdroid.gt101.utils;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;

public class Device {
    public static String getWifiMacAddress(Context context) {
    	if(Build.VERSION.SDK_INT>=23){//Build.VERSION_CODES.M){
    		return Device.getWifiMacAddressNew("wlan0");
    	}else{
	    	WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
		    WifiInfo wifiInfo = wifiManager.getConnectionInfo(); 
		    String macAddress = wifiInfo == null ? null : wifiInfo.getMacAddress();
		    return macAddress;
    	}    	
    }
    
    private static String getWifiMacAddressNew(String interfaceName){
		try {
	    	List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
	    	for(NetworkInterface intf : interfaces){
	    		if (interfaceName!=null){
	    			if(!intf.getName().equalsIgnoreCase(interfaceName)) continue;
	    		}
	    		byte[] mac;
				mac = intf.getHardwareAddress();
	    		if(mac==null) return "";
	    		StringBuilder buf = new StringBuilder();
	    		for(int idx=0;idx<mac.length;idx++){
	    			if(idx==0){
	    				buf.append(String.format("%02X", mac[idx]));
	    			}else{
	    				buf.append(String.format(":%02X", mac[idx]));
	    			}
	    		}
	    		return buf.toString();
	    	}
	    	return "";
		}catch(Exception ex){		
	    	return "";
		}
    }

	public static String getIpAddress() {
		try {
			for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
				NetworkInterface intf = en.nextElement();
				for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
					InetAddress inetAddress = enumIpAddr.nextElement();
					if (!inetAddress.isLoopbackAddress() && inetAddress instanceof Inet4Address) {
						return inetAddress.getHostAddress();
					}
				}
			}
		} catch (SocketException ex) {
			ex.printStackTrace();
		}
		return null;
    }
	//출처: https://itun.tistory.com/354 [Bino:티스토리]
}
