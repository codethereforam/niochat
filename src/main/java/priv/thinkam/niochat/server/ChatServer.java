package priv.thinkam.niochat.server;

import priv.thinkam.niochat.common.Constant;
import priv.thinkam.niochat.util.StringUtils;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * chat server
 *
 * @author yanganyu
 * @date 2018/11/7 15:04
 */
public class ChatServer {
	private Selector selector;
	private Set<SocketChannel> socketChannelSet = new HashSet<>();

	private ChatServer() {
		try {
			selector = Selector.open();
			ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
			serverSocketChannel.configureBlocking(false);
			serverSocketChannel.socket().bind(new InetSocketAddress(Constant.SERVER_PORT));
			serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
		} catch (IOException e) {
			e.printStackTrace();
			this.close();
			System.exit(-1);
		}
		System.out.println("----- chat server start -----");
	}

	public static void main(String[] args) {
		new ChatServer().start();
		System.out.println("server stop...");
	}

	/**
	 * server thread
	 *
	 * @author yanganyu
	 * @date 2018/11/7 15:21
	 */
	private void start() {
		while (true) {
			try {
				selector.select(1000);
			} catch (IOException e) {
				e.printStackTrace();
				this.close();
				return;
			}
			if (!selector.isOpen()) {
				this.close();
				return;
			}
			Set<SelectionKey> selectionKeySet = selector.selectedKeys();
			Iterator<SelectionKey> selectionKeyIterator = selectionKeySet.iterator();
			SelectionKey selectionKey;
			while (selectionKeyIterator.hasNext()) {
				selectionKey = selectionKeyIterator.next();
				selectionKeyIterator.remove();
				try {
					handleSelectionKey(selectionKey);
				} catch (Exception e) {
					e.printStackTrace();
					this.closeSelectionKey(selectionKey);
				}
			}
		}
	}

	private void closeSelectionKey(SelectionKey selectionKey) {
		if (selectionKey != null) {
			selectionKey.cancel();
			if (selectionKey.channel() != null) {
				try {
					selectionKey.channel().close();
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			}
		}
	}

	/**
	 * handle input
	 *
	 * @param selectionKey selectionKey
	 * @author yanganyu
	 * @date 2018/11/7 15:37
	 */
	private void handleSelectionKey(SelectionKey selectionKey) throws IOException {
		if (selectionKey.isValid()) {
			if (selectionKey.isAcceptable()) {
				// Accept the new connection
				ServerSocketChannel serverSocketChannel = (ServerSocketChannel) selectionKey.channel();
				SocketChannel socketChannel = serverSocketChannel.accept();
				socketChannel.configureBlocking(false);
				// Add the new connection to the selector
				socketChannel.register(selector, SelectionKey.OP_READ);
				this.sendConnectedInfoToAll(socketChannel);
				this.sendOnlineInfoToNew(socketChannel);
				socketChannelSet.add(socketChannel);
			} else if (selectionKey.isReadable()) {
				// Read the data
				SocketChannel socketChannel = (SocketChannel) selectionKey.channel();
				ByteBuffer readBuffer = ByteBuffer.allocate(1024);
				int readBytes;
				try {
					readBytes = socketChannel.read(readBuffer);
				} catch (IOException e) {
					handleClientDisconnect(socketChannel, selectionKey);
					e.printStackTrace();
					return;
				}
				if (readBytes > 0) {
					this.handleReceivedMessage(readBuffer, socketChannel);
				} else if (readBytes < 0) {
					handleClientDisconnect(socketChannel, selectionKey);
				}
			} else {
				System.out.println("!!! something wrong !!!");
				System.exit(-1);
			}
		}
	}

	private void handleReceivedMessage(ByteBuffer readBuffer, SocketChannel socketChannel) {
		readBuffer.flip();
		byte[] bytes = new byte[readBuffer.remaining()];
		readBuffer.get(bytes);
		String message = new String(bytes, StandardCharsets.UTF_8);
		System.out.println(message);
		sendMessageToAll(message, socketChannel);
	}

	private void handleClientDisconnect(SocketChannel socketChannel, SelectionKey selectionKey) {
		this.sendExitInfoToAll(socketChannel);
		// close resource
		selectionKey.cancel();
		try {
			socketChannel.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		socketChannelSet.remove(socketChannel);
	}

	/**
	 * send exit server info to all
	 *
	 * @param socketChannel exit socketChannel
	 * @author yanganyu
	 * @date 2018/11/12 14:36
	 */
	private void sendExitInfoToAll(SocketChannel socketChannel) {
		String exitMessage = this.getSocketIpAndPort(socketChannel) + " exited!!!";
		System.out.println(exitMessage);
		sendMessageToAll(exitMessage, socketChannel);
	}

	/**
	 * send connected server info to all
	 *
	 * @param socketChannel socketChannel
	 * @author yanganyu
	 * @date 2018/11/12 14:30
	 */
	private void sendConnectedInfoToAll(SocketChannel socketChannel) {
		String connectedMessage = this.getSocketIpAndPort(socketChannel) + " connected!!!\n";
		System.out.println(connectedMessage);
		sendMessageToAll(connectedMessage, socketChannel);
	}

	/**
	 * send online server info to new
	 *
	 * @param socketChannel socketChannel
	 * @author yanganyu
	 * @date 2018/11/12 14:28
	 */
	private void sendOnlineInfoToNew(SocketChannel socketChannel) {
		StringBuilder onlineMessage = new StringBuilder();
		socketChannelSet.forEach(s -> onlineMessage.append(this.getSocketIpAndPort(s)).append(" is online!!!").append("\n"));
		sendMessage(socketChannel, onlineMessage.toString());
	}

	/**
	 * get ip:port
	 *
	 * @return java.lang.String
	 * @author yanganyu
	 * @date 11/10/18 8:11 PM
	 */
	private String getSocketIpAndPort(SocketChannel socketChannel) {
		return socketChannel.socket().getInetAddress().getHostAddress() + ":" + socketChannel.socket().getPort();
	}

	/**
	 * send message to all except exceptedSocketChannel
	 *
	 * @param message               message
	 * @param exceptedSocketChannel excepted SocketChannel
	 * @author yanganyu
	 * @date 2018/11/12 14:33
	 */
	private void sendMessageToAll(String message, SocketChannel exceptedSocketChannel) {
		socketChannelSet.forEach(s -> {
			if (s.isOpen() && s != exceptedSocketChannel) {
				sendMessage(s, message);
			}
		});
	}

	/**
	 * send message to client
	 *
	 * @param channel  channel
	 * @param response response
	 * @author yanganyu
	 * @date 2018/11/7 15:39
	 */
	private void sendMessage(SocketChannel channel, String response) {
		if (StringUtils.isNotBlank(response)) {
			byte[] bytes = response.getBytes();
			ByteBuffer writeBuffer = ByteBuffer.allocate(bytes.length);
			writeBuffer.put(bytes);
			writeBuffer.flip();
			try {
				channel.write(writeBuffer);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * close resource
	 *
	 * @author yanganyu
	 * @date 2018/11/12 14:44
	 */
	private void close() {
		if (selector != null) {
			try {
				selector.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
