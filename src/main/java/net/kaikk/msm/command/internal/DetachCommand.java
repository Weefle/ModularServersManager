package net.kaikk.msm.command.internal;

import net.kaikk.msm.ModularServersManager;
import net.kaikk.msm.command.CommandExecutor;
import net.kaikk.msm.command.Actor;
import net.kaikk.msm.server.Server;

public class DetachCommand implements CommandExecutor {
	final ModularServersManager instance = ModularServersManager.instance();
	@Override
	public Object process(Actor sender, String command, String[] arguments) {
		final Server attachedServer = sender.getAttachedServer();
		if (attachedServer != null) {
			instance.getConsole().setAttachedServer(null);
			sender.sendMessage("Server "+attachedServer.getId()+" detached");
		} else {
			sender.sendMessage("No attached server");
		}
		return attachedServer;
	}

	
	@Override
	public String shortDescription(Actor sender, String command, String... arguments) {
		return "Detaches from the currently attached server console.";
	}
}
