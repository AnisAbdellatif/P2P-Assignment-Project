
/**
 * UDP_Client is a class that extends NetClient to provide UDP socket communication.
 * It allows sending and receiving data using UDP protocol.
 * 
 * <p>It provides methods to send data, receive data, refresh the socket, and clean up resources.</p>
 * 
 * <p>Usage example:</p>
 * <pre>
 * {@code
 * UDP_Client client = new UDP_Client();
 * client.send(new ByteArrWrapper(...));
 * ByteArrWrapper receivedData = client.receive();
 * }
 * </pre>
 * 
 * <p>Constructor Summary:</p>
 * <ul>
 * <li>{@link #UDP_Client(Object, Callback)} - Creates a UDP_Client with a receive callback.</li>
 * <li>{@link #UDP_Client()} - Creates a UDP_Client without a receive callback.</li>
 * </ul>
 * 
 * <p>Method Summary:</p>
 * <ul>
 * <li>{@link #send(ByteArrWrapper)} - Sends data using UDP.</li>
 * <li>{@link #receive()} - Receives data using UDP.</li>
 * <li>{@link #refresh()} - Refreshes the UDP socket.</li>
 * <li>{@link #clean()} - Cleans up the UDP socket resources.</li>
 * <li>{@link #getLocalPort()} - Returns the local port number.</li>
 * </ul>
 * 
 * <p>Fields:</p>
 * <ul>
 * <li>{@code protected DatagramSocket socket} - The DatagramSocket used for communication.</li>
 * <li>{@code protected int localPort} - The local port number.</li>
 * <li>{@code private final int MAX_BUFF_SIZE} - The maximum buffer size for receiving data.</li>
 * <li>{@code private DatagramPacket recvPacket} - The DatagramPacket used for receiving data.</li>
 * </ul>
 * 
 * @see NetClient
 * @see DatagramSocket
 * @see DatagramPacket
 * @see IOException
 * @see SocketException
 */

package de.luh.vss.chat.utils;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.Arrays;

import de.luh.vss.chat.client.Wrapper.*;

public class UDP_Client extends NetClient {

	protected DatagramSocket socket;
	protected int localPort;
	private final int MAX_BUFF_SIZE = 4096;
	private DatagramPacket recvPacket;

	public UDP_Client(Object receiveCallbackObj, Callback receiveCallback) {
		super(NetClient.SocketType.UDP, receiveCallbackObj, receiveCallback);

		try {
			refresh();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public UDP_Client() {
		this(null, null);
	}

	public void send(ByteArrWrapper bufferwrp) {
		try {
			DatagramPacket payload = new DatagramPacket(bufferwrp.content, bufferwrp.content.length,
					bufferwrp.connInfo.remoteAddr, bufferwrp.connInfo.remotePort);
			socket.send(payload);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public ByteArrWrapper receive() {
		ByteArrWrapper bufferwrp = null;
		try {
			socket.receive(recvPacket);
			byte[] buffer = recvPacket.getData();
			// remove tailing 0s
			byte[] trimmedBuffer = Arrays.copyOf(buffer, recvPacket.getLength());
			bufferwrp = new ByteArrWrapper(NetClient.SocketType.UDP, recvPacket.getAddress(), recvPacket.getPort(),
					trimmedBuffer);
		} catch (Exception e) {
			if (e instanceof SocketException) {
			} else
				e.printStackTrace();
		}
		return bufferwrp;
	}

	@Override
	public void refresh() throws IOException {
		clean();
		// Create a socket to listen on port 36666
		this.socket = new DatagramSocket();
		this.localPort = socket.getLocalPort();
		System.out.printf("UDP Receiver is ready on port: %d\n", this.localPort);

		System.out.flush();

		// Buffer to hold incoming data
		byte[] buffer = new byte[MAX_BUFF_SIZE];

		// Create a packet to receive the data
		recvPacket = new DatagramPacket(buffer, buffer.length);

	}

	@Override
	public void clean() throws IOException {
		if (socket != null && !socket.isClosed())
			socket.close();
	}

	public int getLocalPort() {
		return this.localPort;
	}
}