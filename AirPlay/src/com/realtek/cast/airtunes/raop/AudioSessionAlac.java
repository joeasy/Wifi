package com.realtek.cast.airtunes.raop;
import android.util.Log;

import com.beatofthedrum.alacdecoder.AlacDecodeUtils;
import com.beatofthedrum.alacdecoder.AlacFile;
import com.realtek.cast.util.Alac;

import java.util.Map;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;


public class AudioSessionAlac extends AudioSession {
	private static final String TAG = "ALAC";
	
	private AlacFile alac;
	
	/**
	 * packet size in bytes
	 */
	private int mPacketSize; 
	private int framesPerPacket;
	private int sampleSize;
	private int _7a;
	private int rice_historymult;
	private int rice_initialhistory;
	private int rice_kmodifier;
	private int _7f;
	private int _80;
	private int _82;
	private int _86;
	private int _8a_rate;
	private BiquadFilter bFilter;
	
	public int trackCount;
	public int sampleFreq;
	
	private SecretKeySpec k;
	private Cipher cipher;
	
	private AudioSessionAlac(byte[] aesiv, byte[] aeskey, int[] fmtp){
		super(aeskey, aesiv);
		// FMTP
		// a=fmtp:96 352 0 16 40 10 14 2 255 0 0 44100
		framesPerPacket = fmtp[1];
		_7a = fmtp[2];
		sampleSize = fmtp[3];
		rice_historymult = fmtp[4];
		rice_initialhistory = fmtp[5];
		rice_kmodifier = fmtp[6];
		_7f = fmtp[7];
		_80 = fmtp[8];
		_82 = fmtp[9];
		_86 = fmtp[10];
		_8a_rate = fmtp[11];
		
		this.sampleFreq = _8a_rate;
		this.trackCount = _7f;
		mPacketSize = framesPerPacket * 2 * 2;
		
		initDecoder();
		initAES();
	}
	
	public static AudioSessionAlac parse(byte[] aeskey, byte[] aesiv, Map<String, String> attr) {
		String strFmtp = attr.get("fmtp");
		if (strFmtp == null) {
			return null;
		}
		
    	String[] temp = strFmtp.split(" ");
    	int[] fmtp = new int[temp.length];
    	if (fmtp.length != 12) {
    		Log.e(TAG, "Unrecognized fmtp: " + fmtp);
    		return null;
    	}
    	
    	for (int i = 0; i< temp.length; i++){
    		try {
	            fmtp[i] = Integer.valueOf(temp[i]);
            } catch (NumberFormatException e) {
	            e.printStackTrace();
	            return null;
            }
    	}
    	
    	return new AudioSessionAlac(aesiv, aeskey, fmtp);
    }

	private void initAES(){
		// Init AES encryption
		try {
			if (aesiv != null && aeskey != null) {
				k = new SecretKeySpec(aeskey, "AES");
				cipher = Cipher.getInstance("AES/CBC/NoPadding");
				cipher.init(Cipher.DECRYPT_MODE, k, new IvParameterSpec(aesiv));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Initiate the decoder
	 */
	private void initDecoder(){
		if (this.getSampleSize() != 16){
			Log.d("ShairPort", "ERROR: 16 bits only!!!");
			return;
		}
		
		Alac.createAlac(sampleSize, 2);
		Alac.setupAlac(getFramePerPacket(), _7a, getSampleSize(), rice_historymult, rice_initialhistory, rice_kmodifier, _7f, _80, _82, _86, _8a_rate);

		alac = AlacDecodeUtils.create_alac(this.getSampleSize(), 2);
		if (alac == null){
			Log.d("ShairPort", "ERROR: creating alac!!!");
			return;
		}
		alac.setinfo_max_samples_per_frame = this.getFramePerPacket();
		alac.setinfo_7a = _7a;
		alac.setinfo_sample_size = sampleSize;
		alac.setinfo_rice_historymult = rice_historymult;
	    alac.setinfo_rice_initialhistory = rice_initialhistory;
	    alac.setinfo_rice_kmodifier = rice_kmodifier;
	    alac.setinfo_7f = _7f;
	    alac.setinfo_80 = _80;
	    alac.setinfo_82 = _82;
	    alac.setinfo_86 = _86;
	    alac.setinfo_8a_rate = _8a_rate;
	}
	
	private static byte[] packetBuffer = new byte[4096];
	
	public int decodeFrame(byte[] data, int offset, int length, byte[] outbuffer){		
		if (length <= 0 && length > packetBuffer.length) {
			Log.e(TAG, "Invalid data length: " + length);
			return 0;
		}
		
		int outputsize = 0;
		if (cipher != null) {
			int fragment = length % 16;
			try {
				cipher.doFinal(data, offset, length - fragment, packetBuffer);
			} catch (Exception e) {
				e.printStackTrace();
			}
			for (int i = length - fragment; i < length; i++) {
				packetBuffer[i] = data[i + offset];
			}
			outputsize = Alac.decodeByteFrame(packetBuffer, length, outbuffer);
		} else {
			outputsize = Alac.decodeByteFrame(data, length, outbuffer);
		}
		assert outputsize == getFramePerPacket() * 4; // FRAME_BYTES length

		return outputsize;
	}
	
	public int OUTFRAME_BYTES(){
		return 4*(this.getFramePerPacket()+3);
	}
	
	public AlacFile getAlac(){
		return alac;
	}
	
	public void resetFilter(){
		bFilter = new BiquadFilter(this.getSampleSize(), this.getFramePerPacket());
	}
	
	public void updateFilter(int size){
		if (bFilter != null) {
			bFilter.update(size);
		}
	}
	
	public BiquadFilter getFilter(){
		return bFilter;
	}
	
	public byte[] getAESIV(){
		return this.aesiv;
	}

	public byte[] getAESKEY(){
		return this.aeskey;
	}
	
	public int getFramePerPacket(){
		return this.framesPerPacket;
	}
	
	public int getSampleSize(){
		return this.sampleSize;
	}

	@Override
    public void close() {
		
    }

	@Override
    public int getAudioDelay() {
	    return 88200;
    }

	@Override
    public int getPacketSize() {
		return mPacketSize;
    }

}
