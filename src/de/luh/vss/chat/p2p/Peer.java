package de.luh.vss.chat.p2p;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

import de.luh.vss.chat.client.ChatClient;
import de.luh.vss.chat.client.Settings;
import de.luh.vss.chat.client.Wrapper;
import de.luh.vss.chat.client.Wrapper.*;
import de.luh.vss.chat.common.Message;
import de.luh.vss.chat.common.User;
import de.luh.vss.chat.common.User.UserId;
import de.luh.vss.chat.utils.*;

/**
 * The Peer class extends the ChatClient and represents a peer in a peer-to-peer chat system.
 * It handles file transfers, message sending, and receiving, and peer registration.
 * 
 * <p>Dependencies:
 * - SandboxedFileSystem
 * - ChatClient
 * - Settings
 * - Wrapper
 * - Message
 * - User
 * - NetClient
 * - ByteArrWrapper
 * - ConnectionInfo
 * - MessageWrapper
 * - StringFormatUtils
 * 
 * <p>Usage:
 * <pre>
 * {@code
 * Peer peer = new Peer(new User.UserId(randomId));
 * }
 * </pre>
 * 
 * <p>Example:
 * <pre>
 * {@code
 * Peer peer = new Peer(new User.UserId(1234));
 * peer.sendFile(connInfo, new File("path/to/file"));
 * }
 * </pre>
 * 
 * <p>Methods:
 * <ul>
 *   <li>{@link #Peer(UserId)} - Constructor to initialize a Peer with a given UserId.</li>
 *   <li>{@link #main(String[])} - Main method to run the Peer application.</li>
 *   <li>{@link #registerPeer()} - Registers the peer with the server.</li>
 *   <li>{@link #handleReceivedPacket(NetClient, Object, ByteArrWrapper)} - Handles received packets.</li>
 *   <li>{@link #handleFileRequest(MessageWrapper)} - Handles file requests.</li>
 *   <li>{@link #sendFile(ConnectionInfo, File)} - Sends a file to a specified connection.</li>
 *   <li>{@link #receiveFile(ByteArrWrapper)} - Receives a file from a ByteArrWrapper.</li>
 *   <li>{@link #printFileInfo(File)} - Prints information about a file.</li>
 *   <li>{@link #handleFileLookup(MessageWrapper)} - Handles file lookup responses.</li>
 *   <li>{@link #handleHeartbeatReq(MessageWrapper)} - Handles heartbeat requests.</li>
 * </ul>
 * 
 * <p>Fields:
 * <ul>
 *   <li>{@link #sandboxFS} - Sandboxed file system for secure file operations.</li>
 *   <li>{@link #CHUNK_SIZE} - Size of file chunks for transfer.</li>
 *   <li>{@link #downloadName} - Name of the file being downloaded.</li>
 * </ul>
 * 
 * <p>Exceptions:
 * <ul>
 *   <li>{@link IOException} - Thrown for I/O errors.</li>
 *   <li>{@link UnknownHostException} - Thrown when the IP address of a host could not be determined.</li>
 *   <li>{@link SecurityException} - Thrown for unauthorized file access.</li>
 * </ul>
 * 
 * <p>Notes:
 * <ul>
 *   <li>Ensure that the Settings class is properly configured before running the Peer application.</li>
 *   <li>Handle exceptions appropriately to avoid unexpected termination.</li>
 * </ul>
 */
public class Peer extends ChatClient {
	private final SandboxedFileSystem sandboxFS = new SandboxedFileSystem(Settings.sfs_root);
	private final int CHUNK_SIZE = Settings.CHUNK_SIZE;

	public String downloadName;

	public Peer(UserId userId) throws IOException {
		super(userId, Settings.PMS_serverAddress, Settings.PMS_serverPort);
		this.udpc.setRecvCallback(this, Peer::handleReceivedPacket);
		this.tcpc.setRecvCallback(this, Peer::handleReceivedPacket);
		this.start();
		registerPeer();
	}

	public static void main(String[] args) {
		Peer peer = null;
		try {
			int randomId = new Random().nextInt(9000) + 1000;
			System.out.printf("Spawning user with random ID: %d\n", randomId);
			peer = new Peer(new User.UserId(randomId));
		} catch (Exception e) {
			System.err.println("Error in Peer!");
			e.printStackTrace();
		}

		if (peer == null)
			return;

		Scanner sc = new Scanner(System.in);

		while (true) {
			String msg = sc.nextLine();
			if (msg.equals("q"))
				break;

			Command cmd = Command.fromString(msg.split("\\|", 2)[0]);

			if (cmd != Command.INDEF) {
				NetClient.SocketType sType = cmd == Command.FILE_DOWN ? NetClient.SocketType.UDP
						: NetClient.SocketType.TCP;

				InetAddress rAddress;
				try {
					rAddress = InetAddress.getByName(Settings.PMS_serverAddress);
				} catch (UnknownHostException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					continue;
				}

				int rPort = Settings.PMS_serverPort;
				if (sType == NetClient.SocketType.UDP) {
					String rPortStr = "";
					do {
						System.out.printf("Port: ");
						rPortStr = sc.nextLine();
					} while (rPortStr.length() == 0);
					rPort = Integer.parseInt(rPortStr);
				}

				String queryStr = cmd.toString();
				String fileName = "";
				String downloadName = "";

				if (cmd == Command.LOOKUP_REQ || cmd == Command.FILE_DOWN) {
					System.out.printf("File Name (test.txt): ");
					fileName = sc.nextLine();
					if (fileName.length() == 0)
						fileName = "test.txt";
					queryStr += String.format("|%s", fileName);
				}

				if (cmd == Command.FILE_DOWN) {
					System.out.printf("Download Name (download): ");
					downloadName = sc.nextLine();
					if (downloadName.length() == 0)
						downloadName = "download";
					peer.downloadName = downloadName;
				}

				peer.ms.send(
						new MessageWrapper(sType, rAddress, rPort, new Message.ChatMessage(peer.userId, queryStr)));

				continue;
			} else if (msg.compareTo("test") == 0)
				peer.ms.send(new MessageWrapper(NetClient.SocketType.TCP, peer.tcpc.remoteAddr, peer.tcpc.remotePort,
						new Message.ChatMessage(peer.userId, "TEST1: Spawn Peers")));
			else if (msg.equals("pause send"))
				peer.ms.pauseThread();
			else if (msg.equals("resume send"))
				peer.ms.resumeThread();
			else if (msg.length() > 0)
				peer.ms.send(new MessageWrapper(NetClient.SocketType.TCP, peer.tcpc.remoteAddr, peer.tcpc.remotePort,
						new Message.ChatMessage(peer.userId, msg)));
			else {
			}
		}

		sc.close();
		peer.clean();
		try {
			peer.executor.awaitTermination(1, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("Connection closed.");
		System.exit(0);

	}

	/**
	 * Registers the peer with the server by sending a registration request message.
	 * The message includes the peer's TCP and UDP socket information and a list of
	 * files available in the peer's sandboxed file system.
	 */
	private void registerPeer() {
		String msgContent = String.format("%s|%s:%d|", Command.REGISTER_REQ,
				this.tcpc.getSocket().getLocalAddress().getHostAddress(), this.udpc.getLocalPort());

		File root_directory = sandboxFS.getFile("./");

		// Check if it's a directory
		if (root_directory.isDirectory()) {
			// List all files and directories
			File[] files = root_directory.listFiles();

			if (files != null) {
				for (File file : files) {
					if (file.isFile()) {
						msgContent += file.getName() + ";";
					} else if (file.isDirectory()) {
					}
				}
			}
		} else {
			System.out.println("Not a directory!");
		}

		Message.ChatMessage registerMsg = new Message.ChatMessage(this.userId, msgContent);

		ConnectionInfo connInfo = new ConnectionInfo(NetClient.SocketType.TCP, this.tcpc.remoteAddr,
				this.tcpc.remotePort);
		MessageWrapper msgwrp = new MessageWrapper(connInfo, registerMsg);

		this.ms.send(msgwrp);
	}

	/**
	 * Handles received packets from the network. Depending on the packet type, it
	 * may initiate file transfer, handle registration responses, or process other
	 * commands.
	 *
	 * @param client             The NetClient instance that received the packet.
	 * @param receiveCallbackObj The object to be used as the callback.
	 * @param byteArrWrp         The ByteArrWrapper containing the received packet
	 *                           data.
	 */
	public static void handleReceivedPacket(NetClient client, Object receiveCallbackObj, ByteArrWrapper byteArrWrp) {
		Peer peer = (Peer) receiveCallbackObj;

		if (byteArrWrp.content[2] == 1 || byteArrWrp.content[2] == 2 || byteArrWrp.content[2] == 4
				|| byteArrWrp.content[2] == 8) {
			// This is file transfer data
			try {
				peer.receiveFile(byteArrWrp);
			} catch (IOException e) {
				e.printStackTrace();
			}
			return;
		}

		MessageWrapper msgwrp = Wrapper.byteArrWrpToMsgWrp(byteArrWrp);
		Message.ChatMessage chatMsg = (Message.ChatMessage) msgwrp.content;

		Command cmd = Command.fromString(chatMsg.getMessage().split("\\|", 2)[0]);

		switch (cmd) {
			case Command.REGISTER_SUCCESS:
				System.out.println(chatMsg);
				try {
					peer.tcpc.clean();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				break;
			case Command.REGISTER_FAIL:
				System.out.println(chatMsg);
				try {
					peer.tcpc.clean();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				break;
			case Command.FILE_DOWN:
				System.out.println("Downloading File!");
				peer.handleFileRequest(msgwrp);
				break;
			case Command.LOOKUP_RES:
				System.out.println("Looking up File!");
				peer.handleFileLookup(msgwrp);
				break;
			case Command.HEARTBEAT_REQ:
				System.out.println("Got Heartbeat Req!");
				peer.handleHeartbeatReq(msgwrp);
				break;
			case Command.INDEF:
				System.out.println(chatMsg);
				break;
			default:
				System.out.println(chatMsg);
				break;
		}
	}

	/**
	 * Handles file requests by checking the requested file's existence and
	 * permissions.
	 * If the file exists and is writable, it initiates the file transfer process.
	 *
	 * @param msgwrp The MessageWrapper containing the file request message.
	 */
	private void handleFileRequest(MessageWrapper msgwrp) {
		Message.ChatMessage chatMsg = (Message.ChatMessage) msgwrp.content;

		// Test a relative path by creating a file object
		String relativeFilePath = chatMsg.getMessage().split("\\|", 2)[1];
		File file;
		try {
			file = sandboxFS.getFile(relativeFilePath);
		} catch (SecurityException se) {
			Message.ChatMessage res = new Message.ChatMessage(chatMsg.getRecipient(), "You can't access this file!");
			MessageWrapper reswrp = new MessageWrapper(msgwrp.connInfo, res);
			this.ms.send(reswrp);
			return;
		}

		printFileInfo(file);
		System.out.println("Does the file exist? " + file.exists());
		if (file.exists()) {
			if (file.canWrite()) {
				System.out.println("Transferring file....");
				sendFile(msgwrp.connInfo, file);
			}
		}
	}

	/**
	 * Sends a file to a specified connection by reading the file in chunks and
	 * sending each chunk over the network. It also sends start and end signals to
	 * indicate the eginning and end of the file transfer.
	 *
	 * @param connInfo The ConnectionInfo specifying the connection details.
	 * @param file     The file to be sent.
	 */
	private void sendFile(ConnectionInfo connInfo, File file) {
		try (
				FileInputStream fis = new FileInputStream(file.getPath());) {
			byte[] buffer = new byte[CHUNK_SIZE];
			int bytesRead;
			int sequenceNumber = 0;

			final int PREFIX_START = 256;
			final int PREFIX_TRANSFER = 512;
			final int PREFIX_END = 2048;

			byte[] startFlag = ByteBuffer.allocate(4).putInt(PREFIX_START).array();
			byte[] fileNameBytes = file.getName().getBytes();
			byte[] startSignal = new byte[4 + fileNameBytes.length];

			int offset = 0;
			System.arraycopy(startFlag, 0, startSignal, 0, startFlag.length);
			offset += startFlag.length;
			System.arraycopy(fileNameBytes, 0, startSignal, offset, fileNameBytes.length);

			this.udpc.send(new ByteArrWrapper(connInfo, startSignal));

			while ((bytesRead = fis.read(buffer)) != -1) {
				byte[] transferFlag = ByteBuffer.allocate(4).putInt(PREFIX_TRANSFER).array();
				byte[] seqNumBytes = ByteBuffer.allocate(4).putInt(sequenceNumber).array();
				byte[] dataToSend = new byte[transferFlag.length + seqNumBytes.length + bytesRead];

				offset = 0;
				System.arraycopy(transferFlag, 0, dataToSend, offset, transferFlag.length);
				offset += transferFlag.length;
				System.arraycopy(seqNumBytes, 0, dataToSend, offset, seqNumBytes.length);
				offset += seqNumBytes.length;
				System.arraycopy(buffer, 0, dataToSend, offset, bytesRead);

				this.udpc.send(new ByteArrWrapper(connInfo, dataToSend));
				sequenceNumber++;
			}

			byte[] endSignal = ByteBuffer.allocate(4).putInt(PREFIX_END).array();
			this.udpc.send(new ByteArrWrapper(connInfo, endSignal));

			System.out.println("File sent successfully.");
		} catch (IOException e) {
			System.err.println("I/O Error: " + e.getMessage());
		} catch (Exception e) {
			System.err.println("Unexpected error: " + e.getMessage());
		}
	}

	/**
	 * Receives a file from a ByteArrWrapper by processing the received data ackets.
	 * It handles start, transfer, and end signals to reconstruct the file and save
	 * it o the sandboxed file system.
	 *
	 * @param byteArrWrp The ByteArrWrapper containing the received file data.
	 * @throws IOException If an I/O error occurs during file reception.
	 */
	private void receiveFile(ByteArrWrapper byteArrWrp) throws IOException {
		switch (byteArrWrp.content[2]) {
			case 1:
				// start flag

				// Extract file name and convert it to String
				byte[] fileNameBytes = Arrays.copyOfRange(byteArrWrp.content, 4, byteArrWrp.content.length);
				String fileName = new String(fileNameBytes, StandardCharsets.UTF_8);

				System.out.printf("Starting file transfer: %s\n", fileName);
				break;
			case 2:
				// transfer flag
				System.out.println("Got file data packet");

				// Extract sent data and convert it to String
				byte[] dataBytes = Arrays.copyOfRange(byteArrWrp.content, 8, byteArrWrp.content.length);

				try (FileOutputStream fos = new FileOutputStream("files/temp", true)) {
					fos.write(dataBytes);
				} catch (IOException e) {
					e.printStackTrace();
				}
				break;
			case 4:
				// retransmit flag
				break;
			case 8:
				// end flag
				System.out.println("Finished transmitting data!");

				File tempFile;
				File file;
				try {
					tempFile = sandboxFS.getFile("temp");
					file = sandboxFS.getFile(this.downloadName);
				} catch (SecurityException se) {
					System.out.println("Unauthorized Access");
					return;
				}

				// Rename file
				if (tempFile.renameTo(file)) {
					System.out.printf("File (%s) downloaded successfully!\n", this.downloadName);
				} else {
					System.out.printf("File (%s) download failed! Data is stored in files/temp.\n", this.downloadName);
				}
				printFileInfo(file);
				break;
			default:
				break;
		}
	}

	/**
	 * Prints information about a file, including its path, name, size and
	 * permissions.
	 *
	 * @param file The file whose information is to be printed.
	 */
	private void printFileInfo(File file) {
		System.out.println("Does the file exist? " + file.exists());
		if (file.exists()) {
			System.out.println("File Specifications:");
			System.out.println("---------------------");
			System.out.println("Absolute Path: " + file.getAbsolutePath());
			System.out.println("Name: " + file.getName());
			System.out.println("Parent Directory: " + file.getParent());
			System.out.println("File Size: " + file.length() + " bytes");

			// Permissions
			System.out.println("Readable: " + file.canRead());
			System.out.println("Writable: " + file.canWrite());
			System.out.println("Executable: " + file.canExecute());
			System.out.println("---------------------");
		}
	}

	/**
	 * Handles file lookup responses by processing the received message and
	 * displaying the lookup results.
	 *
	 * @param msgwrp The MessageWrapper containing the file lookup response message.
	 */
	private void handleFileLookup(MessageWrapper msgwrp) {
		Message.ChatMessage chatMsg = (Message.ChatMessage) msgwrp.content;

		System.out.println(chatMsg);
	}

	/**
	 * Handles heartbeat requests by sending an acknowledgment message back to the
	 * sender.
	 *
	 * @param msgwrp The MessageWrapper containing the heartbeat request message.
	 */
	private void handleHeartbeatReq(MessageWrapper msgwrp) {
		Message.ChatMessage chatMsg = (Message.ChatMessage) msgwrp.content;
		System.out.println(chatMsg);

		Message.ChatMessage ackMsg = new Message.ChatMessage(this.userId,
				Command.HEARTBEAT_ACK + "|" + StringFormatUtils.getTimestamp(Instant.now()));
		MessageWrapper ackWrp = new MessageWrapper(msgwrp.connInfo, ackMsg);
		this.ms.send(ackWrp);
	}
}