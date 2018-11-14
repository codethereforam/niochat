package priv.thinkam.niochat.nio.client;

import priv.thinkam.niochat.common.Constant;
import priv.thinkam.niochat.util.AESUtils;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Set;

/**
 * chat client
 *
 * @author yanganyu
 * @date 2018/11/7 15:04
 */
class ChatClient {
	private static final String SERVER_IP = "127.0.0.1";
	private static final String AES_SECRET_KEY = "123456789123456789";
	private Selector selector;
	private SocketChannel socketChannel;
	/**
	 * like "username: "
	 */
	private String sendMessagePrefix;
	private volatile boolean running = true;

	private ChatClientFrame chatClientFrame;

	private ChatClient() {
		try {
			selector = Selector.open();
			socketChannel = SocketChannel.open();
			socketChannel.configureBlocking(false);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	public static void main(String[] args) {
		ChatClient chatClient = new ChatClient();
		ChatClientFrame chatClientFrame = new ChatClientFrame(chatClient);
		chatClient.setChatClientFrame(chatClientFrame);
		chatClient.start();
		System.out.println("client disconnected...");
	}

	private void setChatClientFrame(ChatClientFrame chatClientFrame) {
		this.chatClientFrame = chatClientFrame;
	}

	/**
	 * client thread
	 *
	 * @author yanganyu
	 * @date 2018/11/7 15:48
	 */
	private void start() {
		try {
			this.connect();
		} catch (IOException e) {
			e.printStackTrace();
			this.close();
			System.exit(-1);
		}
		while (running) {
			int readyChannelCount;
			try {
				readyChannelCount = selector.select(1000);
			} catch (IOException e) {
				e.printStackTrace();
				this.close();
				return;
			}
			if (readyChannelCount == 0) {
				continue;
			}
			if (!selector.isOpen()) {
				return;
			}
			this.handleSelectedKeys();
		}
	}

	private void handleSelectedKeys() {
		Set<SelectionKey> selectedKeys = selector.selectedKeys();
		Iterator<SelectionKey> keyIterator = selectedKeys.iterator();
		SelectionKey selectionKey;
		while (keyIterator.hasNext()) {
			selectionKey = keyIterator.next();
			keyIterator.remove();
			try {
				handleSelectionKey(selectionKey);
			} catch (Exception e) {
				e.printStackTrace();
				this.closeSelectionKey(selectionKey);
			}
		}
	}

	private void handleSelectionKey(SelectionKey selectionKey) {
		if (selectionKey.isValid()) {
			if (selectionKey.isConnectable()) {
				this.handleConnectableKey(selectionKey);
			} else if (selectionKey.isReadable()) {
				this.handleReadableKey(selectionKey);
			}
		}
	}

	private void handleConnectableKey(SelectionKey selectionKey) {
		SocketChannel sc = (SocketChannel) selectionKey.channel();
		boolean finishConnected = false;
		try {
			finishConnected = sc.finishConnect();
		} catch (IOException e) {
			System.out.println("can not connected to server...");
			e.printStackTrace();
		}
		if (finishConnected) {
			try {
				sc.register(selector, SelectionKey.OP_READ);
			} catch (ClosedChannelException e) {
				this.handleServerCrashAndClose(selectionKey);
				e.printStackTrace();
			}
		} else {
			// connect fail
			chatClientFrame.handleServerCrash();
			this.close();
		}
	}

	private void handleReadableKey(SelectionKey selectionKey) {
		SocketChannel sc = (SocketChannel) selectionKey.channel();
		ByteBuffer readBuffer = ByteBuffer.allocate(1024);
		int readBytes;
		try {
			readBytes = sc.read(readBuffer);
		} catch (IOException e) {
			this.handleServerCrashAndClose(selectionKey);
			e.printStackTrace();
			return;
		}
		if (readBytes > 0) {
			this.handleReceivedMessage(readBuffer);
		} else if (readBytes < 0) {
			this.handleServerCrashAndClose(selectionKey);
		}
	}

	private void handleReceivedMessage(ByteBuffer readBuffer) {
		readBuffer.flip();
		byte[] bytes = new byte[readBuffer.remaining()];
		readBuffer.get(bytes);
		String body = new String(bytes, StandardCharsets.UTF_8);
		String decryptedMessage;
		try {
			decryptedMessage = AESUtils.decrypt(body, AES_SECRET_KEY);
		} catch (Exception e) {
			decryptedMessage = body;
		}
		chatClientFrame.setTextAreaText(decryptedMessage);
	}

	private void handleServerCrashAndClose(SelectionKey selectionKey) {
		chatClientFrame.handleServerCrash();
		this.closeSelectionKey(selectionKey);
	}

	private void closeSelectionKey(SelectionKey selectionKey) {
		if (selectionKey != null) {
			selectionKey.cancel();
			if (selectionKey.channel() != null) {
				try {
					selectionKey.channel().close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	private void connect() throws IOException {
		if (!socketChannel.connect(new InetSocketAddress(SERVER_IP, Constant.SERVER_PORT))) {
			socketChannel.register(selector, SelectionKey.OP_CONNECT);
		}
	}

	/**
	 * close clint
	 *
	 * @author yanganyu
	 * @date 2018/11/8 16:07
	 */
	void close() {
		if (running) {
			running = false;
			if (selector != null) {
				try {
					selector.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	/**
	 * send message to server
	 *
	 * @author yanganyu
	 * @date 11/10/18 10:22 AM
	 */
	void sendMessageToServer(String text) {
		try {
			String encryptedString = AESUtils.encrypt((sendMessagePrefix + text), AES_SECRET_KEY);
			if (encryptedString == null) {
				System.exit(-1);
			}
			byte[] req = encryptedString.getBytes();
			ByteBuffer writeBuffer = ByteBuffer.allocate(req.length);
			writeBuffer.put(req);
			writeBuffer.flip();
			socketChannel.write(writeBuffer);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}
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
