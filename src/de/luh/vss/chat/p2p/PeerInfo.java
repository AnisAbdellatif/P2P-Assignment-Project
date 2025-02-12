package de.luh.vss.chat.p2p;

import java.net.InetAddress;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Represents information about a peer in a peer-to-peer chat system.
 * 
 * <p>This class encapsulates the peer's network address, port, unique identifier,
 * and the last time the peer was seen active.</p>
 * 
 * <p>Instances of this class are immutable except for the lastSeen field, which
 * can be updated to reflect the latest activity time.</p>
 * 
 * <p>The {@code toString} method provides a string representation of the peer
 * information in the format: {@code address|port|id|timestamp}.</p>
 * 
 * <p>The timestamp is formatted as {@code HH:mm:ss} in the system's default time zone.</p>
 * 
 */
public class PeerInfo {
    private final InetAddress address;
    private final int port;
    private final int id;
    private Instant lastSeen;

    public PeerInfo(InetAddress address, int port, int id, Instant lastSeen) {
        this.address = address;
        this.port = port;
        this.id = id;
        this.lastSeen = lastSeen;
    }

    public InetAddress getAddress() {
        return address;
    }

    public int getPort() {
        return port;
    }

    public int getId() {
        return id;
    }

    public Instant getLastSeen() {
        return lastSeen;
    }

    public void setLastSeen(Instant newLastSeen) {
        this.lastSeen = newLastSeen;
    }

    @Override
    public String toString() {
        ZonedDateTime zonedDateTime = this.lastSeen.atZone(ZoneId.systemDefault());
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        String timestamp = zonedDateTime.format(formatter);
        return String.format("%s|%d|%d|%s", address.getHostAddress(), port, id, timestamp);
    }
}