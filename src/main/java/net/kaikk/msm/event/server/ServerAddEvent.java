package net.kaikk.msm.event.server;

import net.kaikk.msm.server.Server;

/**
 * This event is called when a server is added, usually after the ModularServersManager config is reloaded.
 * 
 * @author Kai
 *
 */
public class ServerAddEvent extends ServerEvent {

	public ServerAddEvent(Server server) {
		super(server);
	}

}
