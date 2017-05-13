package net.kaikk.msm.event.server;

import net.kaikk.msm.server.Server;

/**
 * This event is called when a server is removed, usually after the ModularServersManager config is reloaded.
 * 
 * @author Kai
 *
 */
public class ServerRemoveEvent extends ServerEvent {

	public ServerRemoveEvent(Server server) {
		super(server);
	}

}
