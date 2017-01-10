package com.realtek.cast.airtunes;


public interface AirTunes {
	
	public static final String TAG = "AirTunes";

	public static final int PORT = 5001;
	
	/**
	 * The AirTunes/RAOP service type
	 */
	public static final String AIR_TUNES_SERVICE_TYPE = "_raop._tcp.local.";
	
	/**
	 * The AirTunes/RAOP M-DNS service properties (TXT record)
	 */
	public static final String AIRTUNES_SERVICE_PROPERTIES =
			"tp=UDP " +
			"sm=false " +
			"sv=false " +
			"ek=1 " +
			"et=0,1 " +
			"md=0,1,2 " + /*metadata: text, artwork, progress*/
			"cn=0,1 " +/*audio codecs*/
			"ch=2 " +
			"ss=16 " +
			"sr=44100 " +
			"pw=false " +
			"vn=3 " +
			"txtvers=1";
//			"vs=" + AirPlay.SRCVERS + " " +
//			"am=" + AirPlay.MODEL;
	
	public static final int SAMPLE_RATE = 44100;

	public static final float TOLERANCE_DELAY = 0.05F;
}
