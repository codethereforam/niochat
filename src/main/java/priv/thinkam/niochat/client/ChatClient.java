package priv.thinkam.niochat.client;

import priv.thinkam.niochat.common.Constant;
import priv.thinkam.niochat.util.AESUtils;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
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
			doConnect();
		} catch (IOException e) {
			e.printStackTrace();
			this.close();
			System.exit(-1);
		}
		while (running) {
			try {
				selector.select(1000);
			} catch (IOException e) {
				e.printStackTrace();
				this.close();
				return;
			}
			if (!selector.isOpen()) {
				return;
			}
			Set<SelectionKey> selectedKeys = selector.selectedKeys();
			Iterator<SelectionKey> it = selectedKeys.iterator();
			SelectionKey selectionKey;
			while (it.hasNext()) {
				selectionKey = it.next();
				it.remove();
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
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	private void handleSelectionKey(SelectionKey selectionKey) throws IOException {
		if (selectionKey.isValid()) {
			SocketChannel sc = (SocketChannel) selectionKey.channel();
			if (selectionKey.isConnectable()) {
				boolean finishConnected = false;
				try {
					finishConnected = sc.finishConnect();
				} catch (IOException e) {
					System.out.println("can not connected to server...");
					e.printStackTrace();
				}
				if (finishConnected) {
					sc.register(selector, SelectionKey.OP_READ);
				} else {
					// connect fail
					chatClientFrame.handleServerCrash();
					this.close();
				}
			} else if (selectionKey.isReadable()) {
				ByteBuffer readBuffer = ByteBuffer.allocate(1024);
				int readBytes;
				try {
					readBytes = sc.read(readBuffer);
				} catch (IOException e) {
					this.handleServerCrashAndClose(sc, selectionKey);
					e.printStackTrace();
					return;
				}
				if (readBytes > 0) {
					this.handleReceivedMessage(readBuffer);
				} else if (readBytes < 0) {
					this.handleServerCrashAndClose(sc, selectionKey);
				}
			}
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

	private void handleServerCrashAndClose(SocketChannel socketChannel, SelectionKey selectionKey) {
		chatClientFrame.handleServerCrash();
		selectionKey.cancel();
		try {
			socketChannel.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		this.close();
	}

	private void doConnect() throws IOException {
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
