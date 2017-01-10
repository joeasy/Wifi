package com.realtek.cast.control;

import android.media.MediaCodec;
import android.media.MediaCodec.BufferInfo;
import android.media.MediaFormat;
import android.util.Log;
import android.view.SurfaceHolder;

import com.realtek.cast.control.PlaybackControl.OnMirrorChangedListener;

import java.nio.ByteBuffer;

public class MirrorContext {
	
	private static final String TAG = "Mirroring";
	private static final boolean VERBOSE = true;
	private final OnMirrorChangedListener mListener;
	private int mStat;
	
	private MediaCodec mCodec;
	private SurfaceHolder mDisplay;
	private MediaFormat mFormat;
	
	private ByteBuffer[] mInputBuffers;

	MirrorContext(OnMirrorChangedListener listener) {
		mListener = listener;
    }

	public synchronized boolean hasSession() {
	    return mStat != PlaybackControl.MIRROR_STOPPED;
    }

	public synchronized void initialize() {
		// TODO: release previous
		
		
	    mStat = PlaybackControl.MIRROR_INIT;
	    mCodec = MediaCodec.createDecoderByType("video/avc");
	    
	    mListener.onMirrorStateChanged(mStat);
    }
	
	public synchronized void setup(int width, int height, byte[] sps, byte[] pps) {
		mFormat = MediaFormat.createVideoFormat("video/avc", width, height);
//		ByteBuffer bufSps = ByteBuffer.allocate(sps.length + 4);
//		bufSps.putInt(1);
//		bufSps.put(sps);
//		bufSps.rewind();
//		ByteBuffer bufPps = ByteBuffer.allocate(pps.length + 4);
//		bufPps.putInt(1);
//		bufPps.put(pps);
//		bufPps.rewind();
//		mFormat.setByteBuffer("csd-0", bufSps);
//		mFormat.setByteBuffer("csd-1", bufPps);
		
		
		ByteBuffer buf = ByteBuffer.allocate(sps.length + 4 + pps.length + 4);
		buf.putInt(1);
		buf.put(sps);
		buf.putInt(1);
		buf.put(pps);
		buf.rewind();
		mFormat.setByteBuffer("csd-0", buf);
		
		mFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, width * height);
//		mFormat.setInteger("durationUs", 63446722);
		
		
		checkSetting_l();
		
	}

	public synchronized void registerDisplay(SurfaceHolder holder) {
	    mDisplay = holder;
	    
	    checkSetting_l();
    }
	
	private void checkSetting_l() {
		assert(mStat == PlaybackControl.MIRROR_INIT);
		
		if (mFormat != null && mDisplay != null) {
			mCodec.configure(mFormat, mDisplay.getSurface(), null, 0);
			mCodec.start();
			new PlayerThread(mCodec).start();
			mInputBuffers = mCodec.getInputBuffers();
			if (VERBOSE) {
				Log.v(TAG, "Codec configured");
			}
			
			mStat = PlaybackControl.MIRROR_PLAYING;
			mListener.onMirrorStateChanged(mStat);
		}
	}
	
	public synchronized void writeData(ByteBuffer buffer) {
		if (mStat != PlaybackControl.MIRROR_PLAYING) {
			Log.w(TAG, "writeData: invalid state");
			return;
		}
		
		for(;;) {
			int idx = mCodec.dequeueInputBuffer(10000);
			if (idx >= 0) {
				ByteBuffer input = mInputBuffers[idx];
				int length = buffer.remaining();
				input.rewind();
				input.put(buffer);
				mCodec.queueInputBuffer(idx, 0, length, 0, 0);
				Log.d(TAG, "queueInputBuffer: length=" + length);
				break;
			} else {
				Log.w(TAG, "queueInputBuffer failed: idx=" + idx);
			}
		}
    }

	private static class PlayerThread extends Thread {
		
		private final MediaCodec mCodec;
		private ByteBuffer[] mOutputBuffers;
		
		public PlayerThread(MediaCodec codec) {
			super("Player");
			mCodec = codec;
			mOutputBuffers = mCodec.getOutputBuffers();
		}

		@Override
        public void run() {
	        super.run();
			final long startMs = System.currentTimeMillis();
			BufferInfo info = new BufferInfo();
			while (!isInterrupted()) {
				int idx = mCodec.dequeueOutputBuffer(info, 100000);
				switch (idx) {
					case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED:
						Log.d(TAG, "INFO_OUTPUT_BUFFERS_CHANGED");
						mOutputBuffers = mCodec.getOutputBuffers();
						break;
					case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
						Log.d(TAG, "New format " + mCodec.getOutputFormat());
						break;
					case MediaCodec.INFO_TRY_AGAIN_LATER:
						Log.d(TAG, "dequeueOutputBuffer timed out!");
						break;
					default:
						ByteBuffer buffer = mOutputBuffers[idx];
						Log.v(TAG, "We can't use this buffer but render it due to the API limit, "
						        + buffer);

						// We use a very simple clock to keep the video FPS, or
						// the video
						// playback will be too fast
						while (info.presentationTimeUs / 1000 > System.currentTimeMillis()
						        - startMs) {
							try {
								sleep(10);
							} catch (InterruptedException e) {
								e.printStackTrace();
								break;
							}
						}
						mCodec.releaseOutputBuffer(idx, true);
						break;
				}
			}
		}

	}
}
