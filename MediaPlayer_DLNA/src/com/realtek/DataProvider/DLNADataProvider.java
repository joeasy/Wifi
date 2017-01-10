package com.realtek.DataProvider;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import com.realtek.DLNA_DMP_1p5.DLNA_DMP_1p5;
import com.realtek.DLNA_DMP_1p5.UPNP_DMP_RES_TYPE;
import com.realtek.Utils.FileUtils;

public class DLNADataProvider {

	private static boolean isInited = false;

	public final static int UPNP_DMP_RES_ID = 0;
	public final static int UPNP_DMP_RES_PARENT_ID = 1;
	public final static int UPNP_DMP_RES_PROTOCOLINFO = 2;
	public final static int UPNP_DMP_RES_TITLE = 3;
	public final static int UPNP_DMP_RES_URI = 4;
	public final static int UPNP_DMP_RES_SIZE = 5;
	public final static int UPNP_DMP_RES_DURATION = 6;
	public final static int UPNP_DMP_RES_BITRATE = 7;
	public final static int UPNP_DMP_RES_RESOLUTION = 8;
	public final static int UPNP_DMP_RES_CREATOR = 9;
	public final static int UPNP_DMP_RES_GENRE = 10;
	public final static int UPNP_DMP_RES_ALBUM = 11;
	public final static int UPNP_DMP_RES_ARTIST = 12;
	public final static int UPNP_DMP_RES_DATE = 13;

	public static void Init() {
		if (isInited == true)
			return;

		isInited = true;

		DLNA_DMP_1p5.DLNA_DMP_1p5_Init();
		DLNA_DMP_1p5.DLNA_DMP_1p5_Start();
		DLNA_DMP_1p5.DLNA_DMP_1p5_RegisterBrowserUpdateFunc();
	}

	public static void DeInit() {
		if (isInited == false)
			return;

		isInited = false;

		DLNA_DMP_1p5.DLNA_DMP_1p5_UnInit();
		DLNA_DMP_1p5.DLNA_DMP_1p5_UnregisterBrowserUpdateFunc();
	}

	// server
	public static int getServerSize() {
		return DLNA_DMP_1p5.DLNA_DMP_1p5_MediaServerSizes();
	}

	public static String getServerUrl(int index) {
		String url = null;
		if (index < getServerSize())
			url = DLNA_DMP_1p5.DLNA_DMP_1p5_MediaServerLocationURL(index);
		return url;
	}

	public static String getServerTitle(int index) {
		String title = null;
		byte[] tmpTitle = null;
		if (index < getServerSize())
			tmpTitle = DLNA_DMP_1p5.DLNA_DMP_1p5_MediaServerName(index);
		title = new String(tmpTitle);
		return title;
	}

	public static String getServerManufacturer(int index) {
		String manufacturerName = null;
		if (index < getServerSize())
			manufacturerName = DLNA_DMP_1p5
					.DLNA_DMP_1p5_MediaServerManufacturer(index);
		return manufacturerName;
	}

	public static String getServerModelDescription(int index) {
		String modelDescription = null;
		if (index < getServerSize())
			modelDescription = DLNA_DMP_1p5
					.DLNA_DMP_1p5_MediaServerModelDescription(index);
		return modelDescription;
	}

	public static String getMediaServerRegzaApps(int index) {
		String regzaaapps = null;
		if (index < getServerSize())
			regzaaapps = DLNA_DMP_1p5.DLNA_DMP_1p5_MediaServerRegzaApps(index);
		return regzaaapps;
	}

	public static boolean browseServer(String serverUDN) {
		//byte[] serverName = server.getBytes();
		//DLNA_DMP_1p5.DLNA_DMP_1p5_setMediaServerByFriendlyName(serverName);
		boolean setServer = setMediaServerByUDN(serverUDN);
		if(setServer)
			return DLNA_DMP_1p5.DLNA_DMP_1p5_UPnPServiceDeviceBrowse();
		else
			return false;
	}
	
	public static boolean browseLiveServer(String server, String uniqueCharID) {
		byte[] serverName = server.getBytes();
		DLNA_DMP_1p5.DLNA_DMP_1p5_setMediaServerByFriendlyName(serverName);
		return DLNA_DMP_1p5.DLNA_DMP_1p5_UPnPServiceBrowse(uniqueCharID);
	}

	// directory
	public static int getDirectorySize() {
		return DLNA_DMP_1p5.DLNA_DMP_1p5_MediaContainerObjectSizes();
	}

	public static String getDirectoryTitle(int index) {
		String title = null;
		if (index < getDirectorySize())
			title = DLNA_DMP_1p5
					.DLNA_DMP_1p5_UPnPServiceContentDirectory(index);
		return title;
	}

	public static String getDirectoryCharID(int index) {
		String title = null;
		if (index < getDirectorySize())
			title = DLNA_DMP_1p5
					.DLNA_DMP_1p5_queryContainerObjInfoByIndex(index).UniqueCharID;
		return title;
	}
	
	public static boolean browserDirectory(String uniqueCharID) {
		return DLNA_DMP_1p5.DLNA_DMP_1p5_UPnPServiceBrowse(uniqueCharID);
	}

	// item
	public static int getItemSize() {
		return DLNA_DMP_1p5.DLNA_DMP_1p5_MediaItemObjectSizes();
	}

	public static String getItemTitle(int index) {
		String title = null;
		if (index < getItemSize())
			title = DLNA_DMP_1p5.DLNA_DMP_1p5_UPnPServiceContentFile(index);
		return title;
	}

	public static String getItemCharID(int index) {
		String CharID = null;
		if (index < getItemSize())
			CharID = DLNA_DMP_1p5.DLNA_DMP_1p5_queryItemObjInfoByIndex(index).UniqueCharID;
		return CharID;
	}

	public static String getItemUrl(String charID) {
		return DLNA_DMP_1p5.DLNA_DMP_1p5_queryResourceByID(charID,
				UPNP_DMP_RES_TYPE.UPNP_DMP_RES_URI);
	}

	public static int GetMediaType(String filename) {
		return DLNA_DMP_1p5.DLNA_DMP_1p5_GetMediaType(filename);
	}

	public static String queryDataByFile(String filename, int queryType, String extend) {
		String data = null;
		if (filename != null || filename != "") {
			switch (queryType) {
			case UPNP_DMP_RES_ID:
				break;
			case UPNP_DMP_RES_PARENT_ID:
				break;
			case UPNP_DMP_RES_PROTOCOLINFO:
				break;
			case UPNP_DMP_RES_TITLE:
				break;
			case UPNP_DMP_RES_URI:
				break;
			case UPNP_DMP_RES_SIZE:
				break;
			case UPNP_DMP_RES_DURATION:
				filename = FileUtils.removeExtension(filename);
				data = DLNA_DMP_1p5.DLNA_DMP_1p5_queryResourceByFile(filename,
						queryType);
				if (data.equals("-1")) {
					data = "";
				}
				if (!data.equals("")) {
					int time = Integer.parseInt(data);
					int second = time % 60;
					int minute = time / 60;
					int hour = minute / 60;
					minute -= hour * 60;
					data = getTime(hour) + ":" + getTime(minute) + ":"
							+ getTime(second);
				}
				break;
			case UPNP_DMP_RES_BITRATE:
			case UPNP_DMP_RES_RESOLUTION:
				data = DLNA_DMP_1p5.DLNA_DMP_1p5_queryResourceByFile(filename,
						queryType);
				break;
			case UPNP_DMP_RES_CREATOR:
				break;
			case UPNP_DMP_RES_GENRE:
				break;
			case UPNP_DMP_RES_ALBUM:
				data = DLNA_DMP_1p5.DLNA_DMP_1p5_queryResourceByFile(filename,
						queryType);
				if (data.equals("")) {
					data = "Unknown";
				}
				break;
			case UPNP_DMP_RES_ARTIST:
				filename = FileUtils.removeExtension(filename);
				data = DLNA_DMP_1p5.DLNA_DMP_1p5_queryResourceByFile(filename,
						queryType);
				if (data.equals("")) {
					data = "Unknown";
				}
				break;
			case UPNP_DMP_RES_DATE:
				data = DLNA_DMP_1p5.DLNA_DMP_1p5_queryResourceByFile(filename,
						queryType);
				Date last_date;
				SimpleDateFormat df_des;
				try {
					last_date = parseStringToDate(data);
					if (last_date != null) {
						if (extend != null)
							df_des = new SimpleDateFormat(extend);
						else
							df_des = new SimpleDateFormat("yyyy.MM.dd");
						data = df_des.format(last_date);
					}
				} catch (ParseException e) {
					e.printStackTrace();
				}
				break;
			default:
				break;
			}

		}
		return data;
	}
	
	public static String queryDataByID(String uniqueCharID, int queryType) {
		String data = null;
		if (uniqueCharID != null || uniqueCharID != "") {
			switch (queryType) {
			case UPNP_DMP_RES_ID:
				break;
			case UPNP_DMP_RES_PARENT_ID:
				break;
			case UPNP_DMP_RES_PROTOCOLINFO:
				data = DLNA_DMP_1p5.DLNA_DMP_1p5_queryResourceByID(uniqueCharID, queryType);
				break;
			case UPNP_DMP_RES_TITLE:
				break;
			case UPNP_DMP_RES_URI:
				break;
			case UPNP_DMP_RES_SIZE:
				data = DLNA_DMP_1p5.DLNA_DMP_1p5_queryResourceByID(uniqueCharID, queryType);
				break;
			case UPNP_DMP_RES_DURATION:
				data = DLNA_DMP_1p5.DLNA_DMP_1p5_queryResourceByID(uniqueCharID, queryType);
				break;
			case UPNP_DMP_RES_BITRATE:
			case UPNP_DMP_RES_RESOLUTION:
				break;
			case UPNP_DMP_RES_CREATOR:
				break;
			case UPNP_DMP_RES_GENRE:
				break;
			case UPNP_DMP_RES_ALBUM:
				break;
			case UPNP_DMP_RES_ARTIST:
				break;
			case UPNP_DMP_RES_DATE:
				break;
			default:
				break;
			}

		}
		return data;
	}

	public static ArrayList<String> queryResourceListByIndex(String UniqueCharID,
			int queryType) {
		ArrayList<String> resourceList = new ArrayList<String>();
		Object[] resList;
		resList = DLNA_DMP_1p5.DLNA_DMP_1p5_queryResourceListByID(UniqueCharID,
				queryType);
		for (int i = 0; resList != null && i < resList.length; i++) {
			resourceList.add((String) resList[i]);
		}
		return resourceList;
	}

	public static Date parseStringToDate(String date) throws ParseException {
		Date result = null;
		if (!date.equals("")) {
			String parse = date;
			if (parse.contains("T")) {
				date = date.replace("T", " ");
				parse = parse.replace("T", " ");
			}
			parse = parse.replaceFirst("^[0-9]{4}([^0-9]?)", "yyyy$1");
			parse = parse.replaceFirst("^[0-9]{2}([^0-9]?)", "yy$1");
			parse = parse
					.replaceFirst("([^0-9]?)[0-9]{1,2}([^0-9]?)", "$1MM$2");
			parse = parse.replaceFirst("([^0-9]?)[0-9]{1,2}( ?)", "$1dd$2");
			parse = parse.replaceFirst("( )[0-9]{1,2}([^0-9]?)", "$1HH$2");
			parse = parse
					.replaceFirst("([^0-9]?)[0-9]{1,2}([^0-9]?)", "$1mm$2");
			parse = parse
					.replaceFirst("([^0-9]?)[0-9]{1,2}([^0-9]?)", "$1ss$2");

			DateFormat format = new SimpleDateFormat(parse);
			result = format.parse(date);
		}
		return result;
	}

	public static String getTime(int time) {
		String tmpTime = "";
		if (time < 10) {
			tmpTime = "0" + String.valueOf(time);
		} else {
			tmpTime = String.valueOf(time);
		}
		return tmpTime;
	}
	
	public static boolean queryByteBasedSeekableofID(String id){
		boolean isSeekable = false;
		isSeekable = DLNA_DMP_1p5.QueryByteBasedSeekableofID(id);
		return isSeekable;
	}
	
	public static boolean queryByteBasedSeekableofFileName(String fileName){
		boolean isSeekable = false;
		isSeekable = DLNA_DMP_1p5.QueryByteBasedSeekableofFileName(fileName);
		return isSeekable;
	}
	
	public static void mediaServerSearchScan(){
		DLNA_DMP_1p5.DLNA_DMP_1p5_MediaServerSearchScan();
	}
	
	public static int getFileType(String fileName){
		return DLNA_DMP_1p5.DLNA_DMP_1p5_getFileType(fileName);
	}
	
	public static byte[] getMediaInfo(String fileName){
		return DLNA_DMP_1p5.DLNA_DMP_1p5_getMediaInfo(fileName);
	}
	
	public static void mediaServerDelete(String chosenUDN){
		DLNA_DMP_1p5.DLNA_DMP_1p5_MediaServerDelete(chosenUDN);
	}
	
	public static String getMediaServerUDN(int index) {
		String serverUDN = null;
		if (index < getServerSize())
			serverUDN = DLNA_DMP_1p5
					.DLNA_DMP_1p5_MediaServerUDN(index);
		return serverUDN;
	}
	
	public static boolean setMediaServerByUDN(String serverUDN){
		return DLNA_DMP_1p5.DLNA_DMP_1p5_setMediaServerByUDN(serverUDN);
	}
}
