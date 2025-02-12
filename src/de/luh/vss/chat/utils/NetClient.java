/**
 * The NetClient class is an abstract class that extends the Worker class and provides
 * a framework for network clients using either TCP or UDP socket types.
 * 
 * <p>This class includes an enumeration for socket types, a nested class for a 
 * LinkedBlockingQueue of ByteArrWrapper objects, and a functional interface for 
 * callback execution. It also provides methods for setting receive callbacks, 
 * handling received data, and abstract methods for sending, receiving, refreshing, 
 * and cleaning up network resources.</p>
 * 
 * <p>Subclasses must implement the abstract methods to provide specific functionality 
 * for sending and receiving data over the network.</p>
 * 
 * @see Worker
 */
package de.luh.vss.chat.utils;

import java.io.IOException;
import java.util.concurrent.LinkedBlockingQueue;

import de.luh.vss.chat.client.Wrapper.*;

public abstract class NetClient extends Worker {

	public static enum SocketType {
		TCP("TCP"),
		UDP("UDP");

		private final String desc;

		// Constructor to initialize the description
		SocketType(String desc) {
			this.desc = desc;
		}

		@Override
		public String toString() {
			return desc;
		}
	}

	public static class LBQ_BAWRP extends LinkedBlockingQueue<ByteArrWrapper> {
	};

	@FunctionalInterface
	public interface Callback {
		void execute(NetClient client, Object receiveCallbackObj, ByteArrWrapper bufferwrp);
	}

	protected Callback receiveCallback = null;
	protected Object receiveCallbackObj = null;

	public NetClient(SocketType sType) {
		super(sType.toString() + "_Client");
	}

	public NetClient(SocketType sType, Object receiveCallbackObj, Callback receiveCallback) {
		this(sType);
		setRecvCallback(receiveCallbackObj, receiveCallback);
	}

	public void setRecvCallback(Object receiveCallbackObj, Callback receiveCallback) {
		this.receiveCallbackObj = receiveCallbackObj;
		this.receiveCallback = receiveCallback;
	}

	@Override
	public int task() {
		// Wait for data
		ByteArrWrapper bufferwrp = null;
		try {
			bufferwrp = receive();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if (bufferwrp == null || bufferwrp.content.length == 0)
			return 0;

		receivedDataHandler(bufferwrp);

		return 0;
	}

	private void receivedDataHandler(ByteArrWrapper bytearrwrp) {
		// System.out.printf("Received data over %s from %s:%d!\n",
		// bytearrwrp.connInfo.sType,
		// bytearrwrp.connInfo.remoteAddr.getCanonicalHostName(),
		// bytearrwrp.connInfo.remotePort);
		// System.out.println(bytearrwrp.toString());
		if (receiveCallback != null) {
			receiveCallback.execute(this, receiveCallbackObj, bytearrwrp);
		}
	}

	abstract public void send(ByteArrWrapper bufferwrp) throws IOException;

	abstract protected ByteArrWrapper receive() throws IOException;

	abstract public void refresh() throws IOException;

	abstract public void clean() throws IOException;
}