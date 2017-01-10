package com.realtek.cast.airplay.netty;

import android.util.Log;

import com.dd.plist.Base64;
import com.dd.plist.BinaryPropertyListParser;
import com.dd.plist.NSData;
import com.dd.plist.NSDictionary;
import com.dd.plist.NSString;
import com.realtek.cast.airplay.AirPlay;
import com.realtek.cast.control.PlaybackControl;
import com.realtek.cast.util.NSUtils;
import com.realtek.cast.util.NtpTime;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class AirPlayMirrorHandler extends BaseAirPlayHandler {
	
	@Override
    public void channelHttpRequest(ChannelHandlerContext ctx, FullHttpRequest request) throws Exception {
			
		String uri = request.getUri();
		if (uri.equals("/stream.xml")) {
			NSDictionary plist = new NSDictionary();
			plist.put("width", 1280);
			plist.put("height", 720);
			plist.put("overscanned", false);
			plist.put("refreshRate", 0.016666666666666666);
			plist.put("version", AirPlay.SRCVERS);
			
			DefaultFullHttpResponse response = createResponse(HttpResponseStatus.OK, request);
			byte[] payload = plist.toXMLPropertyList().getBytes();
			response.headers().add("Content-Length", payload.length);
			response.headers().add("Content-Type", "text/x-apple-plist+xml");
			response.content().writeBytes(payload);
			ctx.writeAndFlush(response);
			
		} else if (uri.equals("/stream")) {
			int contentLength = request.content().readableBytes();
			byte[] contents = new byte[contentLength];
			request.content().getBytes(request.content().readerIndex(), contents);
			
			/*
			 * deviceID	integer	181221086727016	MAC address (A4:D1:D2:80:0B:68)
			 * sessionID	integer	¡V808788724	session ID (0xcfcadd0c)
			 * version	string	130.16	server version
			 * param1	data	(72 bytes)	AES key, encrypted with FairPlay
			 * param2	data	(16 bytes)	AES initialization vector
			 * latencyMs	integer	90	video latency in ms
			 * fpsInfo	array
			 * timestampInfo	array		
			 */
			NSDictionary plist = (NSDictionary) BinaryPropertyListParser.parse(contents);
			String deviceId = ((NSString) plist.get("deviceID")).getContent();
			String sessionId = ((NSString) plist.get("sessionId")).getContent();
			String version = ((NSString) plist.get("version")).getContent();
			
			
			int latencyMs = NSUtils.getInteger(plist, "latencyMs", 90);
			
			byte[] aesKey = null;
			byte[] aesIv = null;
			if (plist.containsKey("param1")) {
				String aesKey64 = ((NSData) plist.get("param1")).getBase64EncodedData();
				aesKey = Base64.decode(aesKey64);
				
				String aesIv64 = ((NSData) plist.get("param2")).getBase64EncodedData();
				aesIv = Base64.decode(aesIv64);
			}
			
			//
			ctx.pipeline().remove("server_codec");
			ctx.pipeline().remove("aggregator");
			ctx.pipeline().remove("airmirror");
			ctx.pipeline().addLast(new MirrorHandler());
			PlaybackControl.getInstance().startMirroring();
			
		} else if (uri.equals("/bad-request")) {
			// FIXME: What's this!?
			DefaultFullHttpResponse res = createResponse(HttpResponseStatus.BAD_REQUEST, request);
			ctx.writeAndFlush(res).sync();
			
		} else {
			Log.e(TAG, "Unknown request:" + request.toString());
		}
    }

	private static class MirrorHandler extends ChannelInboundHandlerAdapter {
		
		private ByteBuffer mFragment = ByteBuffer.allocate(1024*1024*4).order(ByteOrder.LITTLE_ENDIAN);

		@Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
	        ByteBuf buf = ((ByteBuf) msg);
	        
	        ByteBuffer buffer = buf.copy().nioBuffer().order(ByteOrder.LITTLE_ENDIAN).slice();
	        mFragment.put(buffer);
	        mFragment.limit(mFragment.position());
	        mFragment.rewind();
	        digestPacket(mFragment);
	        
	        buf.release();
        }
		
		private static void digestPacket(ByteBuffer buffer) {
			while(true) {
				final int start = buffer.position();
				if (buffer.remaining() < 128) {
					buffer.position(start);
					buffer.compact();
					break;
				}
				
				int payloadLength = buffer.getInt();
				short payloadType = buffer.getShort();
				short what = buffer.getShort();
				NtpTime timestamp = NtpTime.create(buffer, buffer.position());
				buffer.getLong();
				buffer.position(start + 128);
				
				if (buffer.remaining() < payloadLength) {
					buffer.position(start);
					buffer.compact();
					break;
				}
				
				//
				final int limit = buffer.limit();
				buffer.limit(buffer.position() + payloadLength);
				ByteBuffer payload = buffer.slice();
				
				// Video bitstream
				if (payloadType == 0) {
					PlaybackControl.getInstance().writeMirrorStream(payload);
					
				} else if (payloadType == 1) {
					payload.order(ByteOrder.BIG_ENDIAN);
					// Codec data
					payload.position(5);
					
					// Sequence of parameter set
					int numSps = payload.get() & 0x1F;
					int lenSps = payload.getShort() & 0xFFFF;
					byte[] sps = new byte[lenSps];
					payload.get(sps, 0, lenSps);
					
					// Picture parameter set
					int numPps = payload.get() & 0xFF;
					int lenPps = payload.getShort() & 0xFFFF;
					byte[] pps = new byte[lenPps];
					payload.get(pps, 0, lenPps);
					
					
					PlaybackControl.getInstance().setupMirror(sps, pps);
					
				} else if (payloadType == 2) {
					// Hearbeats
					
				}
				
				//
				buffer.limit(limit);
				buffer.position(start + 128 + payloadLength);
				
				if (buffer.remaining() == 0) {
					buffer.compact();
					break;
				}
			}
		}
	}
}
