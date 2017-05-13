package net.kaikk.msm.command.internal;

import java.io.IOException;

import net.kaikk.msm.ModularServersManager;
import net.kaikk.msm.command.Actor;
import net.kaikk.msm.command.CommandExecutor;
import net.kaikk.msm.server.Server;
import net.kaikk.msm.util.Utils;

public class SendAllCommand implements CommandExecutor {
	final ModularServersManager instance = ModularServersManager.instance();
	@Override
	public Object process(Actor sender, String command, String[] arguments) throws IllegalStateException, IOException {
		if (arguments.length < 2) {
			sender.sendMessage(this.longDescription(sender, command, arguments));
			return null;
		}
		
		for (final Server server : instance.getServers().values()) {
			try {
				if (server.isAlive()) {
					server.getController().writeln(Utils.mergeStringArrayFromIndex(arguments, 1));
				}
			} catch (Throwable t) {
				t.printStackTrace();
			}
		}
		
		return null;
	}
	
	@Override
	public String shortDescription(Actor sender, String command, String... arguments) {
		return "Sends the specified text line to all running servers";
	}
	
	@Override
	public String longDescription(Actor sender, String command, String... arguments) {
		return "Usage: !"+command+" (text...)";
	}
}
