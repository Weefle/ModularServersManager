package net.kaikk.msm.command;

import net.kaikk.msm.module.ModuleContainer;

public class CommandContainer {
	protected final ModuleContainer moduleContainer;
	protected final CommandExecutor executor;
	protected final String command;
	protected final String[] aliases;
	
	public CommandContainer(ModuleContainer moduleContainer, CommandExecutor executor, String command, String... aliases) {
		this.moduleContainer = moduleContainer;
		this.executor = executor;
		this.command = command.toLowerCase();
		this.aliases = new String[aliases.length];
		for (int i = 0; i < aliases.length; i++) {
			this.aliases[i] = aliases[i].toLowerCase();
		}
	}

	public ModuleContainer getModuleContainer() {
		return moduleContainer;
	}

	public CommandExecutor getExecutor() {
		return executor;
	}

	public String getCommand() {
		return command;
	}

	public String[] getAliases() {
		return aliases;
	}
}
