package net.kaikk.msm.event.server;

import net.kaikk.msm.server.Server;
import net.kaikk.msm.server.Server.ServerState;

/**
 * This event is called whenever a server is being stopped.
 * 
 * @author Kai
 *
 */
public class ServerStopEvent extends CancellableServerEvent implements ServerStateChangeEvent {
	public ServerStopEvent(Server server) {
		super(server);
	}

	@Override
	public ServerState getServerState() {
		return ServerState.STOPPING;
	}
}
