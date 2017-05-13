package net.kaikk.msm.event.server;

import net.kaikk.msm.server.Server;
import net.kaikk.msm.server.Server.ServerState;

/**
 * This event is called whenever a server is started.
 * 
 * @author Kai
 *
 */
public class ServerStartEvent extends CancellableServerEvent implements ServerStateChangeEvent {
	public ServerStartEvent(Server server) {
		super(server);
	}

	@Override
	public ServerState getServerState() {
		return ServerState.STARTING;
	}
}
