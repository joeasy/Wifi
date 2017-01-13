package com.realtek.DLNA_DMP_1p5;

import com.realtek.DLNA_DMP_1p5.UPnPObjInfo;
import com.realtek.DLNA_DMP_1p5.DLNACallback;;

public final class DLNA_DMP_1p5
{
	private static DLNACallback mCallback;
	public native static void DLNA_DMP_1p5_Init();
	public native static void DLNA_DMP_1p5_UnInit();
	public native static void DLNA_DMP_1p5_Start();
	public native static void DLNA_DMP_1p5_Terminate();
	public native static void DLNA_DMP_1p5_stopWatingResponse();
	public native static void DLNA_DMP_1p5_RegisterBrowserUpdateFunc();
	public native static void DLNA_DMP_1p5_UnregisterBrowserUpdateFunc();
	public native static boolean DLNA_DMP_1p5_UPnPServiceDeviceBrowse();
	public native static boolean DLNA_DMP_1p5_UPnPServiceBrowse(String id);
	public native static boolean DLNA_DMP_1p5_cdupDirectoryStack(int level);
	public native static void DLNA_DMP_1p5_unsetMediaServer();
	public native static boolean DLNA_DMP_1p5_setMediaServerByFriendlyName(byte[] name);
	public native static boolean DLNA_DMP_1p5_setDirectoryByTitleName(String name);
	public native static int DLNA_DMP_1p5_MediaContainerObjectSizes();
	public native static int DLNA_DMP_1p5_MediaItemObjectSizes();
	public native static int DLNA_DMP_1p5_MediaItemVideoSizes();
	public native static int DLNA_DMP_1p5_MediaItemAudioSizes();
	public native static int DLNA_DMP_1p5_MediaItemImageSizes();
	public native static String DLNA_DMP_1p5_queryResourceByFile(String fileName, int queryType);
	public native static String DLNA_DMP_1p5_queryResourceByID(String id, int queryType);
	public native static Object[] DLNA_DMP_1p5_querySubtitleURIListByID(String id, String mimeType);
	public native static int DLNA_DMP_1p5_MediaServerSizes();
	public native static byte[] DLNA_DMP_1p5_MediaServerName(int index);
	public native static String DLNA_DMP_1p5_UPnPServiceContentDirectory(int index);
	public native static UPnPObjInfo DLNA_DMP_1p5_queryContainerObjInfoByIndex(int index);
	public native static String DLNA_DMP_1p5_UPnPServiceContentFile(int index);
	public native static UPnPObjInfo DLNA_DMP_1p5_queryItemObjInfoByIndex(int index);
	public native static int DLNA_DMP_1p5_GetMediaType(String filename);
	public native static String DLNA_DMP_1p5_MediaServerLocationURL(int i);
	public native static String DLNA_DMP_1p5_MediaServerManufacturer(int i);
	public native static String DLNA_DMP_1p5_MediaServerModelDescription(int i);
	public native static String DLNA_DMP_1p5_MediaServerRegzaApps(int i);
	public native static Object[] DLNA_DMP_1p5_queryResourceListByID(String fileID, int queryType);
	public native static boolean QueryByteBasedSeekableofID(String charID);
	public native static boolean QueryByteBasedSeekableofFileName(String fileName);
	public native static void DLNA_DMP_1p5_MediaServerSearchScan();
	public native static byte[] DLNA_DMP_1p5_getMediaInfo(String fileName);
	public native static int DLNA_DMP_1p5_getFileType(String fileName);
	public native static String DLNA_DMP_1p5_MediaServerUDN(int i);
	public native static void DLNA_DMP_1p5_MediaServerDelete(String chosenUDN);
	public native static boolean DLNA_DMP_1p5_setMediaServerByUDN(String serverUDN);
	static
	{
		//System.loadLibrary("fileaccess");
		System.loadLibrary("upnp");
		System.loadLibrary("DMP");
	}
	
	public void DLNADeviceAddedOrRemoved(String addedOrRemoved, byte[] tmpDmsName){
		String tmpServerName = new String(tmpDmsName);
		if(addedOrRemoved.equals("added")){
			if(mDeviceStatusListener != null)
				mDeviceStatusListener.deviceAdded(tmpServerName);
		}else if(addedOrRemoved.equals("removed")){
			if(mDeviceStatusListener != null)
				mDeviceStatusListener.deviceRemoved(tmpServerName);
		}
	}
	
	 public interface DeviceStatusListener{
		 public void deviceAdded(String serverName);
		 public void deviceRemoved(String serverName);
	 }
	 
	 public void setDeviceStatusListener(DeviceStatusListener listener){
		 mDeviceStatusListener = listener;
	 }

	 private static DeviceStatusListener mDeviceStatusListener; 
}
