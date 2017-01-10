package com.realtek.cast.airplay.netty;

import android.util.Log;

import com.realtek.cast.airplay.AirPlay;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

abstract public class BaseAirPlayHandler extends ChannelInboundHandlerAdapter {
	
	protected static final String TAG = AirPlay.TAG;
	protected static final boolean VERBOSE = true;
	
	protected static final String APPLE_PURPOSE = "X-Apple-Purpose";
	protected static final String APPLE_SESSION_ID = "X-Apple-Session-ID";
	protected static final String APPLE_DEVICE_ID = "X-Apple-Device-ID";
	protected static final String APPLE_ASSET_ACTION = "X-Apple-AssetAction";
	protected static final String APPLE_ASSET_KEY = "X-Apple-AssetKey";
	
	//Date: Thu, 17 Apr 2014 03:37:50 GMT
	protected static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("EEE, dd MMM HH:hh:mm yyyy z", Locale.US);
	
	public static final Object MIME_APPLE_BINARY_PLIST = "application/x-apple-binary-plist";
	public static final String MIME_PLIST = 
			"text/x-apple-plist+xml";
//			"application/x-apple-plist";
	
	private static int sSessionCount = 0;
	private static final Map<String, String> sDevice2Session = new HashMap<String, String>();
	private static final Map<String, AirPlayCallbackHandler> sDevice2Reverse = new HashMap<String, AirPlayCallbackHandler>();
	
	protected static synchronized AirPlayCallbackHandler createCallbackEventSession(ChannelHandlerContext ctx, String device, String sessionId) {
		AirPlayCallbackHandler callback = new AirPlayCallbackHandler(ctx, ++sSessionCount, sessionId, device);
		sDevice2Reverse.put(device, callback);
		sDevice2Session.put(device, sessionId);
		return callback;
	}
	
	protected static synchronized void removeCallbackEventSession(String device) {
		sDevice2Reverse.remove(device);
		sDevice2Session.remove(device);
	}
	
	protected static synchronized void disconnectOtherConnections(HttpRequest request) {
		String device = request.headers().get(APPLE_DEVICE_ID);
		Set<String> keys = new HashSet<String>();
		keys.addAll(sDevice2Reverse.keySet());
		for (String key : keys) {
			if (!key.equals(device)) {
				sDevice2Reverse.get(key).disconnect();
				sDevice2Reverse.remove(key);
			}
		}
	}
	
	protected static synchronized void sendVideoStatus(HttpRequest request, int status) {
		String device = request.headers().get(APPLE_DEVICE_ID);
		if (device == null) {
			return;
		}
		
		AirPlayCallbackHandler handler = null;
		handler = sDevice2Reverse.get(device);
		if (handler == null) {
			return;
		}
		
		handler.sendVideoState(status);
	}
	// ==========================================================================================
	
	protected static synchronized DefaultFullHttpResponse createResponse(HttpResponseStatus status, HttpRequest request) {
		String device = request.headers().get(APPLE_DEVICE_ID);
		DefaultFullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, status);
		response.headers().add("Date", DATE_FORMAT.format(new Date()));
		if (device != null) {
			String session = sDevice2Session.get(device);
			if (session != null) {
				response.headers().add(APPLE_SESSION_ID, session);
			} else {
				Log.w(TAG, "SessionID not found for device: device=" + device + ", request=" + request.getUri());
			}
		}
		return response;
	}

	protected static void flushOk(ChannelHandlerContext ctx, HttpRequest request) {
		DefaultFullHttpResponse response = createResponse(HttpResponseStatus.OK, request);
		ctx.writeAndFlush(response).syncUninterruptibly();
	}
	
	protected static String getStringHardwareAdress(byte[] hwAddr) {
		StringBuilder sb = new StringBuilder();
		sb.append(String.format("%02x", hwAddr[0]));
		for (int i = 1; i < hwAddr.length; i++) {
			sb.append(String.format(":%02x", hwAddr[i]));
		}
		return sb.toString();
	}
	// ==========================================================================================
	
	@Override
    public final void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		if ((msg instanceof FullHttpRequest)) {
			FullHttpRequest request = (FullHttpRequest) msg;
			channelHttpRequest(ctx, request);
		} else if (msg instanceof FullHttpResponse) {
			FullHttpResponse response = (FullHttpResponse) msg;
			channelHttpResponse(ctx, response);
		} else {
			super.channelRead(ctx, msg);
		}
	}
	
	public void channelHttpRequest(ChannelHandlerContext ctx, FullHttpRequest request) throws Exception {
		super.channelRead(ctx, request);
	}
	
	public void channelHttpResponse(ChannelHandlerContext ctx, FullHttpResponse response) throws Exception {
		super.channelRead(ctx, response);
	}
	
	@Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		cause.printStackTrace();
	    super.exceptionCaught(ctx, cause);
    }
}
