
package com.realtek.cast.airtunes;


import com.realtek.cast.util.Bonjour;

import javax.jmdns.ServiceInfo;

public class BonjourAirTunes extends Bonjour {
	
	public BonjourAirTunes(String name, int port) {
	    super(name, port);
    }

	protected ServiceInfo onCreateServiceInfo(byte[] hwAddr, String name, int port) {
		String identifier = getStringHardwareAdress(hwAddr);
		return ServiceInfo
        .create(AirTunes.AIR_TUNES_SERVICE_TYPE,
        		identifier + "@" + name ,
                port,
                AirTunes.AIRTUNES_SERVICE_PROPERTIES);
	}

	private static String getStringHardwareAdress(byte[] hwAddr) {
		StringBuilder sb = new StringBuilder();
		for (byte b : hwAddr) {
			sb.append(String.format("%02x", b));
		}
		return sb.toString();
	}
}
