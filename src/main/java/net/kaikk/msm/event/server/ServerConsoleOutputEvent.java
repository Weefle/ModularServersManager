package net.kaikk.msm.event.server;

import net.kaikk.msm.server.Server;
import net.kaikk.msm.server.ConsoleLine;

/**
 * This event is called whenever something has been read from the server console
 * 
 * @author Kai
 *
 */
public class ServerConsoleOutputEvent extends ServerEvent {
	private final ConsoleLine line;
	private final boolean error;

	public ServerConsoleOutputEvent(Server server, ConsoleLine line) {
		super(server);
		this.line = line;
		this.error = false;
	}
	
	public ServerConsoleOutputEvent(Server server, ConsoleLine line, boolean error) {
		super(server);
		this.line = line;
		this.error = error;
	}

	/**
	 * @return the line from the server console
	 */
	public ConsoleLine getLine() {
		return line;
	}

	/**
	 * @return whether this line was sent to the System.err, usually denoting an error.
	 */
	public boolean isError() {
		return error;
	}
}
