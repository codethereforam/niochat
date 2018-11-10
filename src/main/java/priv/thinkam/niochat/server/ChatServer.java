package priv.thinkam.niochat.server;

import priv.thinkam.niochat.common.Constant;
import priv.thinkam.niochat.util.StringUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * chat server
 *
 * @author yanganyu
 * @date 2018/11/7 15:04
 */
public class ChatServer {
	private Selector selector;
	private Set<SocketChannel> socketChannelSet = new HashSet<>();
	private volatile boolean running = true;

	private ChatServer() {
		try {
			selector = Selector.open();
			ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
			serverSocketChannel.configureBlocking(false);
			serverSocketChannel.socket().bind(new InetSocketAddress(Constant.SERVER_PORT));
			serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}
		System.out.println("==== chat server start ====");
	}

	public static void main(String[] args) {
		ChatServer chatServer = new ChatServer();
		// listen stop command
		ExecutorService pool = Executors.newSingleThreadExecutor();
		pool.execute(() -> {
			try (
					BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in))
			) {
				while (true) {
					String command = bufferedReader.readLine();
					if (command != null && Constant.STOP_COMMAND.equals(command.trim())) {
						chatServer.stop();
						break;
					} else {
						System.out.println("!!!! invalid command !!!!");
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		});
		chatServer.start();
	}

	/**
	 * server thread
	 *
	 * @author yanganyu
	 * @date 2018/11/7 15:21
	 */
	private void start() {
		while (running) {
			try {
				selector.select(1000);
				Set<SelectionKey> selectionKeySet = selector.selectedKeys();
				Iterator<SelectionKey> selectionKeyIterator = selectionKeySet.iterator();
				SelectionKey key;
				while (selectionKeyIterator.hasNext()) {
					key = selectionKeyIterator.next();
					selectionKeyIterator.remove();
					try {
						handleInput(key);
					} catch (Exception e) {
						if (key != null) {
							key.cancel();
							if (key.channel() != null) {
								key.channel().close();
							}
						}
						break;
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
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
	private void handleInput(SelectionKey selectionKey) throws IOException {
		if (selectionKey.isValid()) {
			if (selectionKey.isAcceptable()) {
				// Accept the new connection
				ServerSocketChannel ssc = (ServerSocketChannel) selectionKey.channel();
				SocketChannel socketChannel = ssc.accept();
				socketChannel.configureBlocking(false);
				// Add the new connection to the selector
				socketChannel.register(selector, SelectionKey.OP_READ);
				String connectedMessage =
						socketChannel.socket().getInetAddress().getHostAddress() + ":" + socketChannel.socket().getPort() + " connected!!!";
				System.out.println(connectedMessage);
				sendMessageToAll(connectedMessage, socketChannel);
				// send old server info to new
				StringBuilder onlineMessage = new StringBuilder();
				socketChannelSet.forEach(s -> onlineMessage.append(socketChannel.socket().getInetAddress().getHostAddress()).append(":").append(socketChannel.socket().getPort()).append(" is online!!!\n"));
				sendMessage(socketChannel, onlineMessage.toString());
				socketChannelSet.add(socketChannel);
			} else if (selectionKey.isReadable()) {
				// Read the data
				SocketChannel socketChannel = (SocketChannel) selectionKey.channel();
				ByteBuffer readBuffer = ByteBuffer.allocate(1024);
				int readBytes = socketChannel.read(readBuffer);
				if (readBytes > 0) {
					readBuffer.flip();
					byte[] bytes = new byte[readBuffer.remaining()];
					readBuffer.get(bytes);
					String message = new String(bytes, StandardCharsets.UTF_8);
					System.out.println(message);
					sendMessageToAll(message, socketChannel);
				} else if (readBytes < 0) {
					String exitMessage = socketChannel.socket().getInetAddress().getHostAddress() + ":" + socketChannel.socket().getPort() + " exited!!!";
					System.out.println(exitMessage);
					sendMessageToAll(exitMessage, null);
					socketChannelSet.remove(socketChannel);
					// close resource
					selectionKey.cancel();
					socketChannel.close();
				}
			} else {
				System.out.println("!!! something wrong !!!");
				System.exit(-1);
			}
		}
	}

	private void sendMessageToAll(String message, SocketChannel exceptedSocketChannel) {
		socketChannelSet.forEach(s -> {
			if (s.isOpen()) {
				if (s != exceptedSocketChannel) {
					sendMessage(s, message);
				}
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
		if (response != null && StringUtils.isNotBlank(response)) {
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
	 * stop server
	 *
	 * @author yanganyu
	 * @date 2018/11/8 15:11
	 */
	private void stop() {
		running = false;
		try {
			selector.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
