package jme3_ext_remote_editor;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;

import com.jme3.app.SimpleApplication;
import com.jme3.app.state.AbstractAppState;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;

public class AppState4RemoteCommand extends AbstractAppState {

	public int port = 4242;
	private ChannelFuture f;
	EventLoopGroup bossGroup;
	EventLoopGroup workerGroup;
	private SimpleApplication app;
	Pgex pgex;

	void start() throws Exception {
		bossGroup = new NioEventLoopGroup();
		workerGroup = new NioEventLoopGroup();

		ServerBootstrap b = new ServerBootstrap();
		b.group(bossGroup, workerGroup)
		.channel(NioServerSocketChannel.class)
		.childHandler(new ChannelInitializer<SocketChannel>() {
			@Override
			public void initChannel(SocketChannel ch) throws Exception {
				RemoteHandler rh = new RemoteHandler(
					AppState4RemoteCommand.this.app
					, AppState4RemoteCommand.this.pgex
				);
				ch.pipeline().addLast(
					new LengthFieldBasedFrameDecoder(Integer.MAX_VALUE, 0, 4, 1, 4)
					,new ServerHandler4Capture(rh)
				);
			}
		})
		.option(ChannelOption.SO_BACKLOG, 128)
		.childOption(ChannelOption.SO_KEEPALIVE, true);

		// Bind and start to accept incoming connections.
		f = b.bind(port).sync();

	}

	void stop() throws Exception {
		if (workerGroup != null) workerGroup.shutdownGracefully();
		if (bossGroup != null) bossGroup.shutdownGracefully();
		if (f != null) f.channel().close().sync();
	}

	public void initialize(com.jme3.app.state.AppStateManager stateManager0, com.jme3.app.Application app0) {
		try {
			app = (SimpleApplication)app0;
			start();
			Material defaultMaterial = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
			defaultMaterial.setColor("Color", ColorRGBA.Gray);
			pgex = new Pgex(app.getAssetManager(), defaultMaterial);
			pgex.setupExtensionRegistry();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void cleanup() {
		try {
			stop();
		} catch (Exception e) {
			e.printStackTrace();
		}
	};
}
