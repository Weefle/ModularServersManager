package net.kaikk.msm.command.internal;

import java.lang.management.ManagementFactory;
import java.time.Duration;

import net.kaikk.msm.ModularServersManager;
import net.kaikk.msm.command.Actor;
import net.kaikk.msm.command.CommandExecutor;
import net.kaikk.msm.util.Utils;

public class UptimeCommand implements CommandExecutor {
	final ModularServersManager instance = ModularServersManager.instance();
	
	@Override
	public Object process(Actor sender, String command, String[] arguments) {
        final long max = Runtime.getRuntime().maxMemory() / 1024 / 1024;
        final long total = Runtime.getRuntime().totalMemory() / 1024 / 1024;
        final long free = Runtime.getRuntime().freeMemory() / 1024 / 1024;
		
		sender.sendMessage("Uptime: "+Utils.formatDuration(ManagementFactory.getRuntimeMXBean().getUptime()));
		sender.sendMessage("Memory:");
		sender.sendMessage("- Free: "+free+" MiB");
		sender.sendMessage("- Total: "+total+" MiB");
		sender.sendMessage("- Max: "+max+" MiB");
		sender.sendMessage("Threads: "+ManagementFactory.getThreadMXBean().getThreadCount());
		
		return null;
	}
	
	@Override
	public String shortDescription(Actor sender, String command, String... arguments) {
		return "Shows uptime, memory and thread count statistics";
	}
	
	@Override
	public String longDescription(Actor sender, String command, String... arguments) {
		return "Prints the ModularServersManager uptime";
	}
}
