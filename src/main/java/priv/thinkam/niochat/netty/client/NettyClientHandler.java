package priv.thinkam.niochat.netty.client;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import priv.thinkam.niochat.util.AESUtils;

/**
 * netty client handler
 *
 * @author yanganyu
 * @date 2018/11/19 16:34
 */
public class NettyClientHandler extends ChannelInboundHandlerAdapter {
	private ChatClientFrame chatClientFrame;
	private static final String AES_SECRET_KEY = "123456789123456789";
	/**
	 * like "username: "
	 */
	private String sendMessagePrefix;
	private ChannelHandlerContext ctx;

	NettyClientHandler(ChatClientFrame chatClientFrame) {
		this.chatClientFrame = chatClientFrame;
	}

	@Override
	public void channelActive(ChannelHandlerContext ctx) {
		System.out.println("connected to server...");
		this.ctx = ctx;
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) {
		String message = (String) msg;
		System.out.println(message);
		String decryptedMessage;
		try {
			decryptedMessage = AESUtils.decrypt(message, AES_SECRET_KEY);
		} catch (Exception e) {
			decryptedMessage = message;
		}
		chatClientFrame.setTextAreaText(decryptedMessage);
	}

	/**
	 * send message to server
	 *
	 * @author yanganyu
	 * @date 11/10/18 10:22 AM
	 */
	void sendMessageToServer(String text) {
		String encryptedString = AESUtils.encrypt((sendMessagePrefix + text), AES_SECRET_KEY);
		if (encryptedString == null) {
			System.exit(-1);
		}
		ctx.channel().writeAndFlush(encryptedString + System.lineSeparator());
	}

	void close() {
		ctx.close();
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
		cause.printStackTrace();
		ctx.close();
		chatClientFrame.handleServerCrash();
	}

	/**
	 * set send message prefix
	 *
	 * @param sendMessagePrefix sendMessagePrefix
	 * @author yanganyu
	 * @date 11/10/18 10:30 AM
	 */
	void setSendMessagePrefix(String sendMessagePrefix) {
		this.sendMessagePrefix = sendMessagePrefix;
	}
}
