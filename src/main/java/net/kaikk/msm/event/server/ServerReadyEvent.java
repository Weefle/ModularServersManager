package net.kaikk.msm.event.server;

import net.kaikk.msm.server.Server;
import net.kaikk.msm.server.Server.ServerState;

/**
 * This event is called whenever a server is ready.
 * 
 * @author Kai
 *
 */
public class ServerReadyEvent extends ServerEvent implements ServerStateChangeEvent {
	public ServerReadyEvent(Server server) {
		super(server);
	}

	@Override
	public ServerState getServerState() {
		return ServerState.RUNNING;
	}
}
