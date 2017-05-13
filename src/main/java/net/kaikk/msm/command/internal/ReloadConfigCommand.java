package net.kaikk.msm.command.internal;

import java.io.IOException;

import net.kaikk.msm.ModularServersManager;
import net.kaikk.msm.command.CommandExecutor;
import net.kaikk.msm.command.Actor;

public class ReloadConfigCommand implements CommandExecutor {
	final ModularServersManager instance = ModularServersManager.instance();
	@Override
	public Object process(Actor sender, String command, String[] arguments) throws IllegalStateException, IOException {
		try {
			instance.reloadConfig();
			sender.sendMessage("Config reloaded");
		} catch (Exception e) {
			e.printStackTrace();
			sender.sendMessage("A fatal error occurred while reloading the config. ModularServersManager configuration may be broken! Fix it ASAP!");
		}
		return null;
	}
	
	@Override
	public String shortDescription(Actor sender, String command, String... arguments) {
		return "Reloads global config file";
	}
	
	@Override
	public String longDescription(Actor sender, String command, String... arguments) {
		return "Reloads global config file. All servers removed from the config will be killed (if running) and removed. All servers added to the config will be added.";
	}
}
