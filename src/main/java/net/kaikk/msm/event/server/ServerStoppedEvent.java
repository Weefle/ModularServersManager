package net.kaikk.msm.event.server;

import net.kaikk.msm.server.Server;
import net.kaikk.msm.server.Server.ServerState;

/**
 * This event is called whenever a server has stopped.
 * 
 * @author Kai
 *
 */
public class ServerStoppedEvent extends ServerEvent implements ServerStateChangeEvent {
	public ServerStoppedEvent(Server server) {
		super(server);
	}

	@Override
	public ServerState getServerState() {
		return ServerState.STOPPED;
	}
}
