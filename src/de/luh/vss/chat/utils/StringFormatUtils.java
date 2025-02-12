
package de.luh.vss.chat.utils;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Set;

import de.luh.vss.chat.p2p.PeerInfo;
import de.luh.vss.chat.p2p.PeersManager.FilesRegsMap;
import de.luh.vss.chat.p2p.PeersManager.PeersRegMap;

/**
 * Utility class for formatting strings related to chat application.
 */
public abstract class StringFormatUtils {
	@FunctionalInterface
	protected interface Callback {
		void execute();
	}

	/**
	 * Parses a string containing peer information and returns a PeerInfo object.
	 *
	 * @param info the string containing peer information in the format
	 *             "address:port"
	 * @param id   the ID of the peer
	 * @return a PeerInfo object or null if the address is unknown
	 */
	public static PeerInfo parsePeerInfo(String info, int id) {
		try {
			InetAddress address = InetAddress.getByName(info.split("\\:")[0]);
			int port = Integer.parseInt(info.split("\\:")[1]);
			return new PeerInfo(address, port, id, Instant.now());
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Converts a PeersRegMap to a string representation.
	 *
	 * @param peersRegistry the PeersRegMap to convert
	 * @return a string representation of the PeersRegMap
	 */
	public static String PRM_ToString(PeersRegMap peersRegistry) {
		StringBuilder res = new StringBuilder("");

		peersRegistry.forEach((id, peer) -> {
			res.append(String.format("# [%d] #: %s:%d\n", id, peer.getAddress().getHostAddress(), peer.getPort()));
		});

		return res.toString();
	}

	/**
	 * Converts a FilesRegsMap to a string representation.
	 *
	 * @param filesRegistry the FilesRegsMap to convert
	 * @return a string representation of the FilesRegsMap
	 */
	public static String FRM_ToString(FilesRegsMap filesRegistry) {
		StringBuilder res = new StringBuilder("");

		filesRegistry.forEach((fileName, peerSet) -> {
			res.append(String.format("[%s] -> # %s #\n", fileName, peerSet_ToString(peerSet)));

		});

		return res.toString();
	}

	/**
	 * Converts a set of PeerInfo objects to a string representation.
	 *
	 * @param peerSet the set of PeerInfo objects to convert
	 * @return a string representation of the set of PeerInfo objects
	 */
	public static String peerSet_ToString(Set<PeerInfo> peerSet) {
		StringBuilder res = new StringBuilder("[");
		peerSet.forEach((peer) -> {
			res.append(String.format("%d, ", peer.getId()));
		});

		res.delete(res.length() - 2, res.length()); // delete last ', '
		res.append("]");

		return res.toString();
	}

	/**
	 * Returns a formatted timestamp for a given Instant.
	 *
	 * @param instant the Instant to format
	 * @return a string representation of the timestamp in the format "HH:mm:ss"
	 */
	public static String getTimestamp(Instant instant) {
		ZonedDateTime zonedDateTime = instant.atZone(ZoneId.systemDefault());
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
		String timestamp = zonedDateTime.format(formatter);

		return timestamp;
	}
}