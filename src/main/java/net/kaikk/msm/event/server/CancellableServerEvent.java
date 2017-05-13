package net.kaikk.msm.event.server;

import net.kaikk.msm.event.Cancellable;
import net.kaikk.msm.server.Server;

/**
 * Defines a cancellable server event
 * 
 * @author Kai
 *
 */
public abstract class CancellableServerEvent extends ServerEvent implements Cancellable {
	private boolean cancelled;
	
	public CancellableServerEvent(Server server) {
		super(server);
	}

	public Server getServer() {
		return server;
	}
	
	@Override
	public boolean isCancelled() {
		return cancelled;
	}
	
	@Override
	public void setCancelled(boolean isCancelled) {
		this.cancelled = isCancelled;
	}
}
