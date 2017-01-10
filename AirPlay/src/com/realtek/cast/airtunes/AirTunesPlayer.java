package com.realtek.cast.airtunes;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.util.Log;

import com.realtek.cast.airtunes.raop.AudioPlayer;
import com.realtek.cast.airtunes.raop.AudioServer;
import com.realtek.cast.airtunes.raop.AudioSession;


import java.util.Arrays;

public class AirTunesPlayer extends AudioPlayer {
	
	private static final boolean VERBOSE = false;
	
	private static final String TAG = "AirTunesPlayer";
	
	private final AudioTrack mAudioTrack;
	private final int mFramesPerPacket;
	private final int mFrameSize;
	private final int mMaxFrameGap;
	private final int mAudioDelay;
	
	
	// Access by player thread
	private int mSeqno = -1;
	private long mGapFrames = 0L;
	private long mFrameCounter = 0L;
	private long mLastTimestampInTrack;
	
	private byte[] mSlienceFrame;

	// Lock protected variables
	private final Object mSync = new Object();
	private boolean mToSync = false;
	private double mSyncTime;
	private long mSyncTimestamp;
	
	// NTP time
	/**
	 * This is an NTP Timestamp as described in RFC 3450 Section 4. and RFC 1305.
	 * The timestamps used by iTunes and the device seems to come from a monotonic clock which starts at 0 when they just started/booted.
	 * This monotonic clock's origin of time is the unix epoch, which corresponds to 0x83aa7e80 seconds in NTP time.
	 * 
	 * Remote time offset in seconds.
	 */
	double mRemoteTimeOffset;

	private boolean mMute;
	
	public static AirTunesPlayer create(AudioServer server, AudioSession session) throws Exception {
		AudioTrack track = new AudioTrack(
				AudioManager.STREAM_MUSIC,
				AirTunes.SAMPLE_RATE,
		        AudioFormat.CHANNEL_OUT_STEREO,
		        AudioFormat.ENCODING_PCM_16BIT,
		        AirTunes.SAMPLE_RATE * 2 * 4,
		        AudioTrack.MODE_STREAM);
		if (track.getState() != AudioTrack.STATE_INITIALIZED) {
			throw new Exception("AudioTrack not initialized");
		}
		return new AirTunesPlayer(track, server, session);
	}
	
	private AirTunesPlayer(AudioTrack track, AudioServer server, AudioSession session) {
		super(server);
		// Create AudioTrack
		mAudioTrack = track;
		
		mFrameSize = 4;/* trackCount * PCM_16BIT */
		mFramesPerPacket = session.getFramePerPacket();
		mMaxFrameGap = (int) (AirTunes.SAMPLE_RATE * AirTunes.TOLERANCE_DELAY);
		int packetSize = mFramesPerPacket * 4;
		
		mSlienceFrame = new byte[packetSize*128];
		Arrays.fill(mSlienceFrame, (byte) 0);
		
		mRemoteTimeOffset = System.currentTimeMillis() / 1000 - 0x83aa7e80;
		mAudioDelay = session.getAudioDelay();
	}
	
	@Override
	public void play(long timestamp) {
		if (mAudioTrack.getState() == AudioTrack.STATE_INITIALIZED) {
			mAudioTrack.play();
		} else {
			Log.e(TAG, "track not intialized");
		}
	}
	
	@Override
	public void stopAndRelease() {
		mFrameCounter = 0;
		mAudioTrack.release();
		Log.v(TAG, "AudioTrack release");
	}

	@Override
	public void flush() {
		Log.v(TAG, "Flush");
		mAudioTrack.pause();
		mAudioTrack.flush();
		mAudioTrack.play();
		mSeqno = -1;
		mGapFrames = 0L;
		mLastTimestampInTrack = -1;
		mFrameCounter = 0;
		synchronized (mSync) {
			mToSync = false;
        }
	}
	
	@Override
	public void write(long timestamp, int seqno, byte[] data, int offset, int length) {
		if (mAudioTrack.getPlayState() != AudioTrack.PLAYSTATE_PLAYING) {
			return;
		}
		writeAudioTrack(timestamp, seqno, data, offset, length);
	}
	
	public void writeAudioTrack(long timestamp, int seqno, byte[] data, int offset, int length) {
		// Update gap frames
		synchronized (mSync) {
			if (mToSync && mLastTimestampInTrack >= 0) {
				
				// Get current playback timestamp
				long position = mAudioTrack.getPlaybackHeadPosition() & 0xFFFFFFFFL;
				long framesInTrack =  mFrameCounter - position;
				long playbackTimestamp = mLastTimestampInTrack - framesInTrack;
				
				// Get target playback timestamp based on current time
				double playbackTime = ((double) System.currentTimeMillis()) / 1000D + mRemoteTimeOffset + 2208988800D;
				double diffTime = mSyncTime - playbackTime;
				if (Math.abs(diffTime) <= 3) {
					long targetPlaybackTimestamp = (long) (mSyncTimestamp - (diffTime * AirTunes.SAMPLE_RATE));
					
					// 
					mGapFrames = playbackTimestamp - targetPlaybackTimestamp + mAudioDelay;
					
//					if (VERBOSE) {
//						Log.v(TAG, String.format("FramesInTrack = %d, position = %d, Time difference = %f, Gap = %d",
//								framesInTrack, position, diffTime, mGapFrames));
//					}
					
				} else {
					Log.w(TAG, "Time difference too large: " + diffTime);
				}
				
				// Mute if the gap is too large
				if (Math.abs(mGapFrames) >= AirTunes.SAMPLE_RATE * 0.1) {
					if (!mMute) {
						Log.i(TAG, "mute");
						mAudioTrack.setStereoVolume(0F, 0F);
						mMute = true;
					}
				} else if (mMute) {
					Log.i(TAG, "unmute");
					mAudioTrack.setStereoVolume(1F, 1F);
					mMute = false;
				}
				mToSync = false;
			}
		} // lock
		
		// Expected frames arrived
		if (mSeqno == seqno || mSeqno < 0) {
			if (mGapFrames <= mMaxFrameGap && mGapFrames >= -mMaxFrameGap) {
				appendFrames(data, offset, length);
				
			// We are too fast.
			} else if (mGapFrames >= 0) {
				appendFrames(data, offset, length);
				
				//  Append silence frames
				long slience = mGapFrames;
				if (VERBOSE) {
					Log.v(TAG, String.format("We're too fast %d: append %d slience frams", mGapFrames, slience));
				}
				appendSilence(slience);
				mGapFrames -= slience;
				
			} else {
				// We are delayed. drop some frames
				int drops = (int) Math.min(length / mFrameSize, -mGapFrames / 2);
				if (VERBOSE) {
					Log.v(TAG, String.format("We're delayed %d frams: drops %d frams", mGapFrames, drops));
				}
				mGapFrames += drops;
				appendFrames(data, offset, (int) (length - drops * mFrameSize));
			}
			
		// Missing packet
		} else if (seqno > mSeqno) {
			int missedFrames = (seqno - mSeqno) * mFramesPerPacket;
			
			if (mGapFrames <= mMaxFrameGap && mGapFrames >= -mMaxFrameGap) {
				Log.w(TAG, String.format("Missing packet: wait=%d, recv=%d. We were sync.", mSeqno, seqno));
				appendSilence(missedFrames);
				appendFrames(data, offset, length);
				
			// We are too fast. 
			} else if (mGapFrames >= 0) {
				Log.w(TAG, String.format("Missing packet: wait=%d, recv=%d. We were too fast. append %d + %d slience", mSeqno, seqno, mGapFrames, missedFrames));
				// Append extra silence frames
				appendSilence(mGapFrames + missedFrames);
				mGapFrames = 0;
				
				appendFrames(data, offset, length);
				
			} else {
				Log.w(TAG, String.format("Missing packet: wait=%d, recv=%d. We were delayed", mSeqno, seqno));
				// We are delayed and some packet are missed. Just drops them.
				mGapFrames += missedFrames;
				appendFrames(data, offset, length);
			}
			
		} else if (seqno < mSeqno) {
			Log.w(TAG, String.format("Dropped packet: wait=%d, recv=%d ", mSeqno, seqno));
		}
		
		mLastTimestampInTrack = timestamp;// small -> fast; large -> delay
		mSeqno = seqno + 1;
		if (mSeqno >= 65536) {
			mSeqno = 0;
		}
	}
	
	private long appendFrames(byte[] data, int offset, int length) {
		assert(length % mFrameSize == 0);
	
		int frames = length / mFrameSize;
		assert(frames == 352);
		mAudioTrack.write(data, offset, length);
		mFrameCounter += frames;
		return frames;
	}
	
	private long appendSilence(final long frames) {
		if (frames == 0) {
			return 0;
		}
		long remain = frames * mFrameSize;
		while (remain > 0) {
			int n = (int) Math.min(mSlienceFrame.length, remain);
			mAudioTrack.write(mSlienceFrame, 0, n);
			remain -= n;
		}
		mFrameCounter += frames;
		return frames;
	}

	@Override
	public void setFrameTime(long timestamp, double ntpTime) {
//		Log.i(TAG, String.format("setFrameTime: timestamp %d, ntptime %f", timestamp, ntpTime));
		synchronized(mSync) {
			mToSync = true;
			mSyncTime = ntpTime;
			mSyncTimestamp = timestamp;
		}
	}
	
	@Override
	public void setRemoteTimeOffset(double timeOffset) {
		synchronized (mSync) {
//			Log.i(TAG, String.format("Timeoffset update: %f -> %f, diff = %f", mRemoteTimeOffset, timeOffset, timeOffset - mRemoteTimeOffset));
			if (Math.abs(timeOffset - mRemoteTimeOffset) > 1F) {
				mRemoteTimeOffset = timeOffset;
			} else {
				mRemoteTimeOffset = mRemoteTimeOffset * 0.4 + timeOffset * 0.6;
			}
        }
	}

}
