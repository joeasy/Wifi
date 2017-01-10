package com.realtek.cast.airtunes.raop;

import android.net.Uri;
import android.util.Base64;
import android.util.Log;

import com.realtek.cast.control.PlaybackControl;
import com.realtek.cast.util.FairPlay;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.security.PrivateKey;
import java.util.Arrays;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.crypto.Cipher;


/**
 * An primitive RTSP responder for replying iTunes
 * 
 * @author bencall
 */
public class RTSPResponder extends Thread{
	
	private static final byte[] CRLFCRLF = {
	        '\r', '\n', '\r', '\n'
	};

	private Socket socket;					// Connected socket
	private PrivateKey mKey;

	private AudioServer mAudioServer; 				// Audio listener
	private AudioSession mAudioSession;
	
	final byte[] hwAddr;
	final byte[] ip;
	private InputStream mInput;
//	private BufferedReader in;
	
	public RTSPResponder(Socket socket, PrivateKey key) throws IOException {
		this.socket = socket;
		mInput = socket.getInputStream();
//		in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		InetAddress addr = socket.getLocalAddress();
		ip =  addr.getAddress();
		hwAddr = NetworkInterface.getByInetAddress(addr).getHardwareAddress();
		mKey = key;
	}


	public RTSPResponse handlePacket(RTSPPacket packet){
		// We init the response holder
		RTSPResponse response = new RTSPResponse("RTSP/1.0 200 OK");

		// Apple Challenge-Response field if needed
    	String challenge;
    	if( (challenge = packet.valueOfHeader("Apple-Challenge")) != null){
    		response.append("Audio-Jack-Status", "connected; type=analog");
    		response.append("CSeq", packet.valueOfHeader("CSeq"));
    		
    		// BASE64 DECODE
    		byte[] decoded = Base64.decode(challenge, Base64.DEFAULT);

    		ByteArrayOutputStream out = new ByteArrayOutputStream();
    		// Challenge
    		try {
				out.write(decoded);
				// IP-Address
				out.write(ip);
				// HW-Addr
				out.write(hwAddr);

				// Pad to 32 Bytes
				int padLen = 32 - out.size();
				for(int i = 0; i < padLen; ++i) {
					out.write(0x00);
				}

			} catch (IOException e) {
				e.printStackTrace();
			}

    		// RSA
    		byte[] crypted = this.encryptRSA(out.toByteArray());
    		
    		// Encode64
    		String ret = Base64.encodeToString(crypted, Base64.DEFAULT);
    		
    		// On retire les ==
	        ret = ret.replace("=", "").replace("\r", "").replace("\n", "");

    		// Write
        	response.append("Apple-Response", ret);
    	} 
    	
		// Paquet request
		String REQ = packet.getReq();
        if(REQ.contentEquals("OPTIONS")){
        	response.append("Audio-Jack-Status", "connected; type=analog");
    		response.append("CSeq", packet.valueOfHeader("CSeq"));
        	
        	// The response field
        	response.append("Public", "ANNOUNCE, SETUP, RECORD, PAUSE, FLUSH, TEARDOWN, OPTIONS, GET_PARAMETER, SET_PARAMETER");

        } else if (REQ.contentEquals("ANNOUNCE")){
        	response.append("Audio-Jack-Status", "connected; type=analog");
    		response.append("CSeq", packet.valueOfHeader("CSeq"));
        	
    		String payload = packet.getContentString();
    		mAudioSession = AudioSession.parse(payload, mKey);
        	
        } else if (REQ.startsWith("SETUP")){
        	PlaybackControl.getInstance().stopAudioSession();
        	
        	response.append("Audio-Jack-Status", "connected; type=analog");
    		response.append("CSeq", packet.valueOfHeader("CSeq"));
        	
    		try {
	        	int controlPort = 0;
	        	int timingPort = 0;
	        	InetAddress iaddr = null;
	        	
	        	String value = packet.valueOfHeader("Transport");        	
	        	
	        	// Control port
	        	Pattern p = Pattern.compile(";control_port=(\\d+)");
	        	Matcher m = p.matcher(value);
	        	if(m.find()){
	        		controlPort =  Integer.valueOf(m.group(1));
	        	}
	        	
	        	// Timing port
	        	p = Pattern.compile(";timing_port=(\\d+)");
	        	m = p.matcher(value);
	        	if(m.find()){
	        		timingPort =  Integer.valueOf(m.group(1));
	        	}
	        	
	        	// Address
	        	p = Pattern.compile("SETUP\\s(rtsp://.*?)\\s");
	        	m = p.matcher(packet.getRawPacket());
	        	if (m.find() && mAudioSession != null) {
	        		String uri = m.group(1);
	        		iaddr = InetAddress.getByName(Uri.parse(uri).getHost());
	        		
	        		// Creaet Audio Server
	        		mAudioServer = AudioServer.create(mAudioSession, iaddr, controlPort, timingPort);
	        	}
            
            } catch (SocketException e) {
	            e.printStackTrace();
            } catch (UnknownHostException e) {
	            e.printStackTrace();
            } catch (Exception e) {
	            e.printStackTrace();
            }
    		
    		if (mAudioServer != null) {
	            String transport = String.format(Locale.US, "RTP/AVP/UDP;unicast;mode=record;server_port=%d;control_port=%d;timing_port=%d",
	            		mAudioServer.getServerPort(), mAudioServer.getControlPort(), mAudioServer.getTimingPort());
	            response.append("Transport", transport);
	            
	            response.append("Session", "DEADBEEF");
    		} else {
    			// TODO: response not okay
    		}
        	
        } else if (REQ.contentEquals("RECORD")){
        	response.append("Audio-Jack-Status", "connected; type=analog");
    		response.append("CSeq", packet.valueOfHeader("CSeq"));
        	
    		if (mAudioServer != null) {
    			PlaybackControl.getInstance().startAudioSession(this);
    			
	        	/* Headers
	        	 * Range: ntp=0-
	        	 * RTP-Info: seq={Note 1};rtptime={Note 2}
	        	 * Note 1: Initial value for the RTP Sequence Number, random 16 bit value
	        	 * Note 2: Initial value for the RTP Timestamps, random 32 bit value
	        	 */
    			String value = packet.getRawPacket();
    			
    			// Control port
    			Pattern p = Pattern.compile(";rtptime=(\\d+)");
    			Matcher m = p.matcher(value);
    			long timestamp = 0;
    			if(m.find()){
    				timestamp =  Long.parseLong(m.group(1));
    			}
    			mAudioServer.start(timestamp);
    		}

        } else if (REQ.contentEquals("FLUSH")){
        	response.append("Audio-Jack-Status", "connected; type=analog");
    		response.append("CSeq", packet.valueOfHeader("CSeq"));
        	
        	mAudioServer.flush();
        
        } else if (REQ.contentEquals("TEARDOWN")){
        	response.append("Audio-Jack-Status", "connected; type=analog");
    		response.append("CSeq", packet.valueOfHeader("CSeq"));
        	
        	if (mAudioServer != null) {
        		mAudioServer.close();
        		mAudioServer = null;
        	}
        	if (mAudioSession != null) {
        		mAudioSession.close();
        		mAudioSession = null;
        	}
        	response.append("Connection", "close");
        	
        } else if (REQ.contentEquals("SET_PARAMETER")){
        	response.append("Audio-Jack-Status", "connected; type=analog");
    		response.append("CSeq", packet.valueOfHeader("CSeq"));
        	
        	// Timing port
        	String contentType = packet.valueOfHeader("Content-Type");
        	if ("text/parameters".equals(contentType)) {
        		String content = packet.getContentString();
        		// Volume control
        		Pattern p = Pattern.compile("volume:\\s?(.+)");
        		Matcher m = p.matcher(content);
        		if(m.find()){
        			double volume = Double.parseDouble(m.group(1));
        			mAudioServer.setVolume(volume);
        		} else {
        			p = Pattern.compile("progress:\\s?([0-9]+)/([0-9]+)/([0-9]+)");
        			m = p.matcher(content);
        			if (m.find()) {
        				long rtpTime = 0;
        				long start = Long.parseLong(m.group(1));
        				long curr = Long.parseLong(m.group(2));
        				long end = Long.parseLong(m.group(3));
        				PlaybackControl.getInstance().updateProgress(0, start, curr, end);
        			}
        		}
        		
        	// Cover Art
        	} else if ("image/jpeg".equals(contentType)) {
        		long rtpTime = 0;
        		PlaybackControl.getInstance().updateCoverArt(rtpTime, packet.getContent(), 0, packet.getContentLength());
        		
        	// Track info
        	} else if ("application/x-dmap-tagged".equals(contentType)) {
        		long rtpTime = 0;
        		String str = packet.getContentString();
        		PlaybackControl.getInstance().updateTrackInfo(rtpTime, packet.getContent(), 0, packet.getContentLength());
        	}
        	
        } else if (REQ.contains("POST")) {
        	String head = packet.getRawPacket();
        	if (head.contains("/fp-setup")) {
        		byte[] content = packet.getContent();
        		int seqno = content[6];
        		if (seqno == 1) {
        			response.append("Content-Type", "application/octet-stream");
        			response.append("X-Apple-ET", "32");
        			response.append("Content-Length", "142");
        			response.finalizeHeader();
        			
        			//payload
        			response.setContent(FairPlay.PACKET2, 0, FairPlay.PACKET2.length);
        			
        		} else if (seqno == 3) {
        			response.append("Content-Type", "application/octet-stream");
        			response.append("X-Apple-ET", "32");
        			response.append("Content-Length", "32");
        			response.finalizeHeader();
        			
        			//payload
        			byte[] payload = Arrays.copyOf(FairPlay.PACKET4, FairPlay.PACKET4.length);
        			System.arraycopy(content, content.length - 20, payload, payload.length - 20, 20);
        			response.setContent(payload, 0, payload.length);
        		}
        	}
        } else {
        	Log.d("ShairPort", "REQUEST(" + REQ + "): Not Supported Yet!");
        	Log.d("ShairPort", packet.getRawPacket());
        }
        
    	// We close the response
    	response.ensureHeaderFinialized();
    	return response;
	}
	
	private static final byte[] FP_REQ = {
	        0x46, 0x50, 0x4C, 0x59, 0x03, 0x01, 0x01, 0x00, 0x00, 0x00, 0x00, 0x04, 0x02, 0x00,
	        0x01, (byte) (0xBB & 0xFF)
	};
	
	private static final byte[] FP_RES = {
	        0x46, 0x50, 0x4C, 0x59, 0x03, 0x01, 0x02, 0x00, 0x00, 0x00, 0x00, (byte) (0x82 & 0xFF),
	        0x02, 0x01
	        
	};
	
	/**
	 * Crypts with private key
	 * @param array	data to encrypt
	 * @return encrypted data
	 */
	public byte[] encryptRSA(byte[] array){
		try{

	        // Encrypt
	        Cipher cipher = Cipher.getInstance("RSA/NONE/PKCS1Padding"); 
	        cipher.init(Cipher.ENCRYPT_MODE, mKey);
	        return cipher.doFinal(array);

		}catch(Exception e){
			e.printStackTrace();
		}

		return null;
	}

    /**
     * Thread to listen packets
     */
	public void run() {
		try {
//			char[] buffer = new char[4096];
			ByteBuffer buffer = ByteBuffer.allocate(4096);
			do {
//				StringBuffer packet = new StringBuffer();
				Log.d("ShairPort", "listening packets ... ");
//				// feed buffer until packet completed
//				
//				int ret = 0;
//				do {
//					ret = in.read(buffer);
//					packet.append(new String(buffer));
//				} while (ret!=-1 && !completedPacket.matcher(packet.toString()).find());
				buffer.rewind();
				int ret = readSegment(buffer, mInput, CRLFCRLF);
				
				
				if (ret!=-1) {
					// We handle the packet
					String packet = new String(buffer.array(), 0, ret);
					RTSPPacket request = new RTSPPacket(packet.toString());
					request.readContent(mInput);
					
					RTSPResponse response = this.handlePacket(request);		
					Log.d("ShairPort", request.toString());	
					Log.d("ShairPort", response.toString());
		
			    	// Write the response to the wire
			    	try {			
//			    		BufferedWriter oStream = new BufferedWriter(new OutputStreamWriter());
//			    		oStream.write(response.getRawPacket());
//			    		oStream.flush();
			    		OutputStream os = socket.getOutputStream();
			    		os.write(response.getRawPacket());
			    		os.flush();
					} catch (IOException e) {
						e.printStackTrace();
					}
					
		    		if("TEARDOWN".equals(request.getReq())){
		    			socket.close();
		    			socket = null;
		    		}
				} else {
	    			socket.close();
	    			socket = null;
				}
			} while (socket!=null);
			
		} catch (IOException e) {
			e.printStackTrace();
			
		} finally {
			try {
				if (mInput != null) mInput.close();
			} catch (IOException e) {
			}
			try {
				if (socket != null) socket.close();
			} catch (IOException e) {
			}
			if (mAudioServer != null) {
				mAudioServer.close();
			}
		}
		Log.d("ShairPort", "connection ended.");
	}
	
	public void close() {
		if (mAudioServer != null) {
			mAudioServer.close();
		}
		if (socket != null) {
			try {
				socket.close();
			} catch (IOException e) {
			}
		}
    }
	
	private static int readSegment(ByteBuffer dst, InputStream is, byte[] endToken) throws IOException {
		int read = 0;
		int idx = 0;
		while(true) {
			int r = is.read();
			if (r < 0) {
				return r;
			}
			read++;
			byte b = (byte) (r & 0xFF);
			dst.put(b);
			if (b == endToken[idx]) {
				idx++;
			} else {
				idx = 0;
			}
			if (idx == endToken.length) {
				break;
			}
		}
		return read;
	}

}