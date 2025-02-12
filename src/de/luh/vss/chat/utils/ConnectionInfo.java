/**
 * Represents the connection information for a network client.
 * This includes the socket type, remote address, and remote port.
 */
package de.luh.vss.chat.utils;

import java.net.InetAddress;

public class ConnectionInfo {
	public NetClient.SocketType sType;
	public InetAddress remoteAddr;
	public int remotePort;

	public ConnectionInfo(NetClient.SocketType sType, InetAddress remoteAddr, int remotePort) {
		this.sType = sType;
		this.remoteAddr = remoteAddr;
		this.remotePort = remotePort;
	}
}