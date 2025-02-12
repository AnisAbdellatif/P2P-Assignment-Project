package de.luh.vss.chat.p2p;

import java.util.HashMap;
import java.util.Map;

/**
 * Enum representing various commands used in the P2P chat application.
 * Each command has a corresponding string description.
 * <pre>
 * Commands:
 * - REGISTER_REQ: Request to register
 * - REGISTER_SUCCESS: Registration successful
 * - REGISTER_FAIL: Registration failed
 * - DEREGISTER: Request to deregister
 * - DEREGISTER_SUCCESS: Deregistration successful
 * - DEREGISTER_FAIL: Deregistration failed
 * - HEARTBEAT_REQ: Heartbeat request
 * - HEARTBEAT_ACK: Heartbeat acknowledgment
 * - HEARTBEAT_TO: Heartbeat timeout
 * - LIST_REQ: Request to list
 * - LIST_RES: List response
 * - LOOKUP_REQ: Lookup request
 * - LOOKUP_RES: Lookup response
 * - FILE_DOWN: File download
 * - FILE_UP: File upload
 * - INDEF: Indefinite command
 * 
 * Methods:
 * - toString(): Returns the string description of the command.
 * - fromString(String val): Returns the Command corresponding to the given string description.
 *   If the description does not match any command, returns INDEF.
 * </pre>
 */
public enum Command {
	REGISTER_REQ("REGISTER_REQ"),
	REGISTER_SUCCESS("REGISTER_SUCCESS"),
	REGISTER_FAIL("REGISTER_FAIL"),
	DEREGISTER("DEREGISTER_REQ"),
	DEREGISTER_SUCCESS("REGISTER_SUCCESS"),
	DEREGISTER_FAIL("REGISTER_FAIL"),
	HEARTBEAT_REQ("HEARTBEAT_REQ"),
	HEARTBEAT_ACK("HEARTBEAT_ACK"),
	HEARTBEAT_TO("HEARTBEAT_TO"),
	LIST_REQ("LIST_REQ"),
	LIST_RES("LIST_RES"),
	LOOKUP_REQ("LOOKUP_REQ"),
	LOOKUP_RES("LOOKUP_RES"),
	FILE_DOWN("FILE_DOWN"),
	FILE_UP("FILE_UP"),
	INDEF("INDEF");

	private final String desc;

	// Constructor to initialize the description
	Command(String desc) {
		this.desc = desc;
	}

	@Override
	public String toString() {
		return desc;
	}

	private static final Map<String, Command> lookup = new HashMap<String, Command>();
	static {
		for (final Command cmd : Command.values()) {
			lookup.put(cmd.desc, cmd);
		}
	}

	public static Command fromString(final String val) {
		Command cmd = lookup.get(val);
		if (cmd == null)
			return Command.INDEF;
		else
			return cmd;
	}
}