package com.realtek.cast.airtunes.raop;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

public class UDPReceiver extends Thread{
	
	public interface OnPacketListener {
		public void onPacketReceived(DatagramSocket socket, DatagramPacket packet);

		public void onSocketClosed(DatagramSocket socket);
	}
	
	public static final int MAX_PACKET = 2048;

	private final DatagramSocket socket;
	private final OnPacketListener delegate;
	
	public UDPReceiver(DatagramSocket socket, OnPacketListener delegate){
		super();
		this.socket = socket;
		this.delegate = delegate;
	}
	
	public void run() {
		byte[] buffer = new byte[MAX_PACKET];
		DatagramPacket p = new DatagramPacket(buffer, buffer.length);
		try {
			while (!socket.isClosed()) {
				socket.receive(p);
				delegate.onPacketReceived(socket, p);
			}
		} catch (IOException e) {
		}
	}

	public void close() {
		if (!socket.isClosed()) {
			socket.close();
		}
	}
}
