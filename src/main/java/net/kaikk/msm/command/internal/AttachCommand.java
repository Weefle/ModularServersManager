package net.kaikk.msm.command.internal;

import net.kaikk.msm.ModularServersManager;
import net.kaikk.msm.command.CommandExecutor;
import net.kaikk.msm.command.Actor;
import net.kaikk.msm.server.Server;
import net.kaikk.msm.server.ConsoleLine;

public class AttachCommand implements CommandExecutor {
	final ModularServersManager instance = ModularServersManager.instance();
	
	@Override
	public Object process(Actor sender, String command, String[] arguments) {
		if (arguments.length == 0) {
			sender.sendMessage("Usage: !"+command+" (server id)");
			return null;
		}
		final Server server = instance.getServer(arguments[0]);
		if (server == null) {
			sender.sendMessage("Server id "+arguments[0]+" does not exist");
			return null;
		}
		
		final Server attachedServer = sender.getAttachedServer();
		if (attachedServer != null) {
			sender.sendMessage("Server "+attachedServer.getId()+" detached");
		}
		
		if (!server.getLines().isEmpty()) {
			synchronized(server.getLines()) {
				for (ConsoleLine line : server.getLines()) {
					sender.sendRawMessage("["+server.getId()+"]"+line);
				}
			}
		}
		
		instance.getConsole().setAttachedServer(server);
		
		return null;
	}
	
	@Override
	public String shortDescription(Actor sender, String command, String... arguments) {
		return "Attaches the specified server console.";
	}
	
	@Override
	public String longDescription(Actor sender, String command, String... arguments) {
		return "Usage: !"+command+" (server id)\n\n"
				+ " Attaches the specified server console.\n"
				+ " In order words, this command lets you read and write to the specified server console.";
	}
}
