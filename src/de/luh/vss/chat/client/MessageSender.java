/**
 * The MessageSender class is responsible for sending messages from the chat
 * client
 * to the server using either TCP or UDP protocols. It extends the Worker class
 * and
 * manages a queue of messages to be sent.
 * 
 * <p>
 * It provides methods to send messages, add messages to the top of the queue,
 * and swap user IDs in messages. It also handles the serialization of messages
 * and sending them over the network.
 * </p>
 * 
 * <p>
 * Constructor:
 * </p>
 * <ul>
 * <li>{@link #MessageSender(ChatClient)}: Initializes the MessageSender with a
 * reference to the ChatClient.</li>
 * </ul>
 * 
 * <p>
 * Methods:
 * </p>
 * <ul>
 * <li>{@link #send(MessageWrapper)}: Adds a message to the queue or to the top
 * of the queue if it is a register request.</li>
 * <li>{@link #getOutQueue()}: Returns the queue of outgoing messages.</li>
 * <li>{@link #addToTop(MessageWrapper)}: Adds a message to the top of the
 * queue.</li>
 * <li>{@link #swapUserId(MessageWrapper)}: Swaps the user ID in the message
 * with the chat client's user ID.</li>
 * <li>{@link #task()}: Processes and sends the next message in the queue.</li>
 * <li>{@link #refresh()}: Refreshes the state of the MessageSender (currently
 * does nothing).</li>
 * <li>{@link #clean()}: Cleans up resources used by the MessageSender.</li>
 * </ul>
 * 
 * <p>
 * Fields:
 * </p>
 * <ul>
 * <li>{@link #chatClient}: Reference to the ChatClient instance.</li>
 * <li>{@link #tcpc}: Reference to the TCP_Client instance.</li>
 * <li>{@link #udpc}: Reference to the UDP_Client instance.</li>
 * <li>{@link #baos}: ByteArrayOutputStream used for message serialization.</li>
 * <li>{@link #dos}: DataOutputStream used for message serialization.</li>
 * <li>{@link #outQueue}: Queue of messages to be sent.</li>
 * </ul>
 */

package de.luh.vss.chat.client;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import de.luh.vss.chat.common.Message;
import de.luh.vss.chat.common.MessageType;
import de.luh.vss.chat.utils.NetClient;
import de.luh.vss.chat.utils.TCP_Client;
import de.luh.vss.chat.utils.UDP_Client;
import de.luh.vss.chat.utils.Worker;
import de.luh.vss.chat.client.Wrapper.*;

public class MessageSender extends Worker {
	private final ChatClient chatClient;
	private final TCP_Client tcpc;
	private final UDP_Client udpc;

	private final ByteArrayOutputStream baos = new ByteArrayOutputStream();
	private final DataOutputStream dos = new DataOutputStream(baos);

	private LinkedBlockingQueue<MessageWrapper> outQueue = new LinkedBlockingQueue<MessageWrapper>();

	public MessageSender(ChatClient chatClient) {
		super("MessageSender");
		this.chatClient = chatClient;
		this.tcpc = chatClient.tcpc;
		this.udpc = chatClient.udpc;
	}

	public void send(MessageWrapper msgwrp) {
		try {
			if (msgwrp.content.getMessageType() == MessageType.REGISTER_REQUEST) {
				addToTop(msgwrp);
			} else {
				outQueue.put(msgwrp);
			}
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public LinkedBlockingQueue<MessageWrapper> getOutQueue() {
		return outQueue;
	}

	public void addToTop(MessageWrapper msgwrp) {
		try {
			LinkedBlockingQueue<MessageWrapper> tempQueue = new LinkedBlockingQueue<>(outQueue.size() + 1);
			tempQueue.put(msgwrp);
			tempQueue.addAll(outQueue);
			outQueue.clear();
			outQueue.addAll(tempQueue);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private MessageWrapper swapUserId(MessageWrapper msgwrp) {
		msgwrp.content = new Message.ChatMessage(chatClient.userId,
				((Message.ChatMessage) msgwrp.content).getMessage());
		return msgwrp;
	}

	public int task() throws InterruptedException {
		MessageWrapper msgwrp = outQueue.poll(Settings.POLLING_RATE_MS, TimeUnit.MILLISECONDS);

		if (msgwrp == null)
			return 0;

		try {

			msgwrp = swapUserId(msgwrp);
			msgwrp.content.toStream(dos);
			dos.flush();
			byte[] buffer = baos.toByteArray();
			baos.reset();

			System.out.printf("%s: Sending to %s:%d : %s\n", msgwrp.connInfo.sType, msgwrp.connInfo.remoteAddr,
					msgwrp.connInfo.remotePort, msgwrp.content);

			switch (msgwrp.connInfo.sType) {
				case NetClient.SocketType.TCP:
					tcpc.send(new ByteArrWrapper(NetClient.SocketType.TCP, tcpc.remoteAddr, tcpc.remotePort, buffer));
					break;
				case NetClient.SocketType.UDP:
					udpc.send(new ByteArrWrapper(NetClient.SocketType.TCP, msgwrp.connInfo.remoteAddr,
							msgwrp.connInfo.remotePort, buffer));
					break;
			}
		} catch (Exception e) {
			e.printStackTrace();
			return -1;
		}

		return 0;
	}

	protected void refresh() throws IOException {
	};

	protected void clean() throws IOException {
		outQueue.clear();
		dos.close();
		baos.close();
	}
}