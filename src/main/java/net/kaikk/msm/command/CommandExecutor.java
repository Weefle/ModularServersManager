package net.kaikk.msm.command;

/**
 * Interface for defining new commands
 * 
 * @author Kai
 *
 */
public interface CommandExecutor {
	/**
	 * The method that will be called whenever this command is used.
	 * 
	 * @param sender the command sender
	 * @param command the command sent
	 * @param arguments the arguments sent
	 * @return a return object, can be null.
	 * @throws Throwable
	 */
	Object process(Actor sender, String command, String[] arguments) throws Throwable;

	/**
	 * A short description for this command that will be shown by the "!help" command.
	 * 
	 * @param sender the command sender
	 * @param command the command sent
	 * @param arguments the arguments sent
	 * @return the description
	 */
	default String shortDescription(Actor sender, String command, String... arguments) {
		return "";
	}
	
	/**
	 * A long description for this command that will be shown by the "!help (command)" command.
	 * 
	 * @param sender the command sender
	 * @param command the command sent
	 * @param arguments the arguments sent
	 * @return the description
	 */
	default String longDescription(Actor sender, String command, String... arguments) {
		return "";
	}
}
