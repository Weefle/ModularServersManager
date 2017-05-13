package net.kaikk.msm.command.internal;

import java.io.IOException;

import net.kaikk.msm.ModularServersManager;
import net.kaikk.msm.command.CommandExecutor;
import net.kaikk.msm.command.Actor;
import net.kaikk.msm.server.Server;

public class StartAllServersCommand implements CommandExecutor {
	final ModularServersManager instance = ModularServersManager.instance();
	@Override
	public Object process(Actor sender, String command, String[] arguments) throws IllegalStateException, IOException {
		for (final Server server : instance.getServers().values()) {
			if (!server.isAlive()) {
				server.start();
			}
		}
		
		return null;
	}
	
	@Override
	public String shortDescription(Actor sender, String command, String... arguments) {
		return "Starts all servers.";
	}
}
