/**
 * The Settings class holds configuration constants for the chat client.
 * These settings include server address, server port, server ID, PIN, 
 * chunk size, root directory for files, and polling rate.
 * 
 * <p>Configuration details:
 * <ul>
 *   <li>{@code PMS_serverAddress} - The IP address of the PMS server.</li>
 *   <li>{@code PMS_serverPort} - The port number of the PMS server.</li>
 *   <li>{@code PMS_serverID} - The ID of the PMS server.</li>
 *   <li>{@code PIN} - The PIN used for authentication.</li>
 *   <li>{@code CHUNK_SIZE} - The size of data chunks in bytes.</li>
 *   <li>{@code sfs_root} - The root directory for storing files.</li>
 *   <li>{@code POLLING_RATE_MS} - The polling rate in milliseconds.</li>
 * </ul>
 * </p>
 */

package de.luh.vss.chat.client;

public class Settings {
	// public static final String PMS_serverAddress = "130.75.202.197";
	public static final String PMS_serverAddress = "127.0.0.1";
	public static final int PMS_serverPort = 4444;
	public static final int PMS_serverID = 0;
	public static final int PIN = 5621;
	public static final int CHUNK_SIZE = 1024;
	public static final String sfs_root = "files";
	public static final int POLLING_RATE_MS = 500;
}