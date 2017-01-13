package com.realtek.wakeonlan;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

import android.util.Log;

public final class DeviceWakeUpper {
	
	private final static String SUB_NET_MASK = "255.255.255.255";
	
	public final static boolean send(String macAddress)
	{
		if(macAddress == null)
		{
			return false;
		}
		
		String mac[] = macAddress.split(":");
		
		if(mac.length != 6)
		{
			return false;
		}
		
		byte[] buf = new byte[102];
		
		int i = 0;
		for(i = 0; i < 6; i++)
		{
			buf[i] = (byte) 0xff;
		}
		
		for (i = 0; i < 16; i++)
		{			
			buf[i*6+6] = (byte)Integer.parseInt(mac[0], 16);
			buf[i*6+7] = (byte)Integer.parseInt(mac[1], 16);
			buf[i*6+8] = (byte)Integer.parseInt(mac[2], 16);
			buf[i*6+9] = (byte)Integer.parseInt(mac[3], 16);
			buf[i*6+10] = (byte)Integer.parseInt(mac[4], 16);
			buf[i*6+11] = (byte)Integer.parseInt(mac[5], 16);
		}
		
    	try{
    		DatagramSocket ds = new DatagramSocket();
    		DatagramPacket dp = new DatagramPacket(buf, buf.length,
    				InetAddress.getByName(SUB_NET_MASK),9);
    		ds.setBroadcast(true);
    		ds.send(dp); 	
    		ds.close();
    		Log.e("112222", "222222222222222");
    	}catch(Exception e)
    	{
    		e.printStackTrace();
    		return false;
    	}
    	
    	return true;
	}
}
