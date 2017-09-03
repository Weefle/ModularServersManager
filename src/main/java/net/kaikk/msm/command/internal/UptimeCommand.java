package net.kaikk.msm.command.internal;

import java.lang.management.ManagementFactory;
import java.time.Duration;

import net.kaikk.msm.ModularServersManager;
import net.kaikk.msm.command.Actor;
import net.kaikk.msm.command.CommandExecutor;

public class UptimeCommand implements CommandExecutor {
	final ModularServersManager instance = ModularServersManager.instance();
	
	@Override
	public Object process(Actor sender, String command, String[] arguments) {
		final Duration uptime = Duration.ofMillis(ManagementFactory.getRuntimeMXBean().getUptime());
		
        final long max = Runtime.getRuntime().maxMemory() / 1024 / 1024;
        final long total = Runtime.getRuntime().totalMemory() / 1024 / 1024;
        final long free = Runtime.getRuntime().freeMemory() / 1024 / 1024;
		
		sender.sendMessage("Uptime: "+uptime.toString());
		sender.sendMessage("Memory:");
		sender.sendMessage("- Free: "+free);
		sender.sendMessage("- Total: "+total);
		sender.sendMessage("- Max: "+max);
		sender.sendMessage("Threads: "+ManagementFactory.getThreadMXBean().getThreadCount());
		
		return null;
	}
	
	@Override
	public String shortDescription(Actor sender, String command, String... arguments) {
		return "ModularServersManager uptime";
	}
	
	@Override
	public String longDescription(Actor sender, String command, String... arguments) {
		return "Prints the ModularServersManager uptime";
	}
}
