package net.kaikk.msm.command.internal;

import net.kaikk.msm.ModularServersManager;
import net.kaikk.msm.command.CommandExecutor;
import net.kaikk.msm.command.Actor;
import net.kaikk.msm.server.Server;

public class ServerListCommand implements CommandExecutor {
	final ModularServersManager instance = ModularServersManager.instance();

	@Override
	public Object process(Actor sender, String command, String[] arguments) {
		sender.sendMessage("Servers list:");
		for (Server server : instance.getServers().values()) {
			sender.sendMessage("- "+server.getId()+": "+server.getName()+" - State: "+server.getState());
		}
		return null;
	}

	@Override
	public String shortDescription(Actor sender, String command, String... arguments) {
		return "Shows the list of servers and their status";
	}
}
