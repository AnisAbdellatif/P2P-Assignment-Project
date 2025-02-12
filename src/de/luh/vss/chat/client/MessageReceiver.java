/**
 * The MessageReceiver class is responsible for receiving and processing
 * messages
 * from a network client. It extends the Worker class and utilizes a
 * LinkedBlockingQueue to manage incoming messages.
 * 
 * <p>
 * It provides methods to handle different types of messages such as chat
 * messages, error responses, and register responses. It also includes methods
 * for adding messages to the queue, processing tasks, and cleaning up
 * resources.
 * 
 * <p>
 * Usage:
 * 
 * <pre>
 * {@code
 * MessageReceiver receiver = new MessageReceiver();
 * receiver.task();
 * }
 * </pre>
 * 
 * <p>
 * Methods:
 * <ul>
 * <li>{@link #MessageReceiver()}: Constructor to initialize the
 * MessageReceiver.
 * <li>{@link #getInQueue()}: Returns the internal queue of MessageWrapper
 * objects.
 * <li>{@link #receivedDataCallback(NetClient, Object, ByteArrWrapper)}:
 * Callback method for receiving data.
 * <li>{@link #addToQueue(ByteArrWrapper)}: Adds a ByteArrWrapper to the receive
 * queue.
 * <li>{@link #task()}: Processes messages from the receive queue.
 * <li>{@link #getMessage(ByteArrWrapper, DataInputStream)}: Parses and handles
 * a message from the input stream.
 * <li>{@link #handleChatMessage(MessageWrapper)}: Handles a chat message.
 * <li>{@link #handleErrorResponse(MessageWrapper)}: Handles an error response
 * message.
 * <li>{@link #handleRegisterResponse(MessageWrapper)}: Handles a register
 * response message.
 * <li>{@link #refresh()}: Refreshes the receiver (currently empty
 * implementation).
 * <li>{@link #clean()}: Cleans up resources by closing input streams.
 * </ul>
 * 
 * <p>
 * Exceptions:
 * <ul>
 * <li>{@link IOException}: Thrown when an I/O error occurs.
 * <li>{@link InterruptedException}: Thrown when a thread is interrupted.
 * </ul>
 * 
 * <p>
 * Dependencies:
 * <ul>
 * <li>{@link de.luh.vss.chat.common.Message}
 * <li>{@link de.luh.vss.chat.utils.Worker}
 * <li>{@link de.luh.vss.chat.utils.NetClient}
 * <li>{@link de.luh.vss.chat.client.Wrapper}
 * </ul>
 */

package de.luh.vss.chat.client;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.SocketException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import de.luh.vss.chat.common.Message;
import de.luh.vss.chat.utils.Worker;
import de.luh.vss.chat.utils.NetClient;
import de.luh.vss.chat.utils.NetClient.LBQ_BAWRP;
import de.luh.vss.chat.client.Wrapper.*;

public class MessageReceiver extends Worker {
	protected LBQ_BAWRP receiveQ = new LBQ_BAWRP();
	BufferedInputStream bufferedIn = null;
	DataInputStream inputStream = null;

	private LinkedBlockingQueue<MessageWrapper> inQueue = new LinkedBlockingQueue<MessageWrapper>();

	public MessageReceiver() {
		super("MessageReceiver");
		receiveQ.clear();
	}

	public LinkedBlockingQueue<MessageWrapper> getInQueue() {
		return inQueue;
	}

	public static void receivedDataCallback(NetClient netClient, Object receiveCallbackObj, ByteArrWrapper byteArrWrp) {
		MessageReceiver mr = (MessageReceiver) receiveCallbackObj;
		mr.addToQueue(byteArrWrp);
	}

	public void addToQueue(ByteArrWrapper buffer) {
		try {
			receiveQ.put(buffer);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public int task() throws InterruptedException {
		ByteArrWrapper bufferwrp = null;
		bufferwrp = receiveQ.poll(Settings.POLLING_RATE_MS, TimeUnit.MILLISECONDS);

		if (bufferwrp == null || bufferwrp.content.length == 0)
			return 0;

		BufferedInputStream bufferedIn = new BufferedInputStream(new ByteArrayInputStream(bufferwrp.content));
		DataInputStream inputStream = new DataInputStream(bufferedIn);

		try {
			while (bufferedIn.available() > 0) {
				getMessage(bufferwrp, inputStream);
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			try {
				bufferedIn.close();
				inputStream.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		return 0;
	}

	private int getMessage(ByteArrWrapper bufferwrp, DataInputStream inputStream) throws IOException {
		Message message = null;
		try {
			message = Message.parse(inputStream);
		} catch (Exception e) {
			e.printStackTrace();
			if (e instanceof SocketException || e instanceof EOFException) {
				paused = true;
				clean();
				return 0;
			}
			System.out.println("Received out of bounds message!");
			bufferedIn.reset();
			System.out.println(inputStream.readUTF());
		} finally {
			clean();
			if (message == null)
				return -1;
		}

		MessageWrapper msgwrp = new MessageWrapper(bufferwrp.connInfo.sType, bufferwrp.connInfo.remoteAddr,
				bufferwrp.connInfo.remotePort, message);

		switch (message.getMessageType()) {
			case CHAT_MESSAGE:
				handleChatMessage(msgwrp);
				break;
			case ERROR_RESPONSE:
				handleErrorResponse(msgwrp);
				break;
			case REGISTER_RESPONSE:
				handleRegisterResponse(msgwrp);
				break;
			default:
				System.out.println("Uknown Type of message received!");
				return -1;
		}
		return 0;
	}

	private void handleChatMessage(MessageWrapper msgwrp) {
		Message.ChatMessage chatMessage = (Message.ChatMessage) msgwrp.content;

		System.out.printf("Received ChatMessage over %s from %s:%d : %s\n", msgwrp.connInfo.sType,
				msgwrp.connInfo.remoteAddr, msgwrp.connInfo.remotePort, chatMessage.toString());

		try {
			inQueue.put(msgwrp);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	private void handleErrorResponse(MessageWrapper msgwrp) {
		Message.ErrorResponse errorResponse = (Message.ErrorResponse) msgwrp.content;
		System.out.printf("Received ErrorResponse from %s:%d : %s\n", msgwrp.connInfo.remoteAddr,
				msgwrp.connInfo.remotePort, errorResponse.toString());
	}

	private void handleRegisterResponse(MessageWrapper msgwrp) {
		Message.RegisterResponse registerResponse = (Message.RegisterResponse) msgwrp.content;
		System.out.printf("Received RegisterResponse from %s:%d : \n", msgwrp.connInfo.remoteAddr,
				msgwrp.connInfo.remotePort, registerResponse.toString());
	}

	public void refresh() throws IOException {
	}

	public void clean() throws IOException {
		if (bufferedIn != null)
			bufferedIn.close();
		if (inputStream != null)
			inputStream.close();
	}
}