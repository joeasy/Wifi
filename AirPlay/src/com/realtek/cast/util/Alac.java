package com.realtek.cast.util;

public class Alac {
	
	static {
		System.loadLibrary("alac_decoder");
	}
	
	public static native void createAlac(int sampleSize, int numChannel);

	public static native void setupAlac(int frameSize, int i7a, int sampleSize, int riceHistoryMult,
	        int riceInitHistory, int riceKModifier, int i7f, int i80, int i82, int i86, int i8a);
	
	public static native int decodeFrame(byte[] inbuf, int length, int[] outbuf);
	public static native int decodeByteFrame(byte[] inbuf, int length, byte[] outbuf);
}
