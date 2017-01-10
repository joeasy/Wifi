package com.realtek.cast.airtunes;

import android.util.Log;

import com.realtek.cast.airtunes.raop.RTSPResponder;


import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.PrivateKey;
import java.util.Vector;


/**
 * LaunchThread class which starts services
 * 
 * @author bencall
 */
public class AirTunesServer extends Thread {
	
	private static final String TAG = AirTunes.TAG;
	
	BonjourAirTunes mEmitter;
	
	private String name;
	private final PrivateKey mKey;
	
	private ServerSocket mServSock;
	
	private Vector<RTSPResponder> mConnections = new Vector<RTSPResponder>();

	/**
	 * Constructor
	 * @param name
	 */
	public AirTunesServer(String name, PrivateKey key){
		super("air_tunes");
		this.name = name;
		mKey = key;
	}
	
	public void run(){
		int port = AirTunes.PORT;
		
		mServSock = null;
		try {
			// We listen for new connections
			do {
				try {
					mServSock = new ServerSocket(port);
				} catch (IOException e) {
					Log.w(TAG, String.format("Cannot open port at %d: %s",  port, e.toString()));
					port++;
				}
			} while(mServSock == null);

			// DNS Emitter (Bonjour)
			mEmitter = new BonjourAirTunes(name, port);
			mEmitter.start();
			
			while (true) {
				Socket socket = mServSock.accept();
				Log.d(TAG, "got connection from: " + socket.toString());
				RTSPResponder connection = new RTSPResponder(socket, mKey);
				connection.start();
				mConnections.add(connection);
			}
			
        } catch (IOException e1) {
	        e1.printStackTrace();
        } finally {
			if (mServSock != null) {
				try {
					mServSock.close();
				} catch (IOException e) {
				}
			}
			while (!mConnections.isEmpty()) {
				RTSPResponder connection = mConnections.remove(0);
				connection.close();
			}
			if (mEmitter != null) {
				mEmitter.stopBonjour();
			}
		}
		Log.d(TAG, "service stopped");
	}
	
	public void close(){
		if (mServSock != null) {
			try {
				mServSock.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
