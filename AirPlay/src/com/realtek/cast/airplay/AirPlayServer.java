package com.realtek.cast.airplay;

import android.util.Log;

import com.realtek.cast.airplay.netty.AirPlayMirrorHandler;
import com.realtek.cast.airplay.netty.AirPlayPhotoHandler;
import com.realtek.cast.airplay.netty.AirPlayReverseHandler;
import com.realtek.cast.airplay.netty.AirPlayVideoHandler;
import com.realtek.cast.util.Bonjour;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPromise;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;

import java.nio.charset.Charset;
import java.security.PrivateKey;


/**
 * LaunchThread class which starts services
 * 
 * @author bencall
 */
public class AirPlayServer extends Thread {
	
	private static final String TAG = AirPlay.TAG;
	public static final boolean LOG_PACKET = false;
	
	private Bonjour mBonjour;
	
	private final String name;
//	private final PrivateKey mKey;
	
	/**
	 * Constructor
	 * @param name
	 */
	public AirPlayServer(String name, PrivateKey key){
		super("air_play");
		this.name = name;
//		mKey = key;
		setDaemon(true);
	}
	
	public void run(){
		// AirPlay Video Server
		int port = AirPlay.PORT;
		EventLoopGroup videoBossGroup = new NioEventLoopGroup(1);
		EventLoopGroup videoWorkerGroup = new NioEventLoopGroup(1);
		EventLoopGroup mirrorBossGroup = new NioEventLoopGroup(1);
		EventLoopGroup mirrorWorkerGroup = new NioEventLoopGroup(1);
		
		ChannelFuture videoFuture = null;
		ChannelFuture mirrorFuture = null;
		try {
			// DNS Emitter (Bonjour)
			mBonjour = new BonjourAirPlay(name, port);
			mBonjour.start();
			
			
			
			
			videoFuture = startVideoServer(port, videoBossGroup, videoWorkerGroup);

			mirrorFuture = startMirrorServer(7100, mirrorBossGroup, mirrorWorkerGroup);
			
			// Wait until the server socket is closed.
            // In this example, this does not happen, but you can do that to gracefully
            // shut down your server.
			videoFuture.channel().closeFuture().sync();
			mirrorFuture.channel().closeFuture().sync();
            
        } catch (InterruptedException e) {
	        e.printStackTrace();
	        
        } finally {
        	videoWorkerGroup.shutdownGracefully();
            videoBossGroup.shutdownGracefully();
            mirrorWorkerGroup.shutdownGracefully();
            mirrorBossGroup.shutdownGracefully();
			if (mBonjour != null) {
				mBonjour.stopBonjour();
			}
		}
		Log.d(TAG, "service stopped");
	}
	
	public void refreshBonjour() {
	    if (mBonjour != null) {
	    	mBonjour.refresh();
	    }
    }
	
	private ChannelFuture startVideoServer(int port, EventLoopGroup bossGroup, EventLoopGroup workerGroup) {
		// Netty - Video
		ServerBootstrap b = new ServerBootstrap();
		b.group(bossGroup, workerGroup)
		.channel(NioServerSocketChannel.class)
		.childHandler(new ChannelInitializer<Channel>() {

			@Override
            protected void initChannel(Channel ch) throws Exception {
				if (LOG_PACKET) {
					ch.pipeline().addLast("logger", new ChannelLogger("VideoChannel"));
				}
				ch.pipeline()
				.addLast("server_codec", new HttpServerCodec())
				.addLast("aggregator", new HttpObjectAggregator(512*1024))
				.addLast("reverse", new AirPlayReverseHandler())
				.addLast("airplay", new AirPlayVideoHandler())
				.addLast("airphoto", new AirPlayPhotoHandler());
            }
		})
		.option(ChannelOption.SO_BACKLOG, 128)
		.childOption(ChannelOption.SO_KEEPALIVE, true);
		
		// Bind and start to accept incoming connections.
		ChannelFuture f = b.bind(port);
		return f;
	}
	
	private ChannelFuture startMirrorServer(int port, EventLoopGroup bossGroup, EventLoopGroup workerGroup) {
		// Netty - Mirror
		ServerBootstrap b = new ServerBootstrap();
		b.group(bossGroup, workerGroup)
		.channel(NioServerSocketChannel.class)
		.childHandler(new ChannelInitializer<Channel>() {

			@Override
            protected void initChannel(Channel ch) throws Exception {
				if (LOG_PACKET) {
					ch.pipeline().addLast("logger", new ChannelLogger("MirrorChannel"));
				}
				ch.pipeline()
				.addLast("server_codec", new HttpServerCodec())
				.addLast("aggregator", new HttpObjectAggregator(512*1024))
				.addLast("airmirror", new AirPlayMirrorHandler());
            }
		})
		.option(ChannelOption.SO_BACKLOG, 128)
		.childOption(ChannelOption.SO_KEEPALIVE, true);
		
		// Bind and start to accept incoming connections.
		ChannelFuture f = b.bind(port);
		return f;
	}
	
	public static class ChannelLogger extends ChannelDuplexHandler {
		
		private final String mTag;
		
		public ChannelLogger(String tag) {
			mTag = tag;
		}
		
		@Override
		public void write(ChannelHandlerContext ctx, Object msg,
				ChannelPromise promise) throws Exception {
			ByteBuf buf = (ByteBuf) msg;
			String str = buf.toString(Charset.defaultCharset());
			Log.i(mTag, "< < < " + str);
			super.write(ctx, msg, promise);
		}
		
		@Override
		public void channelRead(ChannelHandlerContext ctx, Object msg)
				throws Exception {
			ByteBuf buf = (ByteBuf) msg;
			String str = buf.toString(Charset.defaultCharset());
			Log.i(mTag, "> > > " + str);
			super.channelRead(ctx, msg);
		}

		@Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
                throws Exception {
            super.exceptionCaught(ctx, cause);
            Log.e(mTag, cause.getLocalizedMessage());
        }

		@Override
        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
	        super.channelInactive(ctx);
        }
		
	}
	
	public void close() {
		// TODO: close server
	}

}
