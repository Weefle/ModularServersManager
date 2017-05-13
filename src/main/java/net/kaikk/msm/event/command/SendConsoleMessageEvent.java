package net.kaikk.msm.event.command;

import net.kaikk.msm.event.CancellableEvent;

/**
 * Event called when a message is being sent to the server console
 */
public class SendConsoleMessageEvent extends CancellableEvent {
	private String command;
	private String[] arguments;

	public SendConsoleMessageEvent(String command, String... arguments) {
		this.command = command;
		this.arguments = arguments;
	}

	public String getCommand() {
		return command;
	}

	public void setCommand(String command) {
		this.command = command;
	}

	public String[] getArguments() {
		return arguments;
	}

	public void setArguments(String[] arguments) {
		this.arguments = arguments;
	}
	
	
}
