package de.luh.vss.chat.client;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import de.luh.vss.chat.common.Message;
import de.luh.vss.chat.utils.ConnectionInfo;
import de.luh.vss.chat.utils.NetClient;

/**
 * The Wrapper class is an abstract class that encapsulates a connection
 * information and content of type T.
 * It provides nested static classes for wrapping Message and byte[] content
 * types.
 *
 * @param <T> the type of the content to be wrapped
 */
public abstract class Wrapper<T> {
	public ConnectionInfo connInfo;
	public T content;

	/**
	 * Constructs a Wrapper with the specified socket type, remote address, remote
	 * port, and content.
	 *
	 * @param sType      the socket type
	 * @param remoteAddr the remote address
	 * @param remotePort the remote port
	 * @param content    the content to be wrapped
	 */
	public Wrapper(NetClient.SocketType sType, InetAddress remoteAddr, int remotePort, T content) {
		this.connInfo = new ConnectionInfo(sType, remoteAddr, remotePort);
		this.content = content;
	}

	/**
	 * The MessageWrapper class is a nested static class that wraps a Message
	 * object.
	 */
	public static class MessageWrapper extends Wrapper<Message> {
		/**
		 * Constructs a MessageWrapper with the specified socket type, remote address,
		 * remote port, and message content.
		 *
		 * @param sType      the socket type
		 * @param remoteAddr the remote address
		 * @param remotePort the remote port
		 * @param content    the message content to be wrapped
		 */
		public MessageWrapper(NetClient.SocketType sType, InetAddress remoteAddr, int remotePort, Message content) {
			super(sType, remoteAddr, remotePort, content);
		}

		/**
		 * Constructs a MessageWrapper with the specified connection information and
		 * message content.
		 *
		 * @param connInfo the connection information
		 * @param content  the message content to be wrapped
		 */
		public MessageWrapper(ConnectionInfo connInfo, Message content) {
			super(connInfo.sType, connInfo.remoteAddr, connInfo.remotePort, content);
		}

		/**
		 * Returns a string representation of the MessageWrapper.
		 *
		 * @return a string representation of the MessageWrapper
		 */
		@Override
		public String toString() {
			String res = String.format("MessageWrapper over %s | %s:%d\nContent: %s", this.connInfo.sType,
					this.connInfo.remoteAddr, this.connInfo.remotePort, this.content);
			return res;
		}
	}

	/**
	 * The ByteArrWrapper class is a nested static class that wraps a byte array.
	 */
	public static class ByteArrWrapper extends Wrapper<byte[]> {
		/**
		 * Constructs a ByteArrWrapper with the specified socket type, remote address,
		 * remote port, and byte array content.
		 *
		 * @param sType      the socket type
		 * @param remoteAddr the remote address
		 * @param remotePort the remote port
		 * @param content    the byte array content to be wrapped
		 */
		public ByteArrWrapper(NetClient.SocketType sType, InetAddress remoteAddr, int remotePort, byte[] content) {
			super(sType, remoteAddr, remotePort, content);
		}

		/**
		 * Constructs a ByteArrWrapper with the specified connection information and
		 * byte array content.
		 *
		 * @param connInfo the connection information
		 * @param content  the byte array content to be wrapped
		 */
		public ByteArrWrapper(ConnectionInfo connInfo, byte[] content) {
			super(connInfo.sType, connInfo.remoteAddr, connInfo.remotePort, content);
		}

		/**
		 * Returns a string representation of the ByteArrWrapper.
		 *
		 * @return a string representation of the ByteArrWrapper
		 */
		@Override
		public String toString() {
			String byteArrStr;
			String res = "";
			byteArrStr = new String(this.content, StandardCharsets.UTF_8);
			byteArrStr = Arrays.toString(this.content);
			res = String.format("ByteArrWrp over %s | %s:%d\nContent: %s", this.connInfo.sType,
					this.connInfo.remoteAddr, this.connInfo.remotePort, byteArrStr);
			return res;
		}
	}

	/**
	 * Converts a ByteArrWrapper to a MessageWrapper.
	 *
	 * @param bufferwrp the ByteArrWrapper to be converted
	 * @return the resulting MessageWrapper, or null if the conversion fails
	 */
	public static MessageWrapper byteArrWrpToMsgWrp(ByteArrWrapper bufferwrp) {
		BufferedInputStream bufferedIn = new BufferedInputStream(new ByteArrayInputStream(bufferwrp.content));
		DataInputStream inputStream = new DataInputStream(bufferedIn);

		Message message = null;
		try {
			message = Message.parse(inputStream);
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("Received out of bounds message!");
			try {
				bufferedIn.reset();
				System.out.println(inputStream.readUTF());
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		} finally {
			if (message == null)
				return null;
		}

		MessageWrapper msgwrp = new MessageWrapper(bufferwrp.connInfo.sType, bufferwrp.connInfo.remoteAddr,
				bufferwrp.connInfo.remotePort, message);
		return msgwrp;
	}

	/**
	 * Converts a MessageWrapper to a ByteArrWrapper.
	 *
	 * @param msgwrp the MessageWrapper to be converted
	 * @return the resulting ByteArrWrapper
	 */
	public static ByteArrWrapper msgWrpToByteArrWrp(MessageWrapper msgwrp) {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		DataOutputStream dos = new DataOutputStream(baos);
		try {
			msgwrp.content.toStream(dos);
			dos.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
		byte[] buffer = baos.toByteArray();
		baos.reset();
		ByteArrWrapper bufferwrp = new ByteArrWrapper(msgwrp.connInfo.sType, msgwrp.connInfo.remoteAddr,
				msgwrp.connInfo.remotePort, buffer);
		return bufferwrp;
	}
}
