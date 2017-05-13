package net.kaikk.msm.command.internal;

import java.io.IOException;

import net.kaikk.msm.ModularServersManager;
import net.kaikk.msm.command.CommandExecutor;
import net.kaikk.msm.command.Actor;
import net.kaikk.msm.server.Server;

public class RestartServerCommand implements CommandExecutor {
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
		
		server.restart();
		return null;
	}

	@Override
	public String shortDescription(Actor sender, String command, String... arguments) {
		return "Restarts the specified server. If the server is not running, it will be started.";
	}
}
