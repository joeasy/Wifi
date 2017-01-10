package com.rtk.dmp;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import android.util.Log;


public class DeviceInfo {
	private final static String TAG = "DeviceInfo";
	private String deviceInfo = null;
	private int maxCount = 50;
	
	public class atrribute {
		int deviceNameLen;
		String deviceName;
		int ipLen;
		String ip;
		int macLen;
		String mac;
		int portLen;
		String port;
		int isLive;
		int userNameLen;
		String userName;
		int passwordLen;
		String password;
	}
	
	public ArrayList<atrribute> deviceList = new ArrayList<atrribute>();
	
	public DeviceInfo(String fileName) {
		//mContext = context;
		deviceInfo = fileName;
		cleanDeviceList();
		readDeviceInfo();
	}
	
	public void cleanDeviceList()
	{
		deviceList.clear();
	}
	
	public int deviceListLength()
	{
		return deviceList.size();
	}
	
	public int findDevice(String name)
	{
		int i = 0;
		int length = deviceList.size();
		
		while (i < length)
     	{
			if (deviceList.get(i).deviceName.compareTo(name)== 0)
     			break;
     		i++;
     	}
		
		if (i == length)
			return -1;
		
		return i;
	}
	
	public String findDeviceMac(String name){
		int index = findDevice(name);
		if(index > 0){
			return deviceList.get(index).mac;
		}
		return null;
	}
	
	public void removeDevice(int index)
	{
		if (index >= 0 && index < deviceList.size())
			deviceList.remove(index);
	}
	
	public void addDeviceInfo(String deviceName, String ip, String mac, String port, int isLive, String userName, String password)
	{
		int isExist = findDevice(deviceName);
		if(isExist > 0)
			return;
		
		atrribute mark = new atrribute();
		
		mark.deviceNameLen = deviceName.length();
		mark.deviceName = deviceName;
		mark.ipLen = ip.length();
		mark.ip = ip;
		mark.macLen = mac.length();
		mark.mac = mac;
		mark.portLen = port.length();
		mark.port = port;
		mark.isLive = isLive;
		mark.userNameLen = userName.length();
		mark.userName = userName;
		mark.passwordLen = password.length();
		mark.password = password;
		
		Log.v(TAG, "Save a device information:");
		Log.v(TAG, "  name:" + mark.deviceName + ", length:" + mark.deviceNameLen);
		Log.v(TAG, "password:" + mark.password + ", userName:" + mark.userName);
		
		if (deviceList.size() < maxCount)
		{
			deviceList.add(mark);
		}else 
		{
			deviceList.remove(0);
			deviceList.add(mark);
		}
	}
	
	public void readDeviceInfo()
	{
		if (deviceInfo == null)
		{
			Log.e(TAG, "Device Information null!");
			return;
		}
		
		try {
			File f = new File(deviceInfo);
			if(!f.exists()) {
				f.createNewFile();
				return ;
			}
			FileInputStream fis = new FileInputStream(f);
			int hasRead = 0;
			int length = 0;
			byte[] buf = new byte[4];
			hasRead = fis.read(buf, 0, 4);
			while(hasRead > 0)
			{	
				length = byte2int(buf);
				byte[] data = new byte[length];
				fis.read(data, 0, length);
				String deviceName = new String(data);
				
				fis.read(buf, 0, 4);
				length = byte2int(buf);
				data = new byte[length];
				fis.read(data, 0, length);
				String ip = new String(data);
				
				fis.read(buf, 0, 4);
				length = byte2int(buf);
				data = new byte[length];
				fis.read(data, 0, length);
				String mac = new String(data);
				
				fis.read(buf, 0, 4);
				length = byte2int(buf);
				data = new byte[length];
				fis.read(data, 0, length);
				String port = new String(data);
				
				fis.read(buf, 0, 4);
				int isLive = byte2int(buf);
				
				fis.read(buf, 0, 4);
				length = byte2int(buf);
				data = new byte[length];
				fis.read(data, 0, length);
				String userName = new String(data);
				
				fis.read(buf, 0, 4);
				length = byte2int(buf);
				data = new byte[length];
				fis.read(data, 0, length);
				String password = new String(data);
				
				addDeviceInfo(deviceName, ip, mac, port, isLive, userName, password);
				
				hasRead = fis.read(buf, 0, 4);
			}
			fis.close();
		} catch (FileNotFoundException e) {
			Log.v("Read Device Inforation", "DeviceInfo error");
			e.printStackTrace();
		} catch (IOException e) {
			Log.v("Read Device Inforation", "DeviceInfo error");
			e.printStackTrace();
		}
	}
	
	public void writeDeviceInfo()
	{
		try {
			File f = new File(deviceInfo);
			f.delete();
			FileOutputStream fos = new FileOutputStream(f, true);
			int i = 0;
			int count = deviceList.size();
			while (i < count)
			{
				atrribute mark = deviceList.get(i);
				
				Log.v(TAG, "Write a DeviceInfo: " + mark.deviceName);
				
				fos.write(int2byte(mark.deviceNameLen));
				fos.write(mark.deviceName.getBytes());
				fos.write(int2byte(mark.ipLen));
				fos.write(mark.ip.getBytes());
				fos.write(int2byte(mark.macLen));
				fos.write(mark.mac.getBytes());
				fos.write(int2byte(mark.portLen));
				fos.write(mark.port.getBytes());
				fos.write(int2byte(mark.isLive));
				fos.write(int2byte(mark.userNameLen));
				fos.write(mark.userName.getBytes());
				fos.write(int2byte(mark.passwordLen));
				fos.write(mark.password.getBytes());
				i++;
			}
			fos.close();
		} catch(Exception e) {
			Log.v("Write Device Information", "DeviceInfo error");
			e.printStackTrace();
		}
	}
	
	public String getDeviceName(int index)
	{
		if (index >= 0 && index < deviceList.size())
			return deviceList.get(index).deviceName;
		
		return null;
	}
	
	public String getIp(int index)
	{
		if (index >= 0 && index < deviceList.size())
			return deviceList.get(index).ip;
		
		return null;
	}
	
	public String getMac(int index)
	{
		if (index >= 0 && index < deviceList.size())
			return deviceList.get(index).mac;
		
		return null;
	}
	
	public int getIsLive(int index)
	{
		if (index >= 0 && index < deviceList.size())
			return deviceList.get(index).isLive;
		
		return -1;
	}
	
	public String getPort(int index)
	{
		if (index >= 0 && index < deviceList.size())
			return deviceList.get(index).port;
		
		return null;
	}
	
	public String getUserName(int index)
	{
		if (index >= 0 && index < deviceList.size())
			return deviceList.get(index).userName;
		
		return null;
	}
	
	public String getpassword(int index)
	{
		if (index >= 0 && index < deviceList.size())
			return deviceList.get(index).password;
		
		return null;
	}
	
	public static int byte2int(byte[] res)
	{
		int targets = ((char)res[0] | 
					((char)(res[1] & 0xff) << 8)| 
					((char)(res[2] & 0xff) << 16) | 
					((char)(res[3] & 0xff) << 24)); 
		return targets;
	}
	
	public static byte[] int2byte(int data)
	{
		byte [] targets = new byte [4];
		
		targets[0] = (byte)(data & 0xff);
		targets[1] = (byte)((data >> 8) & 0xff);
		targets[2] = (byte)((data >> 16) & 0xff);
		targets[3] = (byte)((data >> 24) & 0xff);
		
		return targets;
	}
}
