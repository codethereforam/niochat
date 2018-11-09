package priv.thinkam.niochat.client;

import priv.thinkam.niochat.util.StringUtils;

import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * chat client frame
 *
 * @author thinkam
 * @date 2018/11/09
 */
public class ChatClientFrame extends Frame {
	private static final int LENGTH = 300;
	private static final int HEIGHT = 400;

	private ChatClient chatClient;
	private TextArea textArea = new TextArea();
	private TextField textField = new TextField();
	private boolean hasName = false;

	private StringBuilder textAreaText = new StringBuilder();

	private ChatClientFrame(ChatClient chatClient) {
		this.chatClient = chatClient;
		this.init();
	}

	private void init() {
		this.setTitle("niochat client");
		this.setBounds(400, 0, LENGTH, HEIGHT);
		this.setLayout(new BorderLayout());
		textAreaText.append("wait counterpart online...").append("\n");
		textAreaText.append("please enter your username: ").append("\n");
		textArea.setText(textAreaText.toString());
		textArea.setEditable(false);
		textArea.setEnabled(false);
		this.add(textArea);
		this.add(textField, BorderLayout.SOUTH);

		this.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				chatClient.stop();
				System.exit(0);
			}
		});
		this.setResizable(true);
		this.setVisible(true);

		textField.addActionListener(e -> {
			String text = textField.getText().trim();
			textField.setText(StringUtils.EMPTY_STRING);
			if(hasName) {
				if(StringUtils.isNotBlank(text)) {
					textAreaText.append("me: ").append(text).append("\n");
					textArea.setText(textAreaText.toString());
					chatClient.sendMessage(text);
				}
			} else {
				hasName = true;
				chatClient.setSendMessagePrefix(text + ": ");
				textAreaText.append(text).append("\n");
				textAreaText.append("chat start...").append("\n");
				textArea.setText(textAreaText.toString());
			}
		});
	}

	public void setText(String text) {
		textAreaText.append(text).append("\n");
		textArea.setText(textAreaText.toString());
	}

	public static void main(String[] args) {
		ChatClient chatClient = new ChatClient();
		ChatClientFrame chatClientFrame = new ChatClientFrame(chatClient);
		chatClient.setChatClientFrame(chatClientFrame);
		chatClient.start();
	}
}
