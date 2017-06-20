package net.kaikk.msm.server;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import com.google.common.collect.EvictingQueue;

import net.kaikk.msm.ModularServersManager;
import net.kaikk.msm.command.Actor;
import net.kaikk.msm.event.Cancellable;
import net.kaikk.msm.event.server.ServerKilledEvent;
import net.kaikk.msm.event.server.ServerStartEvent;
import net.kaikk.msm.event.server.ServerStopEvent;
import net.kaikk.msm.event.server.ServerStoppedEvent;
import net.kaikk.msm.util.BufferedLineReaderThread;

public class Server {
	protected final ModularServersManager instance;
	protected final String id;
	protected String name, workingDirectory, stopCommand, watchDogCommand;
	protected Pattern serverReadyLine, watchDogResponseLine;
	protected List<String> serverProcessCommands, commandsBeforeStart, commandsAfterStop;
	volatile protected boolean startOnNextStop, autostart, autorestart;
	volatile protected int startTimeout, stopTimeout, watchDogInterval, watchDogTimeout;
	volatile protected ServerState state = ServerState.STOPPED;
	volatile protected ServerController controller;
	volatile protected int lastExitCode;
	volatile protected long lastStateChangeTime;
	protected final Collection<ConsoleLine> lines = Collections.synchronizedCollection(EvictingQueue.create(1000));
	protected final Logger logger;
	protected final Set<Actor> attachedActors = new HashSet<Actor>();
	
	public Server(final ModularServersManager instance, String id, String name, String workingDirectory, List<String> serverProcessCommands, String serverReadyLine, int startTimeout, String stopCommand, int stopTimeout, String watchDogCommand, String watchDogResponseLine, int watchDogInterval, int watchDogTimeout, List<String> commandsBeforeStart, List<String> commandsAfterStop, boolean autostart, boolean autorestart) {
		this.logger = Logger.getLogger(id);
		this.logger.addAppender(instance.getLinesAppender());
		this.instance = instance;
		this.id = id;
		this.name = name;
		this.workingDirectory = workingDirectory;
		this.serverProcessCommands = new CopyOnWriteArrayList<>(serverProcessCommands);
		this.serverReadyLine = Pattern.compile(serverReadyLine);
		this.startTimeout = startTimeout;
		this.stopCommand = stopCommand;
		this.stopTimeout = stopTimeout;
		this.watchDogCommand = watchDogCommand;
		this.watchDogResponseLine = Pattern.compile(watchDogResponseLine);
		this.watchDogInterval = watchDogInterval;
		this.watchDogTimeout = watchDogTimeout;
		this.commandsBeforeStart = new CopyOnWriteArrayList<>(commandsBeforeStart);
		this.commandsAfterStop = new CopyOnWriteArrayList<>(commandsAfterStop);
		this.autostart = autostart;
		this.autorestart = autorestart;
	}
	
	/**
	 * Starts the server. This is the preferred method for starting the server as it does not block the caller. A new thread running all the necessary preliminary operations will start.
	 * 
	 * @throws IllegalStateException if the server is already running
	 */
	public void start() throws IllegalStateException {
		if (this.state == ServerState.STARTING || this.isAlive()) {
			throw new IllegalStateException(this.id+" is already running");
		}
		
		new Thread() {
			@Override
			public void run() {
				try {
					startProcess();
				} catch (IllegalStateException | IOException e) {
					e.printStackTrace();
				}
			}
		}.start();
	}

	/**
	 * Starts the process. This will likely block until the actual server process is started.
	 * 
	 * @throws IllegalStateException if the server is already running
	 * @throws IOException
	 */
	synchronized public void startProcess() throws IllegalStateException, IOException {
		if (this.state == ServerState.STARTING || this.isAlive()) {
			throw new IllegalStateException(this.id+" is already running");
		}
		
		this.startOnNextStop = false;
		final Cancellable event = new ServerStartEvent(this);
		instance.getManager().callEventPre(event);
		if (event.isCancelled()) {
			instance.getManager().callEventPost(event);
			return;
		}
		
		this.setState(ServerState.STARTING);
		instance.getManager().callEventPost(event);
		
		if (!this.commandsBeforeStart.isEmpty()) {
			this.logger.info("Running external commands before start...");
			for (final String rawCmd : this.commandsBeforeStart) {
				final String cmd = rawCmd
						.replace("%MSM_SERVER_ID", id+"")
						.replace("%MSM_SERVER_NAME", name)
						.replace("%MSM_SERVER_WORKING_DIR", workingDirectory);
				try {
					this.sendRawMessageToAttachedActors("Running external command: "+cmd);
					this.lines.add(new ConsoleLine("Running external command: "+cmd, false)); // add to lines history
					
					final Process extProcess = Runtime.getRuntime().exec(cmd, null, new File(this.workingDirectory));
	
					try (final BufferedLineReaderThread in = new BufferedLineReaderThread(new BufferedReader(new InputStreamReader(extProcess.getInputStream()), 1048576), "Server_"+this.getId()+"_BeforeStart_InReader", 4096);
						final BufferedLineReaderThread err = new BufferedLineReaderThread(new BufferedReader(new InputStreamReader(extProcess.getErrorStream()), 1048576), "Server_"+this.getId()+"_BeforeStart_ErrReader", 4096);) {
						
						in.start();
						err.start();
						
						while (extProcess.isAlive()) {
							final String lineIn = in.lines().poll();
							if (lineIn != null) {
								this.sendRawMessageToAttachedActors(lineIn);
								this.lines.add(new ConsoleLine(lineIn, false)); // add to lines history
								
								final int skippedLines = in.skippedLines();
								if (skippedLines > 0) {
									this.sendRawMessageToAttachedActors("Skipped "+skippedLines+" lines.");
									this.lines.add(new ConsoleLine("Skipped "+skippedLines+" lines.", true));
								}
							}
							
							final String lineErr = in.lines().poll();
							if (lineErr != null) {
								this.sendRawMessageToAttachedActors(lineErr);
								this.lines.add(new ConsoleLine(lineErr, true)); // add to lines history
								
								final int skippedLines = in.skippedLines();
								if (skippedLines > 0) {
									this.sendRawMessageToAttachedActors("Skipped "+skippedLines+" lines.");
									this.lines.add(new ConsoleLine("Skipped "+skippedLines+" lines.", true));
								}
							}
						}
					}
					lastExitCode = extProcess.exitValue();
					if (lastExitCode != 0) {
						this.sendRawMessageToAttachedActors("An error occurred while running external command: "+cmd+": Exit Code: "+lastExitCode);
						this.lines.add(new ConsoleLine("An error occurred while running external command: "+cmd+": Exit Code: "+lastExitCode, true));
						
						this.stop();
						return;
					}
				} catch (Throwable e) {
					this.sendRawMessageToAttachedActors("An error occurred while running external command: "+cmd+": "+e.getMessage());
					this.lines.add(new ConsoleLine("An error occurred while running external command: "+cmd+": "+e.getMessage(), true));
					
					e.printStackTrace();
					this.stop();
					return;
				}
			}
		}
		
		this.controller = new ServerController(this);
		
		
	}
	
	/**
	 * Stops the server
	 * 
	 * @throws IllegalStateException if the server is not running
	 * @throws IOException
	 */
	public void stop() throws IllegalStateException, IOException {
		if (this.state != ServerState.STARTING && !this.isAlive()) {
			throw new IllegalStateException(this.id+" is not running");
		}
		
		this.startOnNextStop = false;
		if (this.controller != null) {
			final Cancellable event = new ServerStopEvent(this);
			instance.getManager().callEventPre(event);
			if (!event.isCancelled()) {
				this.setState(ServerState.STOPPING);
				this.controller.writeln(stopCommand);
				
				if (stopTimeout != 0) {
					this.controller.stopTimeoutTime = System.currentTimeMillis() + (stopTimeout * 1000L);
				}
			}
			instance.getManager().callEventPost(event);
		} else {
			this.setState(ServerState.STOPPED);
			instance.getManager().callEvent(new ServerStoppedEvent(this));
		}
	}
	
	/**
	 * Restarts the server
	 * 
	 * @throws IOException
	 */
	public void restart() throws IOException {
		if (this.isAlive()) {
			this.startOnNextStop = true;
			final Cancellable event = new ServerStopEvent(this);
			instance.getManager().callEventPre(event);
			if (!event.isCancelled()) {
				this.setState(ServerState.STOPPING);
				this.controller.writeln(stopCommand);
				
				if (stopTimeout != 0) {
					this.controller.stopTimeoutTime = System.currentTimeMillis() + (stopTimeout * 1000L);
				}
			}
			instance.getManager().callEventPost(event);
		} else {
			this.start();
		}
	}
	
	/**
	 * Kills the server. This forcibly destroys the server process.
	 * @see Process#destroyForcibly()
	 */
	public void kill() {
		if (controller != null) {
			this.getLogger().fatal("Killing server process!");
			this.controller.schedule.cancel(true);
			this.controller.process.destroyForcibly();
			this.controller = null;
		}
		this.setState(ServerState.KILLED);
		instance.getManager().callEvent(new ServerKilledEvent(this));
	}
	
	/**
	 * @return whether the server process is running
	 */
	public boolean isAlive() {
		return controller != null && controller.process.isAlive();
	}
	
	/**
	 * Waits until the server is stopped.
	 * 
	 * @return the return code 
	 * @throws InterruptedException
	 */
	public int waitForServerClosed() throws InterruptedException {
		if (controller == null) {
			return lastExitCode;
		}
		
		return controller.process.waitFor();
	}
	
	/**
	 * Waits until the server is stopped or the timeout occurs.
	 * 
	 * @param timeout the timeout
	 * @param unit the unit for the timeout
	 * @return true if the server was stopped, false if the timeout occurred.
	 * @throws InterruptedException
	 */
	public boolean waitForServerClosed(long timeout, TimeUnit unit) throws InterruptedException {
		if (controller == null) {
			return true;
		}
		
		return controller.process.waitFor(timeout, unit);
	}

	/**
	 * @return the server state
	 */
	public ServerState getState() {
		return state;
	}
	
	/**
	 * Sets the server state. This won't change the actual state of the server.
	 * 
	 * @param state the new server state
	 */
	public void setState(final ServerState state) {
		this.state = state;
		this.lastStateChangeTime = System.currentTimeMillis();
	}

	public enum ServerState {
		STOPPED, STARTING, RUNNING, STOPPING, KILLED;
	}

	/**
	 * @return the server id
	 */
	public String getId() {
		return id;
	}

	/**
	 * @return the server working directory
	 */
	public String getWorkingDirectory() {
		return workingDirectory;
	}

	/**
	 * @return the command that is sent to the server console to stop the server
	 */
	public String getStopCommand() {
		return stopCommand;
	}

	/**
	 * @return a list of commands that are used to start the server process instance
	 */
	public List<String> getServerProcessCommands() {
		return serverProcessCommands;
	}

	/**
	 * @return whether the server should autostart after ModularServersManager has been started
	 */
	public boolean isAutostart() {
		return autostart;
	}

	/**
	 * @return this server process controller, null if the server is not running
	 */
	public ServerController getController() {
		return controller;
	}

	/**
	 * @return a collection of the latest 1000 lines from this server console
	 */
	public Collection<ConsoleLine> getLines() {
		return lines;
	}

	/**
	 * @return the watch dog command
	 */
	public String getWatchDogCommand() {
		return watchDogCommand;
	}

	/**
	 * @return a list of commands to run before the server is started
	 */
	public List<String> getCommandsBeforeStart() {
		return commandsBeforeStart;
	}
	
	/**
	 * @return a list of commands to run after the server is stopped
	 */
	public List<String> getCommandsAfterStop() {
		return commandsAfterStop;
	}

	/**
	 * @return whether this server should be restarted if it stops unexpectedly
	 */
	public boolean isAutorestart() {
		return autorestart;
	}

	/**
	 * @return the start timeout, in seconds.
	 */
	public int getStartTimeout() {
		return startTimeout;
	}

	/**
	 * @return the stop timeout, in seconds.
	 */
	public int getStopTimeout() {
		return stopTimeout;
	}

	/**
	 * @return the interval between sending the watchdog command, in seconds.
	 */
	public int getWatchDogInterval() {
		return watchDogInterval;
	}

	/**
	 * @return the last exit code for the server process.
	 */
	public int getLastExitCode() {
		return lastExitCode;
	}

	/**
	 * @return the watchdog timeout, in seconds.
	 */
	public int getWatchDogTimeout() {
		return watchDogTimeout;
	}

	/**
	 * @return the server name
	 */
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setWorkingDirectory(String workingDirectory) {
		this.workingDirectory = workingDirectory;
	}

	public void setStopCommand(String stopCommand) {
		this.stopCommand = stopCommand;
	}

	public void setWatchDogCommand(String watchDogCommand) {
		this.watchDogCommand = watchDogCommand;
	}

	public void setServerReadyLine(Pattern serverReadyLine) {
		this.serverReadyLine = serverReadyLine;
	}

	public void setWatchDogResponseLine(Pattern watchDogResponseLine) {
		this.watchDogResponseLine = watchDogResponseLine;
	}

	public void setServerProcessCommands(List<String> serverProcessCommands) {
		this.serverProcessCommands = serverProcessCommands;
	}

	public void setCommandsBeforeStart(List<String> commandsBeforeStart) {
		this.commandsBeforeStart = new CopyOnWriteArrayList<>(commandsBeforeStart);
	}

	public void setCommandsAfterStop(List<String> commandsAfterStop) {
		this.commandsAfterStop = new CopyOnWriteArrayList<>(commandsAfterStop);
	}

	public void setStartOnNextStop(boolean startOnNextStop) {
		this.startOnNextStop = startOnNextStop;
	}

	public void setAutostart(boolean autostart) {
		this.autostart = autostart;
	}

	public void setAutorestart(boolean autorestart) {
		this.autorestart = autorestart;
	}

	public void setStopTimeout(int stopTimeout) {
		this.stopTimeout = stopTimeout;
	}

	public void setStartTimeout(int startTimeout) {
		this.startTimeout = startTimeout;
	}

	public void setWatchDogInterval(int watchDogInterval) {
		this.watchDogInterval = watchDogInterval;
	}

	public void setWatchDogTimeout(int watchDogTimeout) {
		this.watchDogTimeout = watchDogTimeout;
	}

	/**
	 * @return this server logger.
	 */
	public Logger getLogger() {
		return logger;
	}

	/**
	 * @return a set of actors that are attached to this console
	 */
	public Set<Actor> getAttachedActors() {
		return attachedActors;
	}

	/**
	 * @return the epoch time of the last state change 
	 */
	public long getLastStateChangeTime() {
		return lastStateChangeTime;
	}
	
	public void sendRawMessageToAttachedActors(String rawMessage) {
		for (Actor actor : this.attachedActors) {
			actor.sendRawMessage("["+this.id+"] "+rawMessage);
		}
	}
	
	public void sendMessageToAttachedActors(String rawMessage) {
		for (Actor actor : this.attachedActors) {
			actor.sendMessage("["+this.id+"] "+rawMessage);
		}
	}
}
