package net.kaikk.msm.command.internal;

import java.io.IOException;

import net.kaikk.msm.ModularServersManager;
import net.kaikk.msm.command.CommandExecutor;
import net.kaikk.msm.command.Actor;
import net.kaikk.msm.server.Server;

public class KillServerCommand implements CommandExecutor {
	final ModularServersManager instance = ModularServersManager.instance();
	@Override
	public Object process(Actor sender, String command, String[] arguments) throws IllegalStateException, IOException {
		if (arguments.length == 0) {
			sender.sendMessage("Usage: !"+command+" (server id)");
			return null;
		}
		final Server server = instance.getServer(arguments[0]);
		if (server == null) {
			sender.sendMessage("Server id "+arguments[0]+" does not exist");
			return null;
		}
		
		if (!server.isAlive()) {
			sender.sendMessage("Server id "+arguments[0]+" is not running");
			return null;
		}
		
		server.kill();
		return null;
	}
	
	@Override
	public String shortDescription(Actor sender, String command, String... arguments) {
		return "Kills the specified server";
	}
}
