package de.luh.vss.chat.p2p;

import java.io.IOException;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import de.luh.vss.chat.client.Settings;
import de.luh.vss.chat.client.Wrapper;
import de.luh.vss.chat.client.Wrapper.ByteArrWrapper;
import de.luh.vss.chat.client.Wrapper.MessageWrapper;
import de.luh.vss.chat.common.Message;
import de.luh.vss.chat.common.User;
import de.luh.vss.chat.utils.ConnectionInfo;
import de.luh.vss.chat.utils.NetClient;
import de.luh.vss.chat.utils.StringFormatUtils;
import de.luh.vss.chat.utils.UDP_Client;
import de.luh.vss.chat.utils.Worker;


/**
 * The PeersManager class is responsible for managing peer connections and file
 * registrations in a peer-to-peer chat system. It extends the Worker class and
 * provides methods for adding, removing, and retrieving peer information. It
 * also handles sending and receiving heartbeat messages to keep track of active
 * peers.
 */
public class PeersManager extends Worker {
    
    /**
     * A concurrent hash map storing registered peers with their addresses.
     */
    public static class PeersRegMap extends ConcurrentHashMap<Integer, PeerInfo> {
    };

    /**
     * A concurrent hash map storing file registrations associated with peers.
     */
    public static class FilesRegsMap extends ConcurrentHashMap<String, Set<PeerInfo>> {
    };

    private static final PeersRegMap peersRegistry = new PeersRegMap();
    private static FilesRegsMap filesRegistry = new FilesRegsMap();
    private static final ExecutorService executor = Executors.newFixedThreadPool(10);

    /**
     * Constructs a new PeersManager instance.
     */
    public PeersManager() {
        super("PeersManager");
    }

    /**
     * Retrieves the peers registry.
     * 
     * @return the registry of peers
     */
    public synchronized PeersRegMap getPeersRegistry() {
        return this.peersRegistry;
    }

    /**
     * Retrieves the files registry.
     * 
     * @return the registry of files
     */
    public synchronized FilesRegsMap getFilesRegistry() {
        return this.filesRegistry;
    }

    /**
     * Retrieves the set of peers that have a specific file.
     * 
     * @param fileName the name of the file
     * @return a set of peers that have the file
     */
    public synchronized Set<PeerInfo> getFilePeers(String fileName) {
        return this.filesRegistry.get(fileName);
    }

    /**
     * Adds a peer and registers the files they have.
     * 
     * @param peerInfo the peer information
     * @param fileList the list of files the peer has
     */
    public void addPeer(PeerInfo peerInfo, String[] fileList) {
        synchronized (peersRegistry) {
            peersRegistry.put(peerInfo.getId(), peerInfo);
        }
        synchronized (filesRegistry) {
            for (String fileName : fileList) {
                filesRegistry.putIfAbsent(fileName, new HashSet<>());
                Set<PeerInfo> peersSet = filesRegistry.get(fileName);
                peersSet.add(peerInfo);
            }
        }
    }

    /**
     * Removes a peer from the registry.
     * 
     * @param peer the peer to remove
     */
    private synchronized void removePeer(PeerInfo peer) {
        peersRegistry.remove(peer.getId());
        filesRegistry.forEach(0, (fileName, peerSet) -> {
            peerSet.remove(peer);
            filesRegistry.put(fileName, peerSet);
        });
    }

    /**
     * Callback method for receiving messages from peers.
     */
    private static void receiveCallback(NetClient client, Object receiveCallbackObj, ByteArrWrapper byteArrWrp) {
        PeersManager peersManager = (PeersManager) receiveCallbackObj;
        MessageWrapper msgwrp = Wrapper.byteArrWrpToMsgWrp(byteArrWrp);
        Message.ChatMessage chatMsg = (Message.ChatMessage) msgwrp.content;

        Command cmd = Command.fromString(chatMsg.getMessage().split("\\|", 2)[0]);

        switch (cmd) {
            case Command.HEARTBEAT_ACK:
                peersManager.handleHeartbeatACK(msgwrp);
                break;
            default:
                break;
        }
        ((UDP_Client) client).close();
    }

    /**
     * Handles heartbeat acknowledgment messages from peers.
     * 
     * @param msgwrp the received message wrapper
     */
    private synchronized void handleHeartbeatACK(MessageWrapper msgwrp) {
        Message.ChatMessage chatMsg = (Message.ChatMessage) msgwrp.content;
        PeerInfo peer = peersRegistry.get(chatMsg.getRecipient().id());
        peer.setLastSeen(Instant.now());
        peersRegistry.put(peer.getId(), peer);
    }

    /**
     * Sends heartbeat messages to peers to check their availability.
     */
    private synchronized void sendHeartbeats() {
        peersRegistry.forEachValue(Long.MAX_VALUE, (peer) -> {
            UDP_Client udpc = new UDP_Client(this, PeersManager::receiveCallback);
            executor.submit(udpc);

            Instant lastSeen = Instant.now();
            ConnectionInfo connInfo = new ConnectionInfo(NetClient.SocketType.UDP, peer.getAddress(), peer.getPort());
            Message.ChatMessage heartbeatMsg = new Message.ChatMessage(new User.UserId(Settings.PMS_serverID),
                    Command.HEARTBEAT_REQ + "|" + StringFormatUtils.getTimestamp(lastSeen));
            MessageWrapper msgwrp = new MessageWrapper(connInfo, heartbeatMsg);

            System.out.println(msgwrp);
            udpc.send(Wrapper.msgWrpToByteArrWrp(msgwrp));
        });
    }

    /**
     * Removes offline peers that have not responded in a set timeframe.
     */
    private synchronized void timeoutOfflinePeers() {
        peersRegistry.forEachValue(0, (peer) -> {
            if (Instant.now().isAfter(peer.getLastSeen().plusSeconds(30))) {
                removePeer(peer);
                UDP_Client udpc = new UDP_Client();
                ConnectionInfo connInfo = new ConnectionInfo(NetClient.SocketType.UDP, peer.getAddress(),
                        peer.getPort());
                Message.ChatMessage heartbeatMsg = new Message.ChatMessage(new User.UserId(Settings.PMS_serverID),
                        Command.HEARTBEAT_TO + "|" + StringFormatUtils.getTimestamp(peer.getLastSeen()));
                MessageWrapper msgwrp = new MessageWrapper(connInfo, heartbeatMsg);

                System.out.println(msgwrp);
                udpc.send(Wrapper.msgWrpToByteArrWrp(msgwrp));
                udpc.close();
            }
        });
    }

    @Override
    public int task() throws InterruptedException {
        Thread.sleep(1000 * 10);
        this.sendHeartbeats();
        try {
            executor.awaitTermination(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            executor.shutdownNow();
        }
        this.timeoutOfflinePeers();
        return 0;
    }

    @Override
    protected void refresh() throws IOException {
        // TODO Auto-generated method stub
    }

    @Override
    protected void clean() throws IOException {
        // TODO Auto-generated method stub
    }
}
