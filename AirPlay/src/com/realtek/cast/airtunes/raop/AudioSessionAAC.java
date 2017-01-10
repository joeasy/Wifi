package com.realtek.cast.airtunes.raop;
import android.annotation.TargetApi;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaCodec.BufferInfo;
import android.os.Build;

import java.nio.ByteBuffer;
import java.util.Map;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;


@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
public class AudioSessionAAC extends AudioSession {
//	private static final String TAG = "AAC";
	
	public int trackCount;
	public int sampleFreq;
	
	private SecretKeySpec k;
	private Cipher cipher;
	
	private final MediaCodec mCodec;
	
    private AudioSessionAAC(byte[] aesiv, byte[] aeskey, int sampleRate, int tracks, int minLatency, MediaFormat mediaFormat){
		super(aeskey, aesiv);
		sampleFreq = sampleRate;
		trackCount = tracks;
		mCodec = MediaCodec.createDecoderByType("audio/mp4a-latm");
		mCodec.configure(mediaFormat, null, null, 0);
		mCodec.start();
		
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
	
	public static AudioSessionAAC parse(byte[] aeskey, byte[] aesiv, Map<String, String> attr) {
		int sampleRate = 44100;
		int tracks = 2;
		MediaFormat mediaFormat = MediaFormat.createAudioFormat("audio/mp4a-latm", sampleRate, tracks);
		int profile = MediaCodecInfo.CodecProfileLevel.AACObjectMain;
		
		// AAC-eld
    	String fmtp = attr.get("fmtp");
    	if (fmtp.contains("mode=AAC-eld")) {
    		profile = MediaCodecInfo.CodecProfileLevel.AACObjectELD;
    		mediaFormat.setLong(MediaFormat.KEY_DURATION, 480L);
    	}
    	
    	mediaFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, profile);
    	mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, sampleRate * tracks * 16);
    	return new AudioSessionAAC(aesiv, aeskey, sampleRate, tracks, 11025, mediaFormat);
    }
	
	private byte[] mBuffer = new byte[4096];
	
	public int decodeFrame(byte[] data, int offset, int length, byte[] outbuffer){
		int outputsize = 0;
		try {
			int idx = mCodec.dequeueInputBuffer(0);
			if (idx >= 0) {
				ByteBuffer buf = mCodec.getInputBuffers()[idx];
				
				// Decryption
				if (cipher != null) {
					int fragment = length % 16;
					cipher.doFinal(data, offset, length - fragment, mBuffer);
					for (int i = length - fragment; i < length; i++) {
						mBuffer[i] = data[i + offset];
					}
					buf.put(mBuffer, 0, length);
				} else {
					buf.put(data, offset, length);
				}

				mCodec.queueInputBuffer(idx, 0, length, 0, 0);

				BufferInfo info = new BufferInfo();

				idx = mCodec.dequeueOutputBuffer(info, -1);
				while (idx >= 0) {
					buf = mCodec.getOutputBuffers()[idx];
					outputsize = info.size;
					buf.get(outbuffer, info.offset, info.size);
					mCodec.dequeueOutputBuffer(info, -1);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	    
		return outputsize;
	}

	@Override
    public void close() {
		mCodec.stop();
		mCodec.release();
    }

	@Override
    public int getAudioDelay() {
	    return 4410;
    }

	@Override
    public int getPacketSize() {
	    return 1408;
    }
	
}
