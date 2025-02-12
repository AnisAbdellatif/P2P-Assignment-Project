/**
 * TCP_Client is a class that handles TCP client-side communication.
 * It extends the NetClient class and provides methods to send and receive data over a TCP connection.
 * 
 * <p>It supports three constructors:
 * <ul>
 *   <li>TCP_Client(InetAddress remoteAddr, int remotePort, Object receiveCallbackObj, Callback receiveCallback)</li>
 *   <li>TCP_Client(String remoteAddrString, int remotePort, Object receiveCallbackObj, Callback receiveCallback)</li>
 *   <li>TCP_Client(Socket socket, Object receiveCallbackObj, Callback receiveCallback)</li>
 * </ul>
 * 
 * <p>Methods:
 * <ul>
 *   <li>{@link #send(ByteArrWrapper)}: Sends data to the remote server.</li>
 *   <li>{@link #receive()}: Receives data from the remote server.</li>
 *   <li>{@link #refresh()}: Refreshes the connection to the remote server.</li>
 *   <li>{@link #getSocket()}: Returns the current socket.</li>
 *   <li>{@link #printInfo()}: Prints information about the local and remote addresses.</li>
 *   <li>{@link #clean()}: Cleans up resources by closing the socket and input stream.</li>
 * </ul>
 * 
 * <p>Fields:
 * <ul>
 *   <li>{@code remoteAddr}: The remote server's address.</li>
 *   <li>{@code remotePort}: The remote server's port.</li>
 *   <li>{@code socket}: The socket used for communication.</li>
 *   <li>{@code inputStream}: The input stream for reading data from the socket.</li>
 * </ul>
 * 
 * <p>Usage example:
 * <pre>
 * {@code
 * TCP_Client client = new TCP_Client("127.0.0.1", 8080, receiveCallbackObj, receiveCallback);
 * client.send(new ByteArrWrapper(...));
 * ByteArrWrapper response = client.receive();
 * }
 * </pre>
 * 
 * @see NetClient
 * @see ByteArrWrapper
 */

package de.luh.vss.chat.utils;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Arrays;
import de.luh.vss.chat.client.Wrapper.*;

public class TCP_Client extends NetClient {
    public final InetAddress remoteAddr;
    public final int remotePort;
    private Socket socket;
    private DataInputStream inputStream;

    public TCP_Client(InetAddress remoteAddr, int remotePort, Object receiveCallbackObj, Callback receiveCallback)
            throws IOException {
        super(NetClient.SocketType.TCP, receiveCallbackObj, receiveCallback);
        this.remoteAddr = remoteAddr;
        this.remotePort = remotePort;
        refresh();
    }

    public TCP_Client(String remoteAddrString, int remotePort, Object receiveCallbackObj, Callback receiveCallback)
            throws IOException {
        this(InetAddress.getByName(remoteAddrString), remotePort, receiveCallbackObj, receiveCallback);
    }

    public TCP_Client(Socket socket, Object receiveCallbackObj, Callback receiveCallback) throws IOException {
        super(NetClient.SocketType.TCP, receiveCallbackObj, receiveCallback);
        this.remoteAddr = socket.getInetAddress();
        this.remotePort = socket.getPort();
        this.socket = socket;
        this.inputStream = new DataInputStream(socket.getInputStream());
        printInfo();
    }

    public void send(ByteArrWrapper bufferwrp) throws IOException {
        if (socket == null || socket.isClosed()) {
            this.refresh();
            this.resumeThread();
        }
        OutputStream out = socket.getOutputStream();
        out.write(bufferwrp.content, 0, bufferwrp.content.length);
        out.flush();
    }

    protected ByteArrWrapper receive() throws IOException {
        if (socket == null || socket.isClosed()) {
            this.clean();
            System.out.println("TCP-Socket closed.");
            this.pauseThread();
            return null;
        }

        ByteArrWrapper bufferwrp = null;

        try {
            byte[] buffer = new byte[1024]; // Fixed buffer size
            int bytesRead = inputStream.read(buffer);

            if (bytesRead == -1) {
                // Connection closed by the server
                System.out.println("Socket to PMS closed.");
                this.clean();
                this.pauseThread();
                return null;
            }

            bufferwrp = new ByteArrWrapper(NetClient.SocketType.TCP, socket.getInetAddress(), socket.getPort(),
                    Arrays.copyOf(buffer, bytesRead));

        } catch (IOException e) {
            e.printStackTrace();
        }
        return bufferwrp;
    }

    @Override
    public void refresh() throws IOException {
        if (socket != null && !socket.isClosed()) {
            System.out.println("Socket already active, no need to refresh.");
            return;
        }

        clean(); // Ensure old resources are released
        System.out.printf("Connecting to server... %s:%d\n", remoteAddr, remotePort);
        socket = new Socket(remoteAddr, remotePort);
        inputStream = new DataInputStream(socket.getInputStream());
        printInfo();
    }

    public Socket getSocket() {
        return this.socket;
    }

    public void printInfo() {
        System.out.printf("Local : %s:%d\n", socket.getLocalAddress(), socket.getLocalPort());
        System.out.printf("Remote: %s:%d\n", socket.getInetAddress(), socket.getPort());
    }

    @Override
    public void clean() throws IOException {
        if (socket != null && !socket.isClosed()) {
            socket.close();
            socket = null;
        }
        if (inputStream != null) {
            inputStream.close();
            inputStream = null;
        }
    }
}
