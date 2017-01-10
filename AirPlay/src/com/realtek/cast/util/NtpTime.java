package com.realtek.cast.util;

import java.nio.ByteBuffer;
import java.util.Date;

/**
 * NTP timestamps are represented as a 64-bit unsigned fixed-
point number, in seconds relative to 0h on 1 January 1900. The integer
part is in the first 32 bits and the fraction part in the last 32 bits.
This format allows convenient multiple-precision arithmetic and
conversion to Time Protocol representation (seconds), but does
complicate the conversion to ICMP Timestamp message representation
(milliseconds). The precision of this representation is about 200
picoseconds, which should be adequate for even the most exotic
requirements.
 * @author Jason Lin
 *
 */
public class NtpTime {
	
	private static NtpTime sInstance = new NtpTime();
	
	public static double getTimeSecond(ByteBuffer buffer, int position) {
		sInstance.set(buffer, position);
		return sInstance.getTime();
	}
	
	public static NtpTime create(ByteBuffer buffer, int position) {
		NtpTime t = new NtpTime();
		t.set(buffer, position);
		return t;
	}
	
	public static double currentTimeSecond() {
		sInstance.setToCurrent();
		return sInstance.getTime();
	}
	
	public static NtpTime now() {
		NtpTime t = new NtpTime();
		t.setToCurrent();
	    return t;
    }
	
	private long mSecond;
	private long mFract;
	
	private NtpTime() {
		
    }

	public void set(ByteBuffer buffer, int position) {
		mSecond = buffer.getInt(position) & 0xFFFFFFFFL;
		mFract = buffer.getInt(position + 4) & 0xFFFFFFFFL;
//		Log.v("NTP", String.format("sec = 0x%4x, frac = 0x%4x, %s", second, frac, buffer.order().toString()));
	}
	
	/**
	 * 
	 * @param timeMilli NTP time in millisecond.
	 */
	public void setTime(long timeMilli) {
		mSecond = timeMilli / 1000;
		timeMilli %= 1000L;
		double t = timeMilli / 1000D;
		mFract = (long) (t * 0x100000000L);
	}
	
	public void setToCurrent() {
		long now = System.currentTimeMillis() + 2208988800000L;
		setTime(now);
	}
	
	public void writeToBuffer(ByteBuffer buffer, int position) {
		buffer.putInt(position, (int) mSecond);
		buffer.putInt(position + 4, (int) mFract);
	}
	
	public long getTimeMilli() {
		long time = mSecond * 1000L;
		time += (mFract / 0x100000000L) * 1000;
		return time;
	}
	
	public Date getDate() {
		Date d = new Date(getTimeMilli() - 2208988800000L);
		return d;
	}
	
	public double getTime() {
		return mSecond + (double) mFract / 0x100000000L;
	}

}
