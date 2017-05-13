package net.kaikk.msm.command.internal;

import java.io.IOException;

import net.kaikk.msm.ModularServersManager;
import net.kaikk.msm.command.CommandExecutor;
import net.kaikk.msm.command.Actor;
import net.kaikk.msm.server.Server;

public class RestartAllServersCommand implements CommandExecutor {
	final ModularServersManager instance = ModularServersManager.instance();
	@Override
	public Object process(Actor sender, String command, String[] arguments) throws IllegalStateException, IOException {
		for (final Server server : instance.getServers().values()) {
			server.restart();
		}
		
		return null;
	}
	
	@Override
	public String shortDescription(Actor sender, String command, String... arguments) {
		return "Restarts all servers. Servers that are not running will be started.";
	}
}
