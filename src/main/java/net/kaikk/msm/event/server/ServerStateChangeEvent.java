package net.kaikk.msm.event.server;

import net.kaikk.msm.event.Event;
import net.kaikk.msm.server.Server;
import net.kaikk.msm.server.Server.ServerState;

/**
 * Defines a change of the server state
 * 
 * @author Kai
 *
 */
public interface ServerStateChangeEvent extends Event {
	/**
	 * @return the server
	 */
	Server getServer();
	
	/**
	 * @return the new server state
	 */
	ServerState getServerState();
}
