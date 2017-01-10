package com.realtek.cast.airplay.netty;

import com.dd.plist.NSArray;
import com.dd.plist.NSDictionary;
import com.dd.plist.NSNumber;
import com.dd.plist.NSObject;
import com.dd.plist.PropertyListParser;
import com.realtek.cast.control.PlaybackControl;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;

import java.nio.ByteBuffer;

public class AirPlayPhotoHandler extends BaseAirPlayHandler {
	
	@Override
    public void channelHttpRequest(ChannelHandlerContext ctx, FullHttpRequest request) throws Exception {
		final String uri = request.getUri();
		
		// GET /slideshow-features
		if (uri.startsWith("/slideshow-features")) {
			// TODO: themes?
			NSDictionary dict = new NSDictionary();
			NSDictionary theme = new NSDictionary();
			theme.put("key", "Classic");
			theme.put("name", "Classic");
			dict.put("themes", NSArray.wrap(new NSObject[] {
				theme
			}));
			
			DefaultFullHttpResponse res = createResponse(HttpResponseStatus.OK, request);
			res.content().writeBytes(dict.toXMLPropertyList().getBytes());
			ctx.writeAndFlush(res);
			
		// PUT /photo
		} else if (uri.equals("/photo")) {
			String action = request.headers().get(APPLE_ASSET_ACTION);
			String asset = request.headers().get(APPLE_ASSET_KEY);
			
			boolean show;
			ByteBuffer data;
			if ("cacheOnly".equals(action)) {
				show = false;
				data = request.content().nioBuffer();
			} else if ("displayCached".equals(action)) {
				show = true;
				data = null;
			} else {
				show = true;
				data = request.content().nioBuffer();
			}
			
			PlaybackControl.getInstance().showPhoto(asset, show, data);
			
			flushOk(ctx, request);
			
		// PUT /slideshows/1
		} else if (uri.startsWith("/slideshows/1")) {
			// Disconnect previous connections
			disconnectOtherConnections(request);
			
			int size = request.content().readableBytes();
			byte[] buf = new byte[size];
			request.content().getBytes(0, buf);
			NSDictionary dict = (NSDictionary) PropertyListParser.parse(buf);
			
			// Get state
			String state = dict.get("state").toString();
			
			// Start/Stop slide show
			if ("playing".equalsIgnoreCase(state)) {
				// Get settings
				NSDictionary settings = (NSDictionary) dict.get("settings");
				NSNumber nsDuration = (NSNumber) settings.get("slideDuration");
				String theme = settings.get("theme").toString();
				int duration = nsDuration.intValue();
				
				PlaybackControl.getInstance().startSlideShow(theme, duration);
				
			} else if ("stopped".equalsIgnoreCase(state)) {
				PlaybackControl.getInstance().stopSlideShow();
				
			}
			
			// Response
			NSDictionary d = new NSDictionary();
			byte[] content = d.toXMLPropertyList().getBytes();
			
			DefaultFullHttpResponse res = createResponse(HttpResponseStatus.OK, request);
			HttpHeaders.addIntHeader(res, HttpHeaders.Names.CONTENT_LENGTH, content.length);
			res.content().writeBytes(content);
			ctx.writeAndFlush(res);
			
		// POST /stop
		} else if (uri.equals("/stop")) {
			PlaybackControl.getInstance().stopPlayback();
			flushOk(ctx, request);
			
		} else {
			super.channelHttpRequest(ctx, request);
		}
    }
	
}
