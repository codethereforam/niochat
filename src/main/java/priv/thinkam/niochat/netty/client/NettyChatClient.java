package priv.thinkam.niochat.netty.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import priv.thinkam.niochat.common.Constant;

/**
 * netty chat client
 *
 * @author yanganyu
 * @date 2018/11/19 16:27
 */
public class NettyChatClient {
	private static final String SERVER_IP = "127.0.0.1";

	private ChatClientFrame chatClientFrame;

	public static void main(String[] args) {
		NettyChatClient nettyChatClient = new NettyChatClient();
		ChatClientFrame chatClientFrame = new ChatClientFrame();
		nettyChatClient.setChatClientFrame(chatClientFrame);
		nettyChatClient.start();
	}

	private void setChatClientFrame(ChatClientFrame chatClientFrame) {
		this.chatClientFrame = chatClientFrame;
	}

	private void start() {
		EventLoopGroup eventLoopGroup = new NioEventLoopGroup();
		Bootstrap bootstrap = new Bootstrap();
		bootstrap.group(eventLoopGroup).channel(NioSocketChannel.class)
				.option(ChannelOption.TCP_NODELAY, true)
				.handler(new ChannelInitializer<SocketChannel>() {
					@Override
					public void initChannel(SocketChannel socketChannel) {
						ChannelPipeline channelPipeline = socketChannel.pipeline();
						channelPipeline.addLast(new LineBasedFrameDecoder(2048));
						channelPipeline.addLast("decoder", new StringDecoder());
						channelPipeline.addLast("encoder", new StringEncoder());
						NettyClientHandler nettyClientHandler = new NettyClientHandler(chatClientFrame);
						chatClientFrame.setNettyClientHandler(nettyClientHandler);
						channelPipeline.addLast(nettyClientHandler);
					}
				});
		try {
			ChannelFuture f = bootstrap.connect(SERVER_IP, Constant.SERVER_PORT).sync();
			f.channel().closeFuture().sync();
		} catch (InterruptedException e) {
			e.printStackTrace();
			Thread.currentThread().interrupt();
		} finally {
			eventLoopGroup.shutdownGracefully();
		}
	}

}
