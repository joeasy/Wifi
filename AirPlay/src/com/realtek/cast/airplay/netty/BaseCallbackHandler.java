
package com.realtek.cast.airplay.netty;

import android.util.Log;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpResponse;

public class BaseCallbackHandler extends BaseAirPlayHandler {

	protected final ChannelHandlerContext mContext;
	protected final int mSessionIdx;
	protected final String mSessionId;
	protected final String mDeviceId;

	public BaseCallbackHandler(ChannelHandlerContext ctx, int sessionIdx, String appleSessionId, String deviceId) {
		mContext = ctx;
		mSessionId = appleSessionId;
		mSessionIdx = sessionIdx;
		mDeviceId = deviceId;
	}
	
	@Override
    public void channelHttpResponse(ChannelHandlerContext ctx, FullHttpResponse response) throws Exception {
		if (VERBOSE) {
			Log.v(TAG, "Received response: " + response.getStatus());
		}
	    super.channelHttpResponse(ctx, response);
    }

	public void disconnect() {
		mContext.close();
	}

}
