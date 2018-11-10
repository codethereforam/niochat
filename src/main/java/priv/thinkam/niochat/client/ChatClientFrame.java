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
class ChatClientFrame extends Frame {
	private static final int LENGTH = 300;
	private static final int HEIGHT = 400;

	private StringBuilder textAreaText;
	private TextArea textArea;
	private TextField textField;
	private ChatClient chatClient;
	private boolean hasName = false;

	ChatClientFrame(ChatClient chatClient) {
		this.chatClient = chatClient;
		this.init();
	}

	/**
	 * init component and frame
	 *
	 * @author yanganyu
	 * @date 11/10/18 10:19 AM
	 */
	private void init() {
		initTextAreaText();
		initTextArea();
		initTextField();
		initFrame();
	}

	/**
	 * init frame
	 *
	 * @author yanganyu
	 * @date 11/10/18 10:18 AM
	 */
	private void initFrame() {
		this.setTitle("niochat client");
		this.setBounds(400, 0, LENGTH, HEIGHT);
		this.setLayout(new BorderLayout());
		this.add(textArea);
		this.add(textField, BorderLayout.SOUTH);
		this.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				chatClient.stop();
				System.exit(0);
			}
		});
		this.setVisible(true);
	}

	/**
	 * init TextField
	 *
	 * @author yanganyu
	 * @date 11/10/18 10:12 AM
	 */
	private void initTextField() {
		textField = new TextField();
		textField.setBackground(new Color(23, 33, 43));
		textField.setForeground(Color.white);
		textField.setPreferredSize(new Dimension(0, 30));
		textField.requestFocusInWindow();
		textField.addActionListener(e -> {
			String text = textField.getText().trim();
			textField.setText(StringUtils.EMPTY_STRING);
			if (hasName) {
				if (StringUtils.isNotBlank(text)) {
					textAreaText.append("me: ").append(text).append("\n");
					textArea.setText(textAreaText.toString());
					chatClient.sendMessageToServer(text);
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

	/**
	 * init TextArea Text
	 *
	 * @author yanganyu
	 * @date 11/10/18 10:07 AM
	 */
	private void initTextAreaText() {
		textAreaText = new StringBuilder();
		textAreaText.append("wait counterpart online...").append("\n");
		textAreaText.append("please enter your username: ").append("\n");
	}

	/**
	 * init TextArea
	 *
	 * @author yanganyu
	 * @date 11/10/18 10:04 AM
	 */
	private void initTextArea() {
		textArea = new TextArea();
		textArea.setText(textAreaText.toString());
		textArea.setBackground(new Color(14, 22, 33));
		textArea.setForeground(Color.white);
		textArea.setEditable(false);
	}

	/**
	 * set TextArea text
	 *
	 * @author yanganyu
	 * @date 11/10/18 10:12 AM
	 */
	void setTextAreaText(String text) {
		textAreaText.append(text).append("\n");
		textArea.setText(textAreaText.toString());
	}
}
