package net.kaikk.msm.event.server;

import net.kaikk.msm.server.Server;
import net.kaikk.msm.server.Server.ServerState;

/**
 * This event is called whenever a server is killed.
 * 
 * @author Kai
 *
 */
public class ServerKilledEvent extends ServerEvent implements ServerStateChangeEvent {
	public ServerKilledEvent(Server server) {
		super(server);
	}

	@Override
	public ServerState getServerState() {
		return ServerState.KILLED;
	}
}
