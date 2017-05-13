package net.kaikk.msm;

import java.util.List;

import org.apache.log4j.Logger;

import net.kaikk.msm.command.Actor;
import net.kaikk.msm.server.Server;

/**
 * This defines any action performed by the Console 
 * 
 * @author Kai
 *
 */
public class ConsoleActor implements Actor {
	private final Logger logger;
	private Server attachedServer;
	
	public ConsoleActor() {
		logger = Logger.getLogger("MSM");
	}

	@Override
	public String getName() {
		return "Console";
	}

	/**
	 * The message will be sent to the MSM logger.
	 */
	@Override
	public void sendMessage(String... message) {
		logger.info(String.join("\n", message));
	}

	/**
	 * The message will be sent to the MSM logger.
	 */
	@Override
	public void sendMessage(List<String> message) {
		logger.info(String.join("\n", message));
	}

	/**
	 * The message will be sent to the System.out.
	 */
	@Override
	public void sendRawMessage(String... message) {
		System.out.println(String.join("\n", message));
	}

	/**
	 * The message will be sent to the System.out.
	 */
	@Override
	public void sendRawMessage(List<String> message) {
		System.out.println(String.join("\n", message));
	}
	
	@Override
	public Server getAttachedServer() {
		return attachedServer;
	}
	
	@Override
	public void setAttachedServer(Server attachedServer) {
		if (this.attachedServer != null) {
			this.attachedServer.getAttachedActors().remove(this);
		}
		this.attachedServer = attachedServer;
		if (attachedServer != null) {
			this.attachedServer.getAttachedActors().add(this);
		}
	}
}
