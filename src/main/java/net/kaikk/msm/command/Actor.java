package net.kaikk.msm.command;

import java.util.List;

import net.kaikk.msm.server.Server;

/**
 * Classes implementing this interface represent anything that can perform an action, like running a command.  
 * 
 * @author Kai
 *
 */
public interface Actor {	
	String getName();
	void sendMessage(String... message);
	void sendMessage(List<String> message);
	void sendRawMessage(String... message);
	void sendRawMessage(List<String> message);
	default boolean hasPermission(String permission) {
		return true;
	}
	default Server getAttachedServer() {
		return null;
	}
	default void setAttachedServer(Server attachedServer) {
		
	}
}
