package com.realtek.cast.airplay.netty;

import android.util.Log;

import com.dd.plist.BinaryPropertyListParser;
import com.dd.plist.NSArray;
import com.dd.plist.NSDictionary;
import com.dd.plist.NSNumber;
import com.realtek.cast.airplay.AirPlay;
import com.realtek.cast.control.PlaybackControl;
import com.realtek.cast.control.PlaybackControl.VideoInformation;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.QueryStringDecoder;

import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Locale;

public class AirPlayVideoHandler extends BaseAirPlayHandler {
	
	@Override
    public void channelHttpRequest(ChannelHandlerContext ctx, FullHttpRequest request) throws Exception {
		final String uri = request.getUri();
		
		// Volume Adjustment
		if (uri.startsWith("/volume?")) {
			QueryStringDecoder query = new QueryStringDecoder(uri);
			List<String> volume = query.parameters().get("volume");
			if (volume != null && volume.size() > 0) {
				// 0 ~ 1 -> 1 ~ 10
				double v = Double.parseDouble(volume.get(0)) * 9D + 1D;
				// exp -> linear
				v = Math.log10(v);
				PlaybackControl.getInstance().adjustVolume(v, 0, 1, 0);
			}
			flushOk(ctx, request);
			
		// Scrub (Seek)
		} else if (uri.startsWith("/scrub?")) {
			QueryStringDecoder query = new QueryStringDecoder(uri);
			List<String> positions = query.parameters().get("position");
			if (positions != null && positions.size() > 0) {
				float pos = Float.parseFloat(positions.get(0));
				PlaybackControl.getInstance().seekVideoTo(pos);
			}
			flushOk(ctx, request);
			
		// Scrub (Get current position)
		} else if (uri.equals("/scrub")) {
			VideoInformation info = new VideoInformation(); 
			PlaybackControl.getInstance().getVideoInformation(info);
			float duration = 0F;
			float position = 0F;
			if (info != null) {
				position = info.position / 1000F;
				duration = info.duration / 1000F;
			}
			String str = String.format(Locale.US, "duration: %f\r\nposition: %f\r\n", duration, position);
			byte[] c = str.getBytes();
			FullHttpResponse response = createResponse(HttpResponseStatus.OK, request);
			HttpHeaders.addIntHeader(response, "Content-Length", c.length);
			response.headers().add("Content-Type", "text/parameters");
			response.content().writeBytes(c);
			ctx.writeAndFlush(response);
			
		// Rate
		} else if (uri.startsWith("/rate?")) {
			QueryStringDecoder query = new QueryStringDecoder(uri);
			List<String> rate = query.parameters().get("value");
			int stat = -1;
			try {
				float r = Float.parseFloat(rate.get(0));
				if (r == 0) {
					stat = PlaybackControl.getInstance().pauseVideo();
					
				} else if (r == 1) {
					stat = PlaybackControl.getInstance().resumeVideo();
				}
			} catch (NumberFormatException e) {
				e.printStackTrace();
			}
			
			if (stat >= 0 ) {
				sendVideoStatus(request, stat);
				flushOk(ctx, request);
			} else {
				Log.e(TAG, "/rate?: Unknown uri:" + uri);
				DefaultFullHttpResponse res = createResponse(HttpResponseStatus.NOT_IMPLEMENTED, request);
				ctx.writeAndFlush(res);
			}
			
		// Play
		} else if (uri.equals("/play")) {
			// Disconnect previous connections
			disconnectOtherConnections(request);
			
			//
			String contentType = request.headers().get("Content-Type");
			String location = null;
			float position = 0F;
			if ("text/parameters".equals(contentType)) {
				String c = request.content().toString(Charset.defaultCharset());
				String[] lines = c.split("\n");
				for (String line : lines) {
					if (line.startsWith("Content-Location:")) {
						location = line.substring("Content-Location:".length()).trim();
					} else if (line.startsWith("Start-Position")) {
						String p = line.substring("Start-Position:".length()).trim();
						try {position = Float.parseFloat(p);} catch (Exception e) {}
					}
				}
				
				// Play
				if (location != null) {
					int stat = PlaybackControl.getInstance().playVideo(location, position);
					sendVideoStatus(request, stat);
				}
				
				flushOk(ctx, request);
				
			} else if (MIME_APPLE_BINARY_PLIST.equals(contentType)) {
				int size = request.content().readableBytes();
				byte[] data = new byte[size];
				request.content().getBytes(0, data);
				NSDictionary dict = (NSDictionary) BinaryPropertyListParser.parse(data);
				location = dict.get("Content-Location").toString();
				NSNumber num = (NSNumber) dict.get("Start-Position");
				position = num.floatValue();
				
				int stat = PlaybackControl.getInstance().playVideo(location, position);
				sendVideoStatus(request, stat);
				
				flushOk(ctx, request);
				
			// Unknown content type.
			// Not Implemented 501
			} else {
				Log.e(TAG, "/play: Unknown content: type=" +contentType);
				DefaultFullHttpResponse res = createResponse(HttpResponseStatus.NOT_IMPLEMENTED, request);
				ctx.writeAndFlush(res);
			}
			
		} else if (uri.equals("/stop")) {
			PlaybackControl.getInstance().stopPlayback();
			flushOk(ctx, request);
			
		} else if (uri.equals("/server-info")) {
			InetSocketAddress addr = (InetSocketAddress) ctx.channel().localAddress();
			byte[] mac = NetworkInterface.getByInetAddress(addr.getAddress()).getHardwareAddress();
			NSDictionary dict = new NSDictionary();
			dict.put("deviceid", getStringHardwareAdress(mac));
			dict.put("features", (int) AirPlay.FEATURES);
			dict.put("model", AirPlay.MODEL);
			dict.put("protvers", "1.0");
			dict.put("srcvers", AirPlay.SRCVERS);
			
			byte[] contents = dict.toXMLPropertyList().getBytes();
			
			DefaultFullHttpResponse response = createResponse(HttpResponseStatus.OK, request);
			HttpHeaders.addIntHeader(response, "Content-Length", contents.length);
			response.headers().set("Content-Type", "application/x-apple-plist");
			response.content().writeBytes(contents);
			ctx.write(response);
			ctx.flush();
			
		// Playback info: This may be requested when playing YouTube video.
		} else if (uri.equals("/playback-info")) {
			NSDictionary dict = new NSDictionary();
			VideoInformation info = new VideoInformation(); 
			PlaybackControl.getInstance().getVideoInformation(info);
			float duration = info.duration / 1000F;
			float position = info.position / 1000F;
			float rate = info.state == PlaybackControl.VIDEO_PLAYING ? 1F : 0F;
			if (info.state == PlaybackControl.VIDEO_PLAYING || info.state == PlaybackControl.VIDEO_LOADING) {
				dict.put("duration", duration);
				dict.put("position", position);
				dict.put("rate", rate);
			}
			
			dict.put("readyToPlay",
					info.state == PlaybackControl.VIDEO_PLAYING || info.state == PlaybackControl.VIDEO_PAUSED);
			dict.put("playbackBufferEmpty", false);
			dict.put("playbackBufferFull", false);
			dict.put("playbackLikelyToKeepUp", true);
			
			// Whole range
			NSDictionary range = new NSDictionary();
			range.put("start", 0F);
			range.put("duration", duration);
			NSArray ranges = NSArray.wrap(new NSDictionary[]{range});
			dict.put("loadedTimeRanges", ranges);
			dict.put("seekableTimeRanges", ranges);
			
			String str = dict.toXMLPropertyList();
			byte[] contents = str.getBytes(); 
			DefaultFullHttpResponse response = createResponse(HttpResponseStatus.OK, request);
			HttpHeaders.addIntHeader(response, HttpHeaders.Names.CONTENT_LENGTH, contents.length);
			response.headers().set(HttpHeaders.Names.CONTENT_TYPE, MIME_PLIST);
			response.content().writeBytes(contents);
			ctx.writeAndFlush(response).sync();
			
		// Don't know what's this
		} else if (uri.startsWith("/setProperty?")) {
			DefaultFullHttpResponse res = createResponse(HttpResponseStatus.NOT_FOUND, request);
			ctx.writeAndFlush(res);
			/* TODO:
			String query = uri.substring("/setProperty?".length());
			if (query.equals("forwardEndTime")) {
				
			} else if (query.equals("reverseEndTime")) {
				
			}
			
			NSDictionary plist = new NSDictionary();
			plist.put("errorCode", 0);
			
			byte[] contents = BinaryPropertyListWriter.writeToArray(plist);
			DefaultFullHttpResponse res = createResponse(HttpResponseStatus.OK, request);
			res.content().writeBytes(contents);
			
			ctx.writeAndFlush(res).sync();
			*/
		} else if (uri.startsWith("/getProperty?")) {
			DefaultFullHttpResponse res = createResponse(HttpResponseStatus.NOT_FOUND, request);
			ctx.writeAndFlush(res);
			/* TODO:
			VideoInformation info = new VideoInformation();
			PlaybackControl.getInstance().getVideoInformation(info);
			
			NSDictionary plist = new NSDictionary();
			plist.put("errorCode", 0);
			
			NSDictionary value = new NSDictionary();
			value.put("bytes", 0);
			value.put("c-duration-downloaded", 70F);
			value.put("c-duration-watched", 18F);
			value.put("c-frames-dropped", 0);
			value.put("c-observed-bitrate", 14598047.302367469F);
			value.put("c-overdue", 0);
			value.put("c-stalls", 0);
			value.put("c-start-time", 0F);
			value.put("c-startup-time", 0.2F);
			value.put("cs-guid", "B475F105-78FD-4200-96BC-148BAB6DAC11");
			value.put("date", new NSDate(new Date()));
			value.put("s-ip", "192.168.2.10");
			value.put("s-ip-changes", 0);
			value.put("sc-count", 7);
			if (info.uri != null) {
				value.put("uri", info.uri.toSafeString());
			}
			plist.put("value", NSArray.wrap(new NSObject[]{value}));
			
			
			byte[] contents = BinaryPropertyListWriter.writeToArray(plist);
			DefaultFullHttpResponse res = createResponse(HttpResponseStatus.OK, request);
			res.content().writeBytes(contents);
			
			ctx.writeAndFlush(res);
			*/
			
		} else if (uri.equals("/action")) {
			DefaultFullHttpResponse res = createResponse(HttpResponseStatus.NOT_FOUND, request);
			ctx.writeAndFlush(res);
			
		} else if (uri.equals("/authorize")) {
			// iTunes DRM, ignore for now.
			
		} else {
			super.channelHttpRequest(ctx, request);
		}
    }
	
}
