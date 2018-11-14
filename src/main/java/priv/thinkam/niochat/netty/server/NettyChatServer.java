package priv.thinkam.niochat.netty.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import priv.thinkam.niochat.common.Constant;

/**
 * chat server
 *
 * @author yanganyu
 * @date 2018/11/14 15:18
 */
public class NettyChatServer {

	public static void main(String[] args) {
		new NettyChatServer().start();
	}

	private void start() {
		EventLoopGroup bossGroup = new NioEventLoopGroup();
		EventLoopGroup workerGroup = new NioEventLoopGroup();
		ServerBootstrap serverBootstrap = new ServerBootstrap();
		serverBootstrap.group(bossGroup, workerGroup)
				.channel(NioServerSocketChannel.class)
				.option(ChannelOption.SO_BACKLOG, 1024)
				.childHandler(new ChannelInitializer<SocketChannel>() {
					@Override
					protected void initChannel(SocketChannel socketChannel) {
						ChannelPipeline channelPipeline = socketChannel.pipeline();
						channelPipeline.addLast("decoder", new StringDecoder());
						channelPipeline.addLast("encoder", new StringEncoder());
						channelPipeline.addLast(new NettyServerHandler());
					}
				});
		try {
			ChannelFuture channelFuture = serverBootstrap.bind(Constant.SERVER_PORT).sync();
			System.out.println("----- netty chat server start -----");
			channelFuture.channel().closeFuture().sync();
		} catch (InterruptedException e) {
			e.printStackTrace();
			Thread.currentThread().interrupt();
		} finally {
			bossGroup.shutdownGracefully();
			workerGroup.shutdownGracefully();
		}
	}

}
