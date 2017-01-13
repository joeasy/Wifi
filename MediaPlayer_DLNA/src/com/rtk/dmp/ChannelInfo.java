package com.rtk.dmp;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import java.util.ArrayList;

import android.util.Log;

public class ChannelInfo {
	private final static String TAG = "ChannelInfo";
	private String channelInfo = null;
	private int maxCount = 1000;

	public class Channel {
		int deviceNameLen;
		String deviceName;
		int channelNameLen;
		String channelName;
		int broadTypeLen;
		String broadType;
		int sidLen;
		String sid;
		int nidLen;
		String nid;
	}
	
	public class ChannelAtrr{
		public String channelName;
		public String broadType;
		public String sid;
		public String nid;
	}
	
	public 	ChannelAtrr attr = new ChannelAtrr();
	public ArrayList<Channel> channleList = new ArrayList<Channel>();

	public ChannelInfo(String fileName) {
		channelInfo = fileName;
		cleanChannelList();
		readChannelInfo();
	}

	public void cleanChannelList() {
		channleList.clear();
	}

	public int channleListLength() {
		return channleList.size();
	}

	public void addChannelInfo(String deviceName, String channelName,
			String broadType, String sid, String nid) {
		Channel mark = new Channel();
		mark.deviceNameLen = deviceName.length();
		mark.deviceName = deviceName;
		mark.channelNameLen = channelName.getBytes().length;
		mark.channelName = channelName;
		mark.broadTypeLen = broadType.length();
		mark.broadType = broadType;
		mark.sidLen = sid.length();
		mark.sid = sid;
		mark.nidLen = nid.length();
		mark.nid = nid;

		Log.v(TAG, "Save a device channel information:");
		Log.v(TAG, "Device name:" + mark.deviceName + ", length:"
				+ mark.deviceNameLen +"\n"+ "Channel Name:"+mark.channelName 
				+ ", channel length:"+mark.channelNameLen);

		if (channleList.size() < maxCount) {
			channleList.add(mark);
		} else {
			channleList.remove(0);
			channleList.add(mark);
		}
	}

	public void readChannelInfo() {
		if (channelInfo == null) {
			Log.e(TAG, "Device Information null!");
			return;
		}

		try {
			File f = new File(channelInfo);
			if (!f.exists()) {
				f.createNewFile();
				return;
			}

			FileInputStream fis = new FileInputStream(f);
			int hasRead = 0;
			int length = 0;
			byte[] buf = new byte[4];
			hasRead = fis.read(buf, 0, 4);
			while (hasRead > 0) {
				length = byte2int(buf);
				byte[] data = new byte[length];
				fis.read(data, 0, length);
				String deviceName = new String(data);

				fis.read(buf, 0, 4);
				length = byte2int(buf);
				data = new byte[length];
				fis.read(data, 0, length);
				String channelName = new String(data);

				fis.read(buf, 0, 4);
				length = byte2int(buf);
				data = new byte[length];
				fis.read(data, 0, length);
				String broadType = new String(data);

				fis.read(buf, 0, 4);
				length = byte2int(buf);
				data = new byte[length];
				fis.read(data, 0, length);
				String sid = new String(data);

				fis.read(buf, 0, 4);
				length = byte2int(buf);
				data = new byte[length];
				fis.read(data, 0, length);
				String nid = new String(data);

				addChannelInfo(deviceName, channelName, broadType, sid, nid);
				
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

	public void writeChannelInfo() {
		try {
			File f = new File(channelInfo);
			f.delete();
			FileOutputStream fos = new FileOutputStream(f, true);
			int i = 0;
			int count = channleList.size();
			while (i < count) {
				Channel mark = channleList.get(i);

				Log.v(TAG, "Write a Device Channel Info: " + mark.deviceName);

				fos.write(int2byte(mark.deviceNameLen));
				fos.write(mark.deviceName.getBytes());
				fos.write(int2byte(mark.channelNameLen));
				fos.write(mark.channelName.getBytes());
				fos.write(int2byte(mark.broadTypeLen));
				fos.write(mark.broadType.getBytes());
				fos.write(int2byte(mark.sidLen));
				fos.write(mark.sid.getBytes());
				fos.write(int2byte(mark.nidLen));
				fos.write(mark.nid.getBytes());
				i++;
			}
			fos.close();
		} catch (Exception e) {
			Log.v("Write Device Information", "DeviceInfo error");
			e.printStackTrace();
		}
	}

	public void extractString(String source, ArrayList<ChannelAtrr> list) {
		int start = 0;
		int end = 0;
		String tmp = "";
		int max = source.lastIndexOf("digital10[index2]=\"");
		if (max >= 0) {
			while (start <= max && start >= 0) {
				start = source.indexOf("digital5[index2]=\"");
				if (start >= 0) {
					ChannelAtrr info = new ChannelAtrr();
					source = source.substring(start + 18);
					end = source.indexOf("\"");
					tmp = source.substring(0, end);
					if (tmp != null && !tmp.equals("")) {
						Log.e(TAG, "digital5 start = " + start);
						info.channelName = tmp;
						Log.e(TAG, "Channel name is"+tmp);

						tmp = "";
						start = source.indexOf("digital6[index2]=\"");
						Log.e(TAG, "digital6 start = " + start);
						source = source.substring(start + 18);
						end = source.indexOf("\"");
						tmp = source.substring(0, end);
						if (tmp != null && !tmp.equals("")) {
							info.broadType = tmp;
						} else {
							info.broadType = "";
						}

						tmp = "";
						start = source.indexOf("digital9[index2]=\"");
						Log.e(TAG, "digital9 start = " + start);
						source = source.substring(start + 18);
						end = source.indexOf("\"");
						tmp = source.substring(0, end);
						if (tmp != null && !tmp.equals("")) {
							info.sid = tmp;
						} else {
							info.sid = "";
						}

						tmp = "";
						start = source.indexOf("digital10[index2]=\"");
						Log.e(TAG, "digital10 start = " + start);
						source = source.substring(start + 19);
						end = source.indexOf("\"");
						tmp = source.substring(0, end);
						if (tmp != null && !tmp.equals("")) {
							info.nid = tmp;
						} else {
							info.nid = "";
						}
						list.add(info);
					}
				}
			}
		}
	}

	public static int byte2int(byte[] res) {
		int targets = ((char) res[0] | ((char) (res[1] & 0xff) << 8)
				| ((char) (res[2] & 0xff) << 16) | ((char) (res[3] & 0xff) << 24));
		return targets;
	}

	public static byte[] int2byte(int data) {
		byte[] targets = new byte[4];

		targets[0] = (byte) (data & 0xff);
		targets[1] = (byte) ((data >> 8) & 0xff);
		targets[2] = (byte) ((data >> 16) & 0xff);
		targets[3] = (byte) ((data >> 24) & 0xff);

		return targets;
	}

}
