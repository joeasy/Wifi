package com.realtek.cast.airplay.netty;

import android.util.Log;

import com.dd.plist.BinaryPropertyListParser;
import com.dd.plist.NSData;
import com.dd.plist.NSDictionary;
import com.dd.plist.NSNumber;
import com.realtek.cast.control.PlaybackControl;
import com.realtek.cast.control.PlaybackControl.PhotoRequester;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;

public class AirPlayPhotoRequester extends BaseCallbackHandler implements PhotoRequester {

	public AirPlayPhotoRequester(ChannelHandlerContext ctx, String device, String sessionId) {
		super(ctx, 0, sessionId, device);
    }
	
	@Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
	    super.handlerAdded(ctx);
	    PlaybackControl.getInstance().registerPhotoRequester(this);
    }
	
	@Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
	    super.channelInactive(ctx);
	    PlaybackControl.getInstance().unregisterPhotoRequester(this);
    }

	@Override
    public void channelHttpResponse(ChannelHandlerContext ctx, FullHttpResponse response) throws Exception {
		if (VERBOSE) {
			Log.v(TAG, "Receive photo response: " + response.getStatus());
		}
		
		try {
			HttpResponseStatus status = response.getStatus();
			if (status.equals(HttpResponseStatus.OK)) {
				// TODO: Avoid allocating new buffer
				int size = response.content().readableBytes();
				byte[] buf = new byte[size];
				response.content().getBytes(0, buf);
				NSDictionary plist = (NSDictionary) BinaryPropertyListParser.parse(buf);
				
				NSData data = (NSData) plist.get("data");
				
				NSDictionary info = (NSDictionary) plist.get("info");
				NSNumber id = (NSNumber) info.get("id");
				String key = info.get("key").toString();
				
				PlaybackControl.getInstance().notifySlideShowImage(id.intValue(), key, data.bytes());
				
			} else {
				disconnect();
				
			}
			
        } catch (Exception e) {
	        e.printStackTrace();
        }
    }

	@Override
    public boolean requestPhoto(int assetId) {
		String path = "/slideshows/1/assets/1";//String.format(Locale.US, "/slideshows/1/assets/%d", assetId);
		DefaultFullHttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, path);
		HttpHeaders.addIntHeader(request, HttpHeaders.Names.CONTENT_LENGTH, 0);
		request.headers().add(HttpHeaders.Names.ACCEPT, "application/x-apple-binary-plist");
		request.headers().add(APPLE_SESSION_ID, mSessionId);
		
		ChannelFuture future = mContext.writeAndFlush(request);
		try {
	        future = future.sync();
	        if(future.isSuccess()) {
	        	if (VERBOSE) {
	    			Log.v(TAG, String.format("Send photo request: asset=%d, path = %s", assetId, path));
	    		}
	        } else {
	        	if (VERBOSE) {
	    			Log.v(TAG, String.format("Faild to send photo request: asset=%d, path = %s", assetId, path));
	    		}
	        }
        } catch (InterruptedException e) {
	        e.printStackTrace();
        }
	    return true;
    }

	@Override
    public void close() {
		disconnect();
    }
	
}
