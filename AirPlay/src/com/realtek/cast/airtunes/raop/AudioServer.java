/**
 * The class that process audio data
 * 
 * @author bencall
 */

package com.realtek.cast.airtunes.raop;

import android.util.Log;

import com.realtek.cast.AirService;
import com.realtek.cast.airtunes.AirTunes;
import com.realtek.cast.airtunes.AirTunesPlayer;
import com.realtek.cast.airtunes.raop.UDPReceiver.OnPacketListener;
import com.realtek.cast.control.PlaybackControl;
import com.realtek.cast.util.NtpTime;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;


/**
 * Main class that listen for new packets.
 * 
 * @author bencall
 */
public class AudioServer {
	
	private static final String TAG = AirService.TAG;
	private static final boolean VERBOSE = false;
	
	public static final boolean JAVA_BUFFER = false;
	
	// Constantes
	public static final int BUFFER_FRAMES = 512;	// Total buffer size (number of frame)
	public static final int START_FILL = 282;		// Alac will wait till there are START_FILL frames in buffer
	public static final int MAX_PACKET = 2048;		// Also in UDPListener (possible to merge it in one place?)
	
	// Sockets
	private DatagramSocket mSockPacket, mSockContrl, mSockTiming;
	private UDPReceiver mRecvPacket;
	private UDPReceiver mRecvContrl;
	private UDPReceiver mRecvTiming;
	private Thread mTimingRequester;
	
   
	// client address
	private final InetAddress mRtpClient;
	private final int mRemoteTimingPort;
	private final int mRemoteContrlPort;

	// Audio infos and datas
	private final AudioSession mSession;
	private AudioBuffer audioBuf;

    // The audio player
	private AudioPlayer mPlayer;
    
	public static AudioServer create(AudioSession session, InetAddress iaddr, int controlPort, int timingPort) throws Exception {
		AudioServer server = new AudioServer(session, iaddr, controlPort, timingPort);
		try {
			server.createPlayer();
	        server.openSocket();
        } catch (SocketException e) {
        	server.close();
	        throw e;
        }
		return server;
	}
	
	private AudioServer(AudioSession session, InetAddress addr, int controlPort, int timingPort){
		mSession = session;
		mRtpClient = addr;
		mRemoteTimingPort = timingPort;
		mRemoteContrlPort = controlPort;
		
	}
	
	private void createPlayer() throws Exception {
		// Init functions
		if (JAVA_BUFFER) {
			audioBuf = new AudioBuffer(mSession, this);
			mPlayer = new PCMPlayer(this, mSession, audioBuf);
		} else {
			mPlayer = AirTunesPlayer.create(this, mSession);
		}
	}
	
	private void openSocket() throws SocketException {
		// Open sockets
		mSockPacket = new DatagramSocket();
		mSockPacket.setReceiveBufferSize(AirTunes.SAMPLE_RATE * 16);
		mSockContrl = new DatagramSocket();
		mSockTiming = new DatagramSocket();
		
		mRecvPacket = new UDPReceiver(mSockPacket, mOnAudioPacket);
		mRecvContrl = new UDPReceiver(mSockContrl, mOnControlPacket);
		mRecvTiming = new UDPReceiver(mSockTiming, mOnTimingPacket);
		mRecvPacket.start();
		mRecvContrl.start();
		mRecvTiming.start();
		
		mTimingRequester = new Thread(mRunUpdateTiming);
		mTimingRequester.start();
	}
	
	public void start(long initialTimestamp) {
		mPlayer.play(initialTimestamp);
    }

	public void close(){
		Log.v(TAG, "AudioServer closed");
		// Close the audio sockets.
		// This should stop the UDP receivers as well.
		if (mSockPacket != null) {
			mSockPacket.close();
		}
		if (mSockContrl != null) {
			mSockContrl.close();
		}
		if (mSockTiming != null) {
			mSockTiming.close();
		}
		
		// Release player
		mPlayer.stopAndRelease();
	}
	
	public void flush(){
		if (JAVA_BUFFER) {
			audioBuf.flush();
		} else {
			mPlayer.flush();
		}
	}
	
	public void setVolume(double vol){
		if (JAVA_BUFFER) {
//			mPlayer.setVolume(vol);
		} else {
			PlaybackControl.getInstance().adjustVolume(vol, -30D, 0D, -144D);
		}
	}
	
	/**
	 * Return the server port for the bonjour service
	 * @return
	 */
	public int getServerPort() {
		return mSockPacket.getLocalPort();
	}
	
	public int getControlPort() {
		return mSockContrl.getLocalPort();
	}
	
	public int getTimingPort() {
		return mSockTiming.getLocalPort();
	}
	
	private static byte[] packetBuffer = new byte[4096];
	
//	private int mCurrentSeq = -1;
//	private int mSegmentCount = 0;
//	private ByteBuffer mBuffer = ByteBuffer.allocate(16384);
	
	private final OnPacketListener mOnAudioPacket = new OnPacketListener() {

		@Override
		public void onPacketReceived(DatagramSocket socket, DatagramPacket packet) {
			byte[] data = packet.getData();
			int length = packet.getLength();
			ByteBuffer bf = ByteBuffer.wrap(data, 0, length);
			int type = data[1] & 0x7F;
			if (type == 0x60 || type == 0x56) { // audio data / resend
				// Decale de 4 bytes supplementaires
				int off = 0;
				if (type == 0x56) {
					off = 4;
				}
	
				// seqno is on two byte
	//			int seqno = ((data[2 + off] & 0xff) * 256 + (packet.getData()[3 + off] & 0xff));
				int seqno = bf.getShort(2) & 0xFFFF;
				long timestamp = bf.getInt(4) & 0xFFFFFFFFL;
	
				int offset = off + 12;
				if (JAVA_BUFFER) {
					audioBuf.putPacketInBuffer(seqno, data, offset, length - offset);
				} else {
//					if (mCurrentSeq == seqno) {
//						mBuffer.put(data, offset, length - offset);
//						mSegmentCount++;
//					} else {
//						if (mCurrentSeq >= 0) {
//							int size = mSession.decodeFrame(mBuffer.array(), 0, mBuffer.position(), packetBuffer);
////							if (mCurrentSeq % 128 == 0) {
//								Log.d(TAG, String.format("AudioPacket: seq=%d, segment=%d", mCurrentSeq, mSegmentCount));
////							}
//							mPlayer.write(timestamp, mCurrentSeq, packetBuffer, 0, size);
//							mSegmentCount = 0; 
//						}
//						
//						mBuffer.rewind();
//						mBuffer.put(data, offset, length - offset);
//						mCurrentSeq = seqno;
//						mSegmentCount++;
//					}
					
					int size = mSession.decodeFrame(data, offset, length - offset, packetBuffer);
					mPlayer.write(timestamp, seqno, packetBuffer, 0, size);
//					Log.v(TAG, String.format("data: seq = %d, timestamp = %d", seqno, timestamp));
				}
			} else {
				Log.w(TAG, String.format("Unhandled packeg: payload type = %d", type));
			}
		}

		@Override
        public void onSocketClosed(DatagramSocket socket) {
	        
        }
		
	};
	
	private OnPacketListener mOnControlPacket = new OnPacketListener() {
		
		@Override
		public void onPacketReceived(DatagramSocket socket, DatagramPacket packet) {
			byte[] data = packet.getData();
			int offset = packet.getOffset();
			int length = packet.getLength();
			ByteBuffer bf = ByteBuffer.wrap(data, offset, length);
			
			int type = bf.get(1) & 0x7F;
			// Sync packet
			if (type == 84 && length == 20) {
				NtpTime ntp = NtpTime.create(bf, 8);
				double ntptime = ntp.getTime();
				long timestamp = bf.getInt(16) & 0xFFFFFFFFL;
//				Log.v(TAG, String.format("ntpTime = %f, timestamp = %d, time=%s", ntptime, timestamp, ntp.getDate().toGMTString()));
				mPlayer.setFrameTime(timestamp, ntptime);
			}
			
		}

		@Override
        public void onSocketClosed(DatagramSocket socket) {
	        
        }
	};
	
	/**
	 * On Receiving Timing Packet.
	 */
	private OnPacketListener mOnTimingPacket = new OnPacketListener() {
		
		@Override
		public void onPacketReceived(DatagramSocket socket, DatagramPacket packet) {
			ByteBuffer bf = ByteBuffer.wrap(packet.getData(), packet.getOffset(), packet.getLength());
			double orig = NtpTime.getTimeSecond(bf, 8);
			double recv = NtpTime.getTimeSecond(bf, 16);
			double tran = NtpTime.getTimeSecond(bf, 24);
			double curr = NtpTime.currentTimeSecond();
			
			double delay = (curr - orig) - (tran - recv);
			double offst = ((recv - orig) + (tran - curr)) / 2;/* remote = local + offset */
			if (VERBOSE) {
				Log.v(TAG, String.format("Received timing packet: offset = %f, delay = %f", offst, delay));
			}
			mPlayer.setRemoteTimeOffset(offst);
		}

		@Override
        public void onSocketClosed(DatagramSocket socket) {
	        
        }
	};
	
	/**
	 * Request timing packet
	 */
	private Runnable mRunUpdateTiming = new Runnable() {
		
		@Override
		public void run() {
			// Initialize timing packet
			byte[] buffer = new byte[32];
			ByteBuffer buf = ByteBuffer.wrap(buffer);
			buf.put((byte) 0x80);
			buf.put((byte) (82 | 0x80));
			buf.putShort((short) 7);
			buf.putInt(0);// timestamp
			
			NtpTime time = NtpTime.now();
			DatagramPacket packet = null;
			packet = new DatagramPacket(buffer, 0, buffer.length, mRtpClient, mRemoteTimingPort);
			
			try {
				while (true) {
					// Sequence number
					buf.putShort(2, (short) 7);/* Why Fixed 7.....A___A */
					
					// Put zero timestamp
					buf.putLong(8, 0);
					buf.putLong(16, 0);
					
					// Put current time
					time.setToCurrent();
					time.writeToBuffer(buf, 24);
					
					mSockTiming.send(packet);
					
					// Request every 3 seconds. 
					try {
	                    Thread.sleep(3000);
                    } catch (InterruptedException e) {
                    }
				}
			} catch (IOException e) {
			}
				
		}
	};
	
	/**
	 * Not functional.
	 * @param first
	 * @param last
	 */
	public void requestResend(int first, int last) {
		Log.d("ShairPort", "Resend Request: " + first + "::" + last);
		if(last<first){
			return;
		}
		
		int len = last - first + 1;
	    byte[] request = new byte[] { (byte) 0x80, (byte) (0x55|0x80), 0x01, 0x00, (byte) ((first & 0xFF00) >> 8), (byte) (first & 0xFF), (byte) ((len & 0xFF00) >> 8), (byte) (len & 0xFF)};
	    
		try {
			DatagramPacket temp = new DatagramPacket(request, request.length, mRtpClient, mRemoteContrlPort);
			mSockContrl.send(temp);
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		
	}
	
}
