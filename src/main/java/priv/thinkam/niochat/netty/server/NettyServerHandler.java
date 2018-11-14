package priv.thinkam.niochat.netty.server;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.concurrent.DefaultEventExecutor;

/**
 * server handler
 *
 * @author yanganyu
 * @date 2018/11/14 15:25
 */
public class NettyServerHandler extends ChannelInboundHandlerAdapter {
	private static final ChannelGroup CHANNEL_GROUP = new DefaultChannelGroup(new DefaultEventExecutor());

	@Override
	public void handlerAdded(ChannelHandlerContext ctx) {
		Channel incomingChannel = ctx.channel();
		String connectedMessage = incomingChannel.remoteAddress() + " connected!!!" + System.lineSeparator();
		System.out.println(connectedMessage);
		CHANNEL_GROUP.forEach(c -> c.writeAndFlush(connectedMessage));
		this.sendOnlineInfoToNew(incomingChannel);
		CHANNEL_GROUP.add(incomingChannel);
	}

	private void sendOnlineInfoToNew(Channel incomingChannel) {
		if (!CHANNEL_GROUP.isEmpty()) {
			StringBuilder onlineMessage = new StringBuilder();
			CHANNEL_GROUP.forEach(c -> onlineMessage.append(c.remoteAddress()).append(" is online!!!").append(System.lineSeparator()));
			incomingChannel.writeAndFlush(onlineMessage.toString());
		}
	}

	@Override
	public void handlerRemoved(ChannelHandlerContext ctx) {
		Channel incomingChannel = ctx.channel();
		String exitMessage = incomingChannel.remoteAddress() + " exited!!!";
		System.out.println(exitMessage);
		CHANNEL_GROUP.forEach(c -> c.writeAndFlush(exitMessage));
		CHANNEL_GROUP.remove(incomingChannel);
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) {
		String message = (String) msg;
		System.out.println(message);
		Channel incomingChannel = ctx.channel();
		for (Channel channel : CHANNEL_GROUP) {
			if (channel != incomingChannel) {
				channel.writeAndFlush(message);
			}
		}
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
		cause.printStackTrace();
		ctx.close();
	}
}
