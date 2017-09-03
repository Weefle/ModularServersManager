package net.kaikk.msm.command.internal;

import net.kaikk.msm.AppInfo;
import net.kaikk.msm.ModularServersManager;
import net.kaikk.msm.command.Actor;
import net.kaikk.msm.command.CommandExecutor;

public class VersionCommand implements CommandExecutor {
	final ModularServersManager instance = ModularServersManager.instance();
	
	@Override
	public Object process(Actor sender, String command, String[] arguments) {
		sender.sendMessage("ModularServersManager v."+AppInfo.VERSION+" by KaiNoMood");
		return null;
	}
	
	@Override
	public String shortDescription(Actor sender, String command, String... arguments) {
		return "current running version";
	}
	
	@Override
	public String longDescription(Actor sender, String command, String... arguments) {
		return "Prints the ModularServersManager version";
	}
}
