
package com.realtek.cast.airplay.netty;

import android.util.Log;

import com.realtek.cast.airplay.AirPlayServer;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;

import java.util.Date;

public class AirPlayReverseHandler extends BaseAirPlayHandler {

	@Override
    public void channelHttpRequest(ChannelHandlerContext ctx, FullHttpRequest request) throws Exception {
		if (VERBOSE) {
			Log.v(TAG, "Received request: " + request.getMethod() + " " + request.getUri());
		}
		String uri = request.getUri();
		if (uri.equals("/reverse")) {
			// Get propose
			 String purpose = request.headers().get(APPLE_PURPOSE);
			
			// session ID
			String sessionId = request.headers().get(APPLE_SESSION_ID);
			String device = request.headers().get(APPLE_DEVICE_ID);
			if (sessionId == null) {
				sessionId = "00000000-0000-0000-0000-000000000000";
			}
			
			// Response Switching Protocols
			HttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1,
					HttpResponseStatus.SWITCHING_PROTOCOLS);
			response.headers().set("Date", DATE_FORMAT.format(new Date()))
			.set("Upgrade", "PTTH/1.0").set("Connection", "Upgrade");
			ctx.writeAndFlush(response).sync();
			
			// Create callback handler
			ChannelHandler callback;
			if ("slideshow".equals(purpose)) {
				// Connections for requesting slide show images
				callback = new AirPlayPhotoRequester(ctx, device, sessionId);
			} else {
				// Connections that send callback events
				callback = createCallbackEventSession(ctx, device, sessionId);
			}
			
			// Replace the channel handlers
			if (AirPlayServer.LOG_PACKET) {
				ctx.pipeline().replace("logger", "reverse_logger",
						new AirPlayServer.ChannelLogger("ReverseChannel"));
			}
			ctx.pipeline().replace("server_codec", "client_codec", new HttpClientCodec());
			ctx.pipeline().replace(this, "callback", callback);
			
			if (VERBOSE) {
				Log.v(TAG, String.format("Reverse connection: purpose=%s, device=%s, session=%s",  purpose, device, sessionId));
			}
			
		} else {
			super.channelHttpRequest(ctx, request);
		}
    }
}
