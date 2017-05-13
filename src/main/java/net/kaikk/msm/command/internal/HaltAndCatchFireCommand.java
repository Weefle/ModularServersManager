package net.kaikk.msm.command.internal;

import net.kaikk.msm.ModularServersManager;
import net.kaikk.msm.command.Actor;
import net.kaikk.msm.command.CommandExecutor;

public class HaltAndCatchFireCommand implements CommandExecutor {
	final ModularServersManager instance = ModularServersManager.instance();
	
	@Override
	public Object process(Actor sender, String command, String[] arguments) {
		instance.haltAndCatchFire();
		return null;
	}

	@Override
	public String shortDescription(Actor sender, String command, String... arguments) {
		return "Kills all running servers and immediately halts this program.";
	}
	
	@Override
	public String longDescription(Actor sender, String command, String... arguments) {
		return "Kills all running servers and immediately halts this program. Useful if you need to terminate this program as soon as possible. If you want to gracefully stop this program, use !halt instead. HACF means \"Halt And Catch Fire\"";
	}
}
