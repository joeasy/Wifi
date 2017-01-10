package com.realtek.cast.airtunes.raop;



public abstract class AudioPlayer {
	
	protected final AudioServer mServer;

	public AudioPlayer(AudioServer server) {
		mServer = server;
    }

	abstract public void play(long timestamp);
	
	abstract public void stopAndRelease();

	abstract public void flush();
	
	abstract public void write(long timestamp, int seqno, byte[] data, int offset, int length);

	public void setFrameTime(long timestamp, double ntpTime) {
	    
    }

	public void setRemoteTimeOffset(double timeOffset) {
	    
    }

}
