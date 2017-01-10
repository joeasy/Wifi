package com.realtek.cast.airtunes.raop;
import android.util.Log;

import com.beatofthedrum.alacdecoder.AlacDecodeUtils;
import com.realtek.cast.util.Alac;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * A ring buffer where every frame is decrypted, decoded and stored
 * Basically, you can put and get packets
 * @author bencall
 *
 */
public class AudioBuffer {
	private static final boolean NATIVE = true;
	
	// Constants - Should be somewhere else
	public static final int BUFFER_FRAMES = 512;	// Total buffer size (number of frame)
	public static final int START_FILL = 282;		// Alac will wait till there are START_FILL frames in buffer
	public static final int MAX_PACKET = 2048;		// Also in UDPListener (possible to merge it in one place?)

	// The lock for writing/reading concurrency
    private final Lock lock = new ReentrantLock();    
    
    // The array that represents the buffer
	private final AudioData[] audioBuffer = new AudioData[BUFFER_FRAMES];;
	
	// Can we read in buffer?
//	private boolean synced = false;
	
	//Audio infos (rate, etc...)
	AudioSessionAlac session;
	
	// The seqnos at which we read and write
	private int readIndex;							
	private int writeIndex;
	private int actualBufSize;						// The number of packet in buffer
	private boolean decoder_isStopped = false;		//The decoder stops 'cause the isn't enough packet. Waits till buffer is ok
	
	// RSA-AES decryption infos
	private SecretKeySpec k;
	private Cipher cipher; 
	
	// Needed for asking missing packets
	AudioServer server;
	
	
	
	/**
	 * Instantiate the buffer
	 * @param session	audio infos
	 * @param server	whre to ask for resending missing packets
	 */
	public AudioBuffer(AudioSession session, AudioServer server){
		this.session = (AudioSessionAlac) session;
		this.server = server;
		for (int i = 0; i< BUFFER_FRAMES; i++){
			audioBuffer[i] = new AudioData();
			audioBuffer[i].data = new int[session.OUTFRAME_BYTES()];	// = OUTFRAME_BYTES = 4(frameSize+3)
		}
	}
		
	/**
	 * Sets the packets as not ready. Audio thread will only listen to ready packets.
	 * No audio more.
	 */
	public void flush(){
		for (int i = 0; i< BUFFER_FRAMES; i++){
			audioBuffer[i].ready = false;
//			synced = false;
		}
	}
	
	
	/**
	 * Returns the next ready frame. If none, waiting for one
	 * @return
	 */
	public int[] getNextFrame(){
	    synchronized (lock) {	    
	    	while(true) {
	    		actualBufSize = writeIndex-readIndex;	// Packets in buffer
	    		if (actualBufSize < 0) { // If loop
	    			actualBufSize = 65536 - readIndex + writeIndex;
	    		}
	    		
	    		if (actualBufSize < 1) {			// If no packets more or Not synced (flush: pause)
	    			{							// If it' because there is not enough packets
	    				Log.d("ShairPort", "Underrun!!! Not enough frames in buffer!");
	    			}
	    			
	    			try {
	    				// We say the decoder is stopped and we wait for signal
	    				Log.d("ShairPort", "Waiting");
	    				lock.wait();
	    				Log.d("ShairPort", "re-starting");					
//	    				readIndex++;	// We read next packet
//	    				
//	    				// Underrun: stream reset
	    				session.resetFilter();
	    			} catch (InterruptedException e) {
	    				e.printStackTrace();
	    			}
	    		} else {
	    			break;
	    		}
	    	}
			
			// Overrunning. Restart at a sane distance
		    if (actualBufSize >= BUFFER_FRAMES) {   // overrunning! uh-oh. restart at a sane distance
				Log.d("ShairPort", "Overrun!!! Too much frames in buffer!");
		        readIndex = writeIndex - START_FILL;
		    }
			// we get the value before the unlock ;-)
		    int read = readIndex;
		    readIndex++;
		     
		    // If loop
		    actualBufSize = writeIndex-readIndex;
		    if(actualBufSize<0){
		    	actualBufSize = 65536-readIndex+ writeIndex;
		    }
		    
		    session.updateFilter(actualBufSize); 
		    
		    AudioData buf = audioBuffer[read % BUFFER_FRAMES];
		    
		    if(!buf.ready){
		    	Log.d("ShairPort", "Missing Frame!");
		    	// Set to zero then
		    	for(int i=0; i<buf.data.length; i++){
		    		buf.data[i] = 0;
		    	}
		    }
		    buf.ready = false;
		    
		    // SEQNO is stored in a short an come back to 0 when equal to 65536 (2 bytes)
		    if(readIndex == 65536){
		    	readIndex = 0;
		    }
			return buf.data;

		}
	}
	
	private static final byte[] sDecodeBuffer = new byte[4096];
	/**
	 * Adds packet into the buffer
	 * @param seqno	seqno of the given packet. Used as index
	 * @param data
	 */
	public void putPacketInBuffer(int seqno, byte[] data, int offset, int length){
	    // Ring buffer may be implemented in a Hashtable in java (simplier), but is it fast enough?		
		// We lock the thread
		synchronized(lock){
		
//			if(!synced){
//				writeIndex = seqno;
//				readIndex = seqno;
//				synced = true;
//			}
	
			int outputSize = 0;
			if (seqno == writeIndex){													// Packet we expected
				outputSize = this.alac_decode(data, offset, length, audioBuffer[(seqno % BUFFER_FRAMES)].data);		// With (seqno % BUFFER_FRAMES) we loop from 0 to BUFFER_FRAMES
				audioBuffer[(seqno % BUFFER_FRAMES)].ready = true;
				writeIndex++;
			} else if(seqno > writeIndex){												// Too early, did we miss some packet between writeIndex and seqno?
				server.requestResend(writeIndex, seqno);
				outputSize = this.alac_decode(data, offset, length,  audioBuffer[(seqno % BUFFER_FRAMES)].data);
				audioBuffer[(seqno % BUFFER_FRAMES)].ready = true;
				writeIndex = seqno + 1;
			} else if(seqno > readIndex){												// readIndex < seqno < writeIndex not yet played but too late. Still ok
				outputSize = this.alac_decode(data, offset, length,  audioBuffer[(seqno % BUFFER_FRAMES)].data);
				audioBuffer[(seqno % BUFFER_FRAMES)].ready = true;
			} else {
				Log.d("ShairPort", "Late packet with seq. numb.: " + seqno);			// Really to late
			}
			
			// The number of packet in buffer
		    actualBufSize = writeIndex - readIndex;
		    if(actualBufSize<0){
		    	actualBufSize = 65536-readIndex+ writeIndex;
		    }
		    
//		    if(decoder_isStopped && actualBufSize > START_FILL){
			    lock.notify();
//		    }
		    
		    // SEQNO is stored in a short an come back to 0 when equal to 65536 (2 bytes)
		    if(writeIndex == 65536){
		    	writeIndex = 0;
		    }
		}
	}
	
	private static byte[] packetBuffer = new byte[MAX_PACKET];
	/**
	 * Decrypt and decode the packet.
	 * @param data
	 * @param outbuffer the result
	 * @return
	 */
	private int alac_decode(byte[] data, int offset, int length, int[] outbuffer){		
//		byte[] packet = new byte[MAX_PACKET];
		
		// Init AES
		initAES();
		
	    int i;
	    int fragment = length % 16;
	    try {
	        cipher.doFinal(data, offset, length - fragment, packetBuffer);
        } catch (Exception e) {
	        e.printStackTrace();
        }
	    for (i = length - fragment; i < length; i++) {
	    	packetBuffer[i] = data[i + offset];
	    }
	    
	    int outputsize = 0;
	    if (NATIVE) {
	    	outputsize = Alac.decodeFrame(packetBuffer, length, outbuffer);
	    } else {
		    outputsize = AlacDecodeUtils.decode_frame(session.getAlac(), packetBuffer, outbuffer, outputsize);
	    }
	    	    
	    assert outputsize==session.getFramePerPacket()*4;						// FRAME_BYTES length
	    
		return outputsize;
	}
	
	/**
	 * Initiate the cipher	
	 */
	private void initAES(){
		// Init AES encryption
		try {
			if (k == null) {
				k = new SecretKeySpec(session.getAESKEY(), "AES");
				cipher = Cipher.getInstance("AES/CBC/NoPadding");
				cipher.init(Cipher.DECRYPT_MODE, k, new IvParameterSpec(session.getAESIV()));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
