package net.kaikk.msm.command.internal;

import java.util.Arrays;
import java.util.TreeSet;

import net.kaikk.msm.ModularServersManager;
import net.kaikk.msm.command.CommandContainer;
import net.kaikk.msm.command.CommandExecutor;
import net.kaikk.msm.command.Actor;
import net.kaikk.msm.util.Utils;

public class HelpCommand implements CommandExecutor {
	final ModularServersManager instance = ModularServersManager.instance();
	
	@Override
	public Object process(Actor sender, String command, String[] arguments) {
		if (arguments.length == 0) {
			sender.sendMessage("Commands list:");
			final TreeSet<String> tree = new TreeSet<>(instance.getManager().getCommands());
			for (String cs : tree) {
				final CommandContainer container = instance.getManager().getCommand(cs);
				if (container != null) {
					final String shortDesc = container.getExecutor().shortDescription(sender, command);
					sender.sendMessage("- "+cs+(shortDesc.isEmpty() ? "" : " : "+shortDesc));
				}
			}
		} else {
			CommandContainer container = instance.getManager().getCommand(arguments[0]);
			if (container == null) {
				sender.sendMessage("Command \""+command+"\" not found.");
			} else {
				final String[] nArgs = arguments.length > 1 ? Arrays.copyOfRange(arguments, 1, arguments.length) : Utils.EMPTY_STRING_ARRAY;
				final String longDesc = container.getExecutor().longDescription(sender, arguments[0], nArgs);

				if (longDesc.isEmpty()) {
					final String shortDesc = container.getExecutor().shortDescription(sender, arguments[0], nArgs);
					if (shortDesc.isEmpty()) {
						sender.sendMessage("No help found for command "+arguments[0]);
					} else {
						sender.sendMessage("Help Command: "+arguments[0]);
						sender.sendMessage(shortDesc);
					}
				} else {
					sender.sendMessage("Help Command: "+arguments[0]);
					sender.sendMessage(longDesc);
				}
			}
		}
		return null;
	}
	
	@Override
	public String shortDescription(Actor sender, String command, String... arguments) {
		return "This help";
	}
	
	@Override
	public String longDescription(Actor sender, String command, String... arguments) {
		return "Do you think the help command needs a description..?";
	}
}
