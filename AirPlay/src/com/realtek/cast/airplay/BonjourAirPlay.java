
package com.realtek.cast.airplay;


import com.realtek.cast.util.Bonjour;

import java.util.HashMap;
import java.util.Map;

import javax.jmdns.ServiceInfo;

public class BonjourAirPlay extends Bonjour {
	
	public BonjourAirPlay(String name, int port) {
	    super(name, port);
    }

	protected ServiceInfo onCreateServiceInfo(byte[] hwAddr, String name, int port) {
		String addr = getStringHardwareAdress(hwAddr);
		Map<String, String> txt = new HashMap<String, String>();
		txt.put("deviceid", addr);
		txt.put("features", String.format("0x%04x", AirPlay.FEATURES));
		txt.put("model", AirPlay.MODEL);
		txt.put("srcvers", AirPlay.SRCVERS);
		txt.put("vv", "1");
		txt.put("rhd", "1.9.7");
		return ServiceInfo.create(AirPlay.TYPE, name, port, 0, 0, txt);
	}

	private static String getStringHardwareAdress(byte[] hwAddr) {
		StringBuilder sb = new StringBuilder();
		sb.append(String.format("%02x", hwAddr[0]));
		for (int i = 1; i < hwAddr.length; i++) {
			sb.append(String.format(":%02x", hwAddr[i]));
		}
		return sb.toString();
	}
}
