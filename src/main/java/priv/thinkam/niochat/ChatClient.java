package priv.thinkam.niochat;

import priv.thinkam.niochat.common.Constant;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

/**
 * chat client
 *
 * @author yanganyu
 * @date 2018/11/7 15:04
 */
public class ChatClient {
	/**
	 * server IP
	 */
	private static final String SERVER_IP = "127.0.0.1";
	private Selector selector;
	private SocketChannel socketChannel;
	private String prefix;
	private BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));
	private volatile boolean running = true;

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

	/**
	 * client thread
	 *
	 * @author yanganyu
	 * @date 2018/11/7 15:48
	 */
	public void start() {
		try {
			doConnect();
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}
		while (running) {
			try {
				selector.select(1000);
				Set<SelectionKey> selectedKeys = selector.selectedKeys();
				Iterator<SelectionKey> it = selectedKeys.iterator();
				SelectionKey key;
				while (it.hasNext()) {
					key = it.next();
					it.remove();
					try {
						handleInput(key);
					} catch (Exception e) {
						if (key != null) {
							key.cancel();
							if (key.channel() != null) {
								key.channel().close();
							}
						}
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
				System.exit(1);
			}
		}

		// 多路复用器关闭后，所有注册在上面的Channel和Pipe等资源都会被自动去注册并关闭，所以不需要重复释放资源
//		if (selector != null) {
//			try {
//				selector.close();
//			} catch (IOException e) {
//				e.printStackTrace();
//			}
//	}

	}

	private void handleInput(SelectionKey key) throws IOException {
		if (key.isValid()) {
			// 判断是否连接成功
			SocketChannel socketChannel = (SocketChannel) key.channel();
			if (key.isConnectable()) {
				if (socketChannel.finishConnect()) {
					socketChannel.register(selector, SelectionKey.OP_READ);
					new Thread(() -> sendMessage(socketChannel)).start();
				} else {
					// 连接失败，进程退出
					System.exit(-1);
				}
			} else if (key.isReadable()) {
				ByteBuffer readBuffer = ByteBuffer.allocate(1024);
				int readBytes = socketChannel.read(readBuffer);
				if (readBytes > 0) {
					readBuffer.flip();
					byte[] bytes = new byte[readBuffer.remaining()];
					readBuffer.get(bytes);
					String body = new String(bytes, StandardCharsets.UTF_8);
					System.out.println(body);
				} else if (readBytes < 0) {
					// 对端链路关闭
					key.cancel();
					socketChannel.close();
				}

				socketChannel.register(selector, SelectionKey.OP_READ);
			}
		}
	}

	private void doConnect() throws IOException {
		if (!socketChannel.connect(new InetSocketAddress(SERVER_IP, Constant.SERVER_PORT))) {
			socketChannel.register(selector, SelectionKey.OP_CONNECT);
		}
	}

	/**
	 * stop client
	 *
	 * @author yanganyu
	 * @date 2018/11/8 16:07
	 */
	private void stop() {
		running = false;
		try {
			bufferedReader.close();
			socketChannel.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void sendMessage(SocketChannel socketChannel) {
		try {
			while (true) {
				String text = bufferedReader.readLine();
				if (text == null) {
					System.out.println("!!!! read text error!!!!");
					System.exit(-1);
				}
				text = text.trim();
				if (Constant.STOP_COMMAND.equals(text)) {
					this.stop();
				}
				byte[] req = (prefix + text).getBytes();
				ByteBuffer writeBuffer = ByteBuffer.allocate(req.length);
				writeBuffer.put(req);
				writeBuffer.flip();
				socketChannel.write(writeBuffer);
			}
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}

	public static void main(String[] args) {
		ChatClient chatClient = new ChatClient();
		CountDownLatch latch = new CountDownLatch(1);
		System.out.print("please enter your username: ");
		new Thread(() -> {
			try {
				String text = chatClient.bufferedReader.readLine();
				if (text == null) {
					System.out.println("!!!! read name error !!!!");
					System.exit(-1);
				}
				chatClient.prefix = text.trim() + ": ";
				latch.countDown();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}).start();
		try {
			latch.await();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			e.printStackTrace();
		}
		chatClient.start();
	}
}
