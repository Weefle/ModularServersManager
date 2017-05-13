package net.kaikk.msm.event.server;

import net.kaikk.msm.event.Event;
import net.kaikk.msm.server.Server;

/**
 * Defines a server event
 * 
 * @author Kai
 *
 */
public abstract class ServerEvent implements Event {
	protected final Server server;

	public ServerEvent(Server server) {
		this.server = server;
	}

	/**
	 * @return the server involved in this event
	 */
	public Server getServer() {
		return server;
	}
}
