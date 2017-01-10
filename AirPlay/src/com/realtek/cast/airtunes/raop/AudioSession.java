package com.realtek.cast.airtunes.raop;
import android.util.Base64;
import android.util.Log;

import com.realtek.cast.airtunes.AirTunes;

import java.security.PrivateKey;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.Cipher;


public abstract class AudioSession {
	
	private static final String TAG = AirTunes.TAG;
	
	public static AudioSession parse(String payload, PrivateKey privateKey) {
		AudioSession session = null;
		byte[] aesiv = null;
		byte[] aeskey = null;
		
		Map<String, String> attr = new HashMap<String, String>();
		
		String[] lines = payload.split("\n");
		for (String line : lines) {
			line = line.trim();
			
			if (line.startsWith("a=")) {
				String[] keyValue = line.substring(2).split(":");
				if (keyValue.length == 2) {
					attr.put(keyValue[0], keyValue[1]);
				}
			}
		}
		
		// Get key
		if (attr.containsKey("rsaaeskey")) {
			byte[] rsaaes = Base64.decode(attr.get("rsaaeskey"), Base64.DEFAULT);
			aeskey = decryptRsa(rsaaes, privateKey);
		} else if (attr.containsKey("fpaeskey")) {
			aeskey = null;
			byte[] fpaes = Base64.decode(attr.get("fpaeskey"), Base64.DEFAULT);
			aeskey = Arrays.copyOfRange(fpaes, 16, 32);
			Log.e(TAG, "Got FairPlay AES Key: Don't know how to handle it A___A: length = " + fpaes.length);
		} else {
			Log.w(TAG, "AES key not found");
		}
		
		// Get initial vector
		String iv = attr.get("aesiv");
		if (iv != null) {
			aesiv = Base64.decode(iv, Base64.DEFAULT);
		} else {
			Log.w(TAG, "AES initial vector not found");
		}
		
		String rtpMap = attr.get("rtpmap");
		if (rtpMap != null) {
			if (rtpMap.contains("AppleLossless")) {
				session = AudioSessionAlac.parse(aeskey, aesiv, attr);
			} else if (rtpMap.contains("mpeg4-generic")) {
				session = AudioSessionAAC.parse(aeskey, aesiv, attr);
			} else {
				Log.e(TAG, "Audio codec not supported: " + rtpMap);
			}
		} else {
			Log.e(TAG, "rtpmap not found");
		}
		
	    return session;
    }
	
	/**
	 * Decrypt with RSA priv key
	 * @param array
	 * @return
	 */
	private static byte[] decryptRsa(byte[] array, PrivateKey privateKey){
		try{
	        // Encrypt
	        Cipher cipher = Cipher.getInstance("RSA/NONE/OAEPPadding"); 
	        cipher.init(Cipher.DECRYPT_MODE, privateKey);
	        return cipher.doFinal(array);

		}catch(Exception e){
			e.printStackTrace();
		}

		return null;
	}
	
	abstract public int decodeFrame(byte[] data, int offset, int length, byte[] outbuffer);
	abstract public int getAudioDelay();
	abstract public int getPacketSize();
	abstract public void close();
	
	protected final byte[] aesiv;
	protected final byte[] aeskey;
	
	private int frameSize;
	private int sampleSize;
	
	public int trackCount;
	public int sampleFreq;
	
	public AudioSession(byte[] aeskey, byte[] aesiv) {
		this.aeskey = aeskey;
		this.aesiv = aesiv;
	}

	public int OUTFRAME_BYTES() {
		return 4 * (this.getFramePerPacket() + 3);
	}

	public byte[] getAESIV() {
		return this.aesiv;
	}

	public byte[] getAESKEY() {
		return this.aeskey;
	}

	public int getFramePerPacket() {
		return this.frameSize;
	}

	public int getSampleSize() {
		return this.sampleSize;
	}

}
