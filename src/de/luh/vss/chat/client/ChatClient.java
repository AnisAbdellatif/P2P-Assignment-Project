package de.luh.vss.chat.client;

import java.io.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import de.luh.vss.chat.common.*;
import de.luh.vss.chat.utils.*;

/**
 * The ChatClient class is responsible for managing the chat client operations.
 * It initializes the necessary components for communication and handles the
 * execution of various tasks using an ExecutorService.
 * 
 * <p>It supports both TCP and UDP communication protocols and provides methods
 * to start and clean up the client operations.</p>
 * 
 * <p>Usage:</p>
 * <pre>
 * {@code
 * ChatClient client = new ChatClient();
 * client.start();
 * }
 * </pre>
 * 
 * <p>Constructor Details:</p>
 * <ul>
 * <li>{@link #ChatClient(User.UserId, String, int)}: Initializes the client with a specific user ID, server address, and TCP server port.</li>
 * <li>{@link #ChatClient()}: Initializes the client with default settings.</li>
 * </ul>
 * 
 * <p>Method Details:</p>
 * <ul>
 * <li>{@link #intialize()}: Initializes the message receiver, TCP client, UDP client, message sender, and message handler.</li>
 * <li>{@link #start()}: Starts the execution of the message receiver, TCP client, UDP client, message sender, and message handler.</li>
 * <li>{@link #clean()}: Shuts down the executor service and stops all running tasks.</li>
 * <li>{@link #getServerAddressString()}: Returns the server address as a string.</li>
 * <li>{@link #getTcpServerPort()}: Returns the TCP server port.</li>
 * </ul>
 * 
 * <p>Fields:</p>
 * <ul>
 * <li>{@link #userId}: The user ID associated with the chat client.</li>
 * <li>{@link #executor}: The executor service used to manage concurrent tasks.</li>
 * <li>{@link #mr}: The message receiver instance.</li>
 * <li>{@link #ms}: The message sender instance.</li>
 * <li>{@link #tcpc}: The TCP client instance.</li>
 * <li>{@link #udpc}: The UDP client instance.</li>
 * <li>{@link #mh}: The message handler instance.</li>
 * <li>{@link #serverAddress}: The server address for the chat client.</li>
 * <li>{@link #tcpServerPort}: The TCP server port for the chat client.</li>
 * </ul>
 */
public class ChatClient {
	public User.UserId userId = new User.UserId(Settings.PIN);
	protected ExecutorService executor = Executors.newFixedThreadPool(5);

	public MessageReceiver mr;
	public MessageSender ms;
	public TCP_Client tcpc;
	public UDP_Client udpc;
	public MessageHandler mh;

	protected String serverAddress = Settings.PMS_serverAddress;
	protected int tcpServerPort = Settings.PMS_serverPort;

	public ChatClient(User.UserId userId, String serverAddress, int tcpServerPort) throws IOException {
		this.userId = userId;
		this.serverAddress = serverAddress;
		this.tcpServerPort = tcpServerPort;
		intialize();
	}

	public ChatClient() throws IOException {
		intialize();
	}

	public void intialize() throws IOException {
		this.mr = new MessageReceiver();
		this.tcpc = new TCP_Client(Settings.PMS_serverAddress, Settings.PMS_serverPort, this.mr,
				MessageReceiver::receivedDataCallback);
		this.udpc = new UDP_Client(this.mr, MessageReceiver::receivedDataCallback);
		this.ms = new MessageSender(this);
		this.mh = new MessageHandler(this);
	}

	public void start() {

		executor.submit(mr);
		executor.submit(tcpc);
		executor.submit(udpc);
		executor.submit(ms);
		executor.submit(mh);
	}

	public void clean() {
		executor.shutdownNow();
	}

	public String getServerAddressString() {
		return this.serverAddress;
	}

	public int getTcpServerPort() {
		return this.tcpServerPort;
	}
}
