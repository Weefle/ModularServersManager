package net.kaikk.msm.command.internal;

import java.io.IOException;
import java.util.regex.Pattern;

import net.kaikk.msm.ModularServersManager;
import net.kaikk.msm.command.CommandExecutor;
import net.kaikk.msm.command.Actor;
import net.kaikk.msm.server.Server;
import net.kaikk.msm.util.Utils;

public class SendCommand implements CommandExecutor {
	final ModularServersManager instance = ModularServersManager.instance();
	@Override
	public Object process(Actor sender, String command, String[] arguments) throws IllegalStateException, IOException {
		if (arguments.length < 2) {
			sender.sendMessage(this.longDescription(sender, command, arguments));
			return null;
		}
		
		for (final String serverId : arguments[0].split(Pattern.quote(","))) {
			try {
				final Server server = instance.getServer(serverId);
				if (server == null) {
					sender.sendMessage("Server id "+serverId+" does not exist");
					continue;
				}
				
				if (!server.isAlive()) {
					sender.sendMessage("Server id "+serverId+" is not running");
					continue;
				}
				
				server.getController().writeln(Utils.mergeStringArrayFromIndex(arguments, 1));
			} catch (Throwable t) {
				t.printStackTrace();
			}
		}
		
		return null;
	}
	
	@Override
	public String shortDescription(Actor sender, String command, String... arguments) {
		return "Sends the specified text line to the specified server(s)";
	}
	
	@Override
	public String longDescription(Actor sender, String command, String... arguments) {
		return "Usage: !"+command+" (server id1,[server id2],...) (text...)";
	}
}
