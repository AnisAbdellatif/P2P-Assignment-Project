package de.luh.vss.chat.p2p;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import de.luh.vss.chat.client.Settings;
import de.luh.vss.chat.client.Wrapper;
import de.luh.vss.chat.client.Wrapper.ByteArrWrapper;
import de.luh.vss.chat.client.Wrapper.MessageWrapper;
import de.luh.vss.chat.common.Message;
import de.luh.vss.chat.common.User;
import de.luh.vss.chat.utils.*;

/**
 * <p>
 * The PeerManagementServer class is responsible for managing peer connections
 * and handling peer communication in a peer-to-peer chat application.
 * It listens for incoming peer connections on a specified port and delegates
 * the handling of each connection to a separate thread.
 * 
 * The server maintains a list of connected peers and provides functionalities
 * for registering peers, listing connected peers, and looking up peers by file name.
 * 
 * The server also handles shutdown operations gracefully by cleaning up resources
 * and terminating threads.
 * </p>
 * 
 * The main components of this class include:
 * <pre>
 * - ExecutorService for managing threads.
 * - PeersManager for managing peer information.
 * - PeerHandler for handling individual peer communication.
 * </pre>
 * 
 * Methods:
 * <pre>
 * - clean(): Shuts down the executor service and closes the peers manager.
 * - main(String[] args): The main method that starts the server and listens for incoming connections.
 * </pre>
 * 
 * Inner Class:
 * <pre>
 * - PeerHandler: Handles communication with individual peers, processes different types of messages,
 *   and performs actions such as registering peers, listing peers, and looking up peers by file name.
 * </pre>
 */
public class PeerManagementServer {
	private static final int port = Settings.PMS_serverPort; // Central server's listening port

	private static final ExecutorService executor = Executors.newFixedThreadPool(10);
	private static final PeersManager peersManager = new PeersManager();

	public static void clean() {
		executor.shutdownNow();
		try {
			executor.awaitTermination(1, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		peersManager.close();
	}

	public static void main(String[] args) {
		Thread peersManager_t = new Thread(peersManager, "PeersManager");
		peersManager_t.start();

		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			clean();
		}));

		try (ServerSocket serverSocket = new ServerSocket(port)) {
			System.out.println("PMS is running on port " + port);

			Socket socket;
			while (true) {
				// Accept incoming connections from peers
				socket = serverSocket.accept();
				System.out.println("New TCP connection from: " + socket.getInetAddress() + ":" + socket.getPort());
				socket.setKeepAlive(true);
				// Create a new thread to handle the peer
				executor.submit(new PeerHandler(socket));
			}
		} catch (IOException e) {
			System.err.println("Error in PMS: " + e.getMessage());
		}
		clean();

		peersManager.close();
		try {
			peersManager_t.join();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	// Class to handle individual peer communication
	private static class PeerHandler extends TCP_Client {
		private final User.UserId userId = new User.UserId(0);

		public PeerHandler(Socket socket) throws IOException {
			super(socket, null, PeerHandler::receiveCallback);
		}

		public static void receiveCallback(NetClient client, Object receiveCallbackObj, ByteArrWrapper bytearrwrp) {
			MessageWrapper msgwrp = Wrapper.byteArrWrpToMsgWrp(bytearrwrp);
			if (msgwrp == null)
				return;

			PeerHandler peerHandler = (PeerHandler) client;

			switch (msgwrp.content.getMessageType()) {
				case CHAT_MESSAGE:
					peerHandler.handleChatMessage(msgwrp);
					break;
				case ERROR_RESPONSE:
					// handleErrorResponse(msgwrp);
					break;
				case REGISTER_RESPONSE:
					// handleRegisterResponse(msgwrp);
					break;
				default:
					System.out.println("Uknown Type of message received!");
					break;
			}
		}

		private void handleChatMessage(MessageWrapper msgwrp) {
			Message.ChatMessage chatMsg = (Message.ChatMessage) msgwrp.content;

			if (chatMsg.getMessage().contentEquals("TEST1: Spawn Peers")) {
				try {
					Peer peer1 = new Peer(new User.UserId(1));
					Message.ChatMessage response = new Message.ChatMessage(this.userId, "Spawned peer with id: 1");
					MessageWrapper reswrp = new MessageWrapper(msgwrp.connInfo, response);
					this.send(Wrapper.msgWrpToByteArrWrp(reswrp));
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				return;
			}

			Command cmd = Command.fromString(chatMsg.getMessage().split("\\|", 2)[0]);

			switch (cmd) {
				case Command.REGISTER_REQ:
					System.out.println("Received register message!");
					handleRegister(msgwrp);
					break;
				case Command.LIST_REQ:
					System.out.println("Received list message!");
					handleList(msgwrp);
					break;
				case Command.LOOKUP_REQ:
					System.out.println("Received lookup message!");
					handleLookup(msgwrp);
					break;
				case Command.INDEF:
					System.out.println("Unknown command!");
					break;
				default:
					break;
			}

			this.close();
		}

		private void handleRegister(MessageWrapper msgwrp) {
			Message.ChatMessage chatMsg = (Message.ChatMessage) msgwrp.content;

			PeerInfo peerInfo = StringFormatUtils.parsePeerInfo(chatMsg.getMessage().split("\\|")[1],
					chatMsg.getRecipient().id());
			peersManager.addPeer(peerInfo, chatMsg.getMessage().split("\\|")[2].split("\\;"));

			System.out.println("Peer registered: " + peerInfo);

			Message.ChatMessage registerSuccess = new Message.ChatMessage(this.userId, "REGISTER_SUCCESS|" + peerInfo);
			System.out.println(registerSuccess);

			MessageWrapper reswrp = new MessageWrapper(msgwrp.connInfo, registerSuccess);
			try {
				this.send(Wrapper.msgWrpToByteArrWrp(reswrp));
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		private void handleList(MessageWrapper msgwrp) {
			String messageStr = Command.LIST_RES + "|Here are all the connected peers:\n";
			messageStr += StringFormatUtils.PRM_ToString(peersManager.getPeersRegistry());
			messageStr += StringFormatUtils.FRM_ToString(peersManager.getFilesRegistry());

			Message.ChatMessage resMsg = new Message.ChatMessage(this.userId, messageStr.toString());
			MessageWrapper reswrp = new MessageWrapper(msgwrp.connInfo, resMsg);

			System.out.println(resMsg);
			// System.out.println(StringFormatUtils.FPM_ToString(filesToPeers));

			try {
				this.send(Wrapper.msgWrpToByteArrWrp(reswrp));
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		private void handleLookup(MessageWrapper msgwrp) {
			Message.ChatMessage chatMsg = (Message.ChatMessage) msgwrp.content;
			String fileName = chatMsg.getMessage().split("\\|", 2)[1];
			Set<PeerInfo> peers = peersManager.getFilePeers(fileName);

			Message.ChatMessage resMsg;

			if (peers == null) {
				String resStr = "LOOKUP_RES|NOT_FOUND";
				resMsg = new Message.ChatMessage(this.userId, resStr);
			} else {
				String resStr = "LOOKUP_RES|FOUND|Peers List: ";
				resStr += peers.toString();
				resMsg = new Message.ChatMessage(this.userId, resStr);
			}
			MessageWrapper reswrp = new MessageWrapper(msgwrp.connInfo, resMsg);
			try {
				this.send(Wrapper.msgWrpToByteArrWrp(reswrp));
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

}
