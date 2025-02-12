/**
 * The MessageHandler class is responsible for handling incoming messages for
 * the chat client.
 * It extends the Worker class and processes messages from the client's message
 * queue.
 * 
 * <p>
 * This class specifically handles messages of type CHAT_MESSAGE. Other message
 * types are ignored.
 * Messages related to P2P communication are handled by custom handlers in the
 * Peer class.
 * </p>
 * 
 * @see de.luh.vss.chat.client.ChatClient
 * @see de.luh.vss.chat.common.Message
 * @see de.luh.vss.chat.common.MessageType
 * @see de.luh.vss.chat.utils.Worker
 */

package de.luh.vss.chat.client;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import de.luh.vss.chat.client.Wrapper.MessageWrapper;
import de.luh.vss.chat.common.Message;
import de.luh.vss.chat.common.MessageType;
import de.luh.vss.chat.utils.*;

public class MessageHandler extends Worker {
	private ChatClient chatClient = null;
	public ArrayList<MessageWrapper> bufferQ = new ArrayList<MessageWrapper>();

	public MessageHandler(ChatClient chatClient) {
		super("MessageHandler");
		this.chatClient = chatClient;
	}

	@Override
	public int task() throws InterruptedException {
		MessageWrapper msgwrp = chatClient.mr.getInQueue().poll(Settings.POLLING_RATE_MS, TimeUnit.MILLISECONDS);

		if (msgwrp == null)
			return 0;

		if (msgwrp.content.getMessageType() != MessageType.CHAT_MESSAGE)
			return 0;

		Message.ChatMessage chatMsg = (Message.ChatMessage) msgwrp.content;

		System.out.println(chatMsg);

		/**
		 * Here handle messages for normal chat client.
		 * All messages concerning P2P are handled using custom handlers in Peer.java.
		 */
		return 0;
	}

	protected void refresh() throws IOException {
	};

	protected void clean() throws IOException {
	}
}