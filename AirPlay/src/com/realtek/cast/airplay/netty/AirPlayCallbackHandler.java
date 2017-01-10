
package com.realtek.cast.airplay.netty;

import android.util.Log;

import com.dd.plist.NSDictionary;
import com.realtek.cast.control.PlaybackControl;
import com.realtek.cast.control.PlaybackControl.OnPhotoChangedListener;
import com.realtek.cast.control.PlaybackControl.OnVideoChangedListener;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpVersion;

import java.util.Date;

public class AirPlayCallbackHandler extends BaseCallbackHandler implements OnVideoChangedListener, OnPhotoChangedListener {
	
	private static final String CATEGORY_VIDEO = "video";
	private static final String CATEGORY_PHOTO = "photo";
	private static final String CATEGORY_SLIDESHOW = "slideshow";

	private volatile int mLastVideoStatus = -1;
	private volatile int mLastPhotoStatus = -1;

	public AirPlayCallbackHandler(ChannelHandlerContext ctx, int sessionIdx, String appleSessionId, String deviceId) {
		super(ctx, sessionIdx, appleSessionId, deviceId);
	}

	public void disconnect() {
		super.disconnect();
		sendVideoState(PlaybackControl.VIDEO_STOPPED);
		mContext.close();
	}

	@Override
	public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
		super.handlerAdded(ctx);
		PlaybackControl.getInstance().registerVideoCallback(this, false);
		PlaybackControl.getInstance().registerPhotoCallback(this, false);
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		super.channelInactive(ctx);
		PlaybackControl.getInstance().unregisterVideoCallback(this);
		PlaybackControl.getInstance().unregisterPhotoCallback(this);
		removeCallbackEventSession(mDeviceId);
	}

	@Override
	public void onVideoStateChanged(int state) {
		sendVideoState(state);
	}
	
	@Override
    public void onPhotoStateChange(int stat) {
	    sendPhotoState(stat);
    }

	@Override
    public void onSlideShowStateChange(int stat, int assetId, int lastAssetId) {
	    sendSlideState(stat, lastAssetId);
    }

	public void sendVideoState(int state) {
		if (mLastVideoStatus == state) {
			return;
		}
		mLastVideoStatus = state;
		switch (state) {
			case PlaybackControl.VIDEO_LOADING:
				sendEvent(CATEGORY_VIDEO, "loading");
				break;
			case PlaybackControl.VIDEO_PLAYING:
				sendEvent(CATEGORY_VIDEO, "playing");
				break;
			case PlaybackControl.VIDEO_PAUSED:
				sendEvent(CATEGORY_VIDEO, "paused");
				break;
			case PlaybackControl.VIDEO_STOPPED:
				sendEvent(CATEGORY_VIDEO, "stopped");
				break;
			default:
				break;
		}
	}
	
	public void sendPhotoState(int state) {
		if (mLastPhotoStatus == state) {
			return;
		}
		mLastPhotoStatus = state;
		switch (state) {
			case PlaybackControl.PHOTO_STOPPED:
				sendEvent(CATEGORY_PHOTO, "stopped");
				break;
			default:
				break;
		}
	}
	
	public void sendSlideState(int state, int assetId) {
		switch (state) {
			case PlaybackControl.SLIDESHOW_LOADING:
				sendSlideEvent(CATEGORY_SLIDESHOW, "loading", assetId);
				break;
			case PlaybackControl.SLIDESHOW_PLAYING:
				sendSlideEvent(CATEGORY_SLIDESHOW, "playing", assetId);
				break;
			case PlaybackControl.SLIDESHOW_STOPPED:
				sendSlideEvent(CATEGORY_SLIDESHOW, "stopped", assetId);
				break;
			default:
				break;
		}
	}

	private void sendEvent(String category, String event) {
		if (VERBOSE) {
			Log.v(TAG, String.format("Send %s event to %s: %s", category, mDeviceId, event));
		}
		// ================
		// Send event
		NSDictionary dict = new NSDictionary();
		dict.put("category", category);
		dict.put("sessionID", mSessionIdx);
		dict.put("state", event);
		String xml = dict.toXMLPropertyList();
		byte[] contents = xml.getBytes();

		DefaultFullHttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1,
		        HttpMethod.POST, "/event");
		request.headers().set("Date", DATE_FORMAT.format(new Date()));
		HttpHeaders.addIntHeader(request, "Content-Length", contents.length);
		request.headers().set("Content-Type", MIME_PLIST);
		if (mSessionId != null) {
			request.headers().add(APPLE_SESSION_ID, mSessionId);
		}
		request.content().writeBytes(contents);
		mContext.writeAndFlush(request);
	}
	
	private void sendSlideEvent(String category, String event, int lastAssetId) {
		if (VERBOSE) {
			Log.v(TAG, String.format("Send %s event to %s: %s, asset=%d", category, mDeviceId, event, lastAssetId));
		}
		// ================
		// Send event
		NSDictionary dict = new NSDictionary();
		dict.put("category", category);
		if (lastAssetId >= 0) {
			dict.put("lastAssetID", lastAssetId);
		} else {
			dict.put("lastAssetID", 0);
		}
		dict.put("sessionID", mSessionIdx);
		dict.put("state", event);
		String xml = dict.toXMLPropertyList();
		byte[] contents = xml.getBytes();

		DefaultFullHttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1,
		        HttpMethod.POST, "/event");
		request.headers().set("Date", DATE_FORMAT.format(new Date()));
		HttpHeaders.addIntHeader(request, "Content-Length", contents.length);
		request.headers().set("Content-Type", MIME_PLIST);
		if (mSessionId != null) {
			request.headers().add(APPLE_SESSION_ID, mSessionId);
		}
		request.content().writeBytes(contents);
		mContext.writeAndFlush(request);
	}

}
