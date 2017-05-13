package net.kaikk.msm.event.server;

import net.kaikk.msm.command.Actor;
import net.kaikk.msm.event.Cancellable;
import net.kaikk.msm.server.Server;

/**
 * This event is called whenever something has been written to the server console
 * 
 * @author Kai
 *
 */
public class ServerConsoleInputEvent extends ServerEvent implements Cancellable {
	private String line;
	private final Actor sender;
	private boolean cancelled;

	public ServerConsoleInputEvent(Server server, Actor sender, String line) {
		super(server);
		this.sender = sender;
		this.line = line;
	}

	/**
	 * @return the line that was sent
	 */
	public String getLine() {
		return line;
	}

	/**
	 * @param line set the line that was sent to a new one
	 */
	public void setLine(String line) {
		this.line = line;
	}

	/**
	 * @return the sender
	 */
	public Actor getSender() {
		return sender;
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
