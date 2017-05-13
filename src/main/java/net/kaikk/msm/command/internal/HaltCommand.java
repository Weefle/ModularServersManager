package net.kaikk.msm.command.internal;

import net.kaikk.msm.ModularServersManager;
import net.kaikk.msm.command.Actor;
import net.kaikk.msm.command.CommandExecutor;

public class HaltCommand implements CommandExecutor {
	final ModularServersManager instance = ModularServersManager.instance();
	
	@Override
	public Object process(Actor sender, String command, String[] arguments) {
		instance.halt();
		return null;
	}

	@Override
	public String shortDescription(Actor sender, String command, String... arguments) {
		return "Stops all running servers and then terminates this program.";
	}
}
