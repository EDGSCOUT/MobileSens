package com.nevin.dmeo.jnimac.jni;

import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

public class MacAddr {

	static{
		System.loadLibrary("macAddr");
	}
		
	public static native String getMacAddr(Context context);
	
	public String getLocalMacAddress(Context context) {
		WifiManager wifi = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
		WifiInfo info = wifi.getConnectionInfo();
		return info.getMacAddress();
	}
}
