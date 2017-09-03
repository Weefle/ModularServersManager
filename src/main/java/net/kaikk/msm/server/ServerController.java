package net.kaikk.msm.server;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import net.kaikk.msm.command.Actor;
import net.kaikk.msm.event.Cancellable;
import net.kaikk.msm.event.server.ServerConsoleInputEvent;
import net.kaikk.msm.event.server.ServerConsoleOutputEvent;
import net.kaikk.msm.event.server.ServerKilledEvent;
import net.kaikk.msm.event.server.ServerReadyEvent;
import net.kaikk.msm.event.server.ServerStoppedEvent;
import net.kaikk.msm.server.Server.ServerState;
import net.kaikk.msm.util.BufferedProcessLineReader;
import net.kaikk.msm.util.BufferedProcessLineReader.LogLine;

public class ServerController implements Runnable {
	protected final Server server;
	protected final Process process;
	protected final BufferedProcessLineReader processLines;
	protected final BufferedWriter out;
	protected final ScheduledFuture<?> schedule;
	protected final long creationDate;
	
	protected final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(0, new ThreadFactory() {
		@Override
		public Thread newThread(Runnable r) {
			if (r instanceof ServerController) {
				ServerController serverController = (ServerController) r;
				return new Thread(r, "Server_"+serverController.getServer().getId());
			}
			return Executors.defaultThreadFactory().newThread(r);
		}
	});

	volatile protected long watchDogNextTimeout, stopTimeoutTime, nextWatchDogCheckTime;
	
	protected ServerController(final Server server) throws IOException {
		this.server = server;
		this.creationDate = System.currentTimeMillis();

		final ProcessBuilder processBuilder = new ProcessBuilder(server.serverProcessCommands);
		processBuilder.directory(new File(server.workingDirectory));
		
		process = processBuilder.start();
		server.logger.info("Server process started.");
		server.sendRawMessageToAttachedActors("Server process started.");
		server.lines.add(new ConsoleLine("Server process started.", false));
		
		processLines = new BufferedProcessLineReader(process, "Server_"+this.getServer().getId(), 16384);
		
		out = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()), 102400);
		
		schedule = scheduler.scheduleAtFixedRate(this, 100, 100, TimeUnit.MILLISECONDS);
	}
	
	@Override
	public void run() {
		try {
			final long t = System.currentTimeMillis();
			LogLine logLine;
			try {
				while ((logLine = processLines.poll(10L, TimeUnit.MILLISECONDS)) != null && System.currentTimeMillis() - t < 100) {
					final String line = logLine.getString();
					// add to lines history
					final ConsoleLine serverConsoleLine = new ConsoleLine(line, logLine.isError());
					server.lines.add(serverConsoleLine);
					
					// call event
					server.instance.getManager().callEvent(new ServerConsoleOutputEvent(server, serverConsoleLine, logLine.isError()));
					
					this.handleConsoleLine(line);
					
					final int skippedLines = processLines.skippedLines();
					if (skippedLines > 0) {
						server.sendRawMessageToAttachedActors("Skipped "+skippedLines+" lines.");
						server.lines.add(new ConsoleLine("Skipped "+skippedLines+" lines.", true));
					}
				}
			} catch (InterruptedException e1) {
				
			}
			
			if(!process.isAlive()) {
				server.sendAndLogMessage("Server process has terminated.");
				
				server.controller = null;
				schedule.cancel(true);
				try {
					server.lastExitCode = process.exitValue();
				} catch (IllegalThreadStateException ex) {
					// ignore
				}
				
				if (!server.commandsAfterStop.isEmpty()) {
					server.logger.info("Running external commands after stop...");
					
					for (final String rawCmd : server.commandsAfterStop) {
						this.handleCommandAfterStop(rawCmd);
					}
				}
				
				if (server.state != ServerState.STOPPING || server.startOnNextStop) {
					server.setState(ServerState.STOPPED);
					server.instance.getManager().callEvent(new ServerStoppedEvent(server));
					// autorestart
					if (server.autorestart || server.startOnNextStop) {
						server.startOnNextStop = false;
						try {
							server.logger.info("Restarting...");
							server.startProcess();
						} catch (IllegalStateException | IOException e) {
							e.printStackTrace();
						}
					}
				} else {
					server.setState(ServerState.STOPPED);
					server.instance.getManager().callEvent(new ServerStoppedEvent(server));
				}
				return;
			}
			
			if (server.state == ServerState.STARTING && server.startTimeout > 0 && System.currentTimeMillis() - creationDate > server.startTimeout * 1000L) {
				// server start timed out
				server.sendAndLogMessage("Start timeout! Killing server process...");
				process.destroyForcibly();
				server.setState(ServerState.KILLED);
				server.instance.getManager().callEvent(new ServerKilledEvent(server));
			}
			
			// WatchDog check
			if (server.state == ServerState.RUNNING) {
				if (isElapsed(nextWatchDogCheckTime)) {
					// run watchdog command and set the watchdog timeout
					watchDogNextTimeout = nextWatchDogCheckTime + (server.getWatchDogTimeout() * 1000L);
					nextWatchDogCheckTime = 0;
					this.writeln(server.watchDogCommand);
				} else if (isElapsed(watchDogNextTimeout)) {
					// the watchdog command timed out... kill the server
					server.logger.warn("WatchDog timeout! Killing server process...");
					process.destroyForcibly();
					server.setState(ServerState.KILLED);
					server.instance.getManager().callEvent(new ServerKilledEvent(server));
				}
			}
			
			// stop timeout check
			if (stopTimeoutTime != 0) {
				if (server.state == ServerState.STOPPING) {
					if (System.currentTimeMillis() - stopTimeoutTime >= 0) {
						server.sendAndLogMessage("Stop timeout! Killing server process...");
						process.destroyForcibly();
						server.setState(ServerState.KILLED);
						server.instance.getManager().callEvent(new ServerKilledEvent(server));
					}
				} else {
					stopTimeoutTime = 0;
				}
			}

			if (server.attachedActors.contains(server.instance.getConsole())) {
				server.instance.consoleRedrawLine();
			}
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}
	
	protected void handleCommandAfterStop(String rawCmd) {
		final String cmd = rawCmd
				.replace("%MSM_SERVER_ID", server.id+"")
				.replace("%MSM_SERVER_NAME", server.name)
				.replace("%MSM_SERVER_WORKING_DIR", server.workingDirectory);
		
		server.sendRawMessageToAttachedActors("Running external command: "+cmd);
		server.lines.add(new ConsoleLine("Running external command: "+cmd, false)); // add to lines history
		
		try {
			final Process extProcess = Runtime.getRuntime().exec(cmd, null, new File(server.workingDirectory));
			final BufferedProcessLineReader extProcessLines = new BufferedProcessLineReader(extProcess, "Server_"+this.server.getId()+"_AfterStop", 16384);
			
			LogLine logLine;
			try {
				while(extProcess.isAlive()) {
					while ((logLine = extProcessLines.poll(10L, TimeUnit.MILLISECONDS)) != null) {
						final String line = logLine.getString();
						server.sendRawMessageToAttachedActors(line);
						server.lines.add(new ConsoleLine(line, logLine.isError())); // add to lines history
						
						final int skippedLines = extProcessLines.skippedLines();
						if (skippedLines > 0) {
							server.sendRawMessageToAttachedActors("Skipped "+skippedLines+" lines.");
							server.lines.add(new ConsoleLine("Skipped "+skippedLines+" lines.", true));
						}
					}
					
					if (server.getState() == ServerState.KILLED) {
						extProcess.destroyForcibly();
						return;
					}
				}
			} catch (InterruptedException e) {
				
			}
			
			int ecode = extProcess.waitFor();
			if (ecode != 0) {
				server.logger.info("External command exited with code "+ecode);
			}
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}

	protected void handleConsoleLine(String line) {
		// if attached server, show in console
		server.sendRawMessageToAttachedActors(line);
		
		switch(server.state) {
		case STARTING: {
			if (server.serverReadyLine.matcher(line).matches()) {
				// server is ready (matching server ready line)
				server.setState(ServerState.RUNNING);
				server.instance.getManager().callEvent(new ServerReadyEvent(server));
				server.logger.info("Server ready");
				
				// initialize watchdog if enabled
				if (server.watchDogInterval > 0 && server.watchDogTimeout > 0) {
					nextWatchDogCheckTime = System.currentTimeMillis() + (server.getWatchDogInterval() * 1000L);
				}
			}
			break;
		}
		case RUNNING: {
			if (watchDogNextTimeout > 0 && server.watchDogResponseLine.matcher(line).matches()) {
				// this is a watchdog response line
				nextWatchDogCheckTime = System.currentTimeMillis() + (server.getWatchDogInterval() * 1000L);
				watchDogNextTimeout = 0;
			}
			break;
		}
		default:
			break;
		}
	}

	/**
	 * @return the server process
	 */
	public Process getProcess() {
		return process;
	}

	/**
	 * Writes the specified string to the server console. A {@link ServerConsoleInputEvent} will be fired.
	 * 
	 * @param string the string to write
	 * @throws IOException
	 */
	public void write(String string) throws IOException {
		this.write(string, server.instance.getConsole(), true);
	}
	
	/**
	 * Writes the specified string to the server console.
	 * 
	 * @param string the string to write
	 * @param actor the actor who sends this string
	 * @param fireEvent whether to fire the {@link ServerConsoleInputEvent} event
	 * @throws IOException
	 */
	public void write(String string, Actor actor, boolean fireEvent) throws IOException {
		if (fireEvent) {
			Cancellable event = new ServerConsoleInputEvent(server, actor, string);
			server.instance.getManager().callEvent(event);
			if (event.isCancelled()) {
				return;
			}
		}
		this.out.write(string);
		this.out.flush();
	}
	
	/**
	 * Writes the specified line to the server console. A {@link ServerConsoleInputEvent} will be fired.<p>
	 * This method will also append a new line character (line feed) at the end of the line.
	 * 
	 * @param line the string to write
	 * @throws IOException
	 */
	public void writeln(String line) throws IOException {
		this.writeln(line, server.instance.getConsole(), true);
	}
	
	/**
	 * Writes the specified line to the server console.<p>
	 * This method will also append a new line character (line feed) at the end of the line.
	 * 
	 * @param line the line to write
	 * @param actor the actor who sends this line
	 * @param fireEvent whether to fire the {@link ServerConsoleInputEvent} event
	 * @throws IOException
	 */
	public void writeln(String line, Actor actor, boolean fireEvent) throws IOException {
		if (fireEvent) {
			Cancellable event = new ServerConsoleInputEvent(server, actor, line);
			server.instance.getManager().callEvent(event);
			if (event.isCancelled()) {
				return;
			}
		}
		this.out.write(line);
		this.out.write(System.lineSeparator());
		this.out.flush();
	}

	/**
	 * @return the server for this server process
	 */
	public Server getServer() {
		return server;
	}
	
	protected static boolean isElapsed(long time) {
		return time > 0  && System.currentTimeMillis() - time > 0;
	}

	/**
	 * @return the time this server process has been created
	 */
	public long getCreationDate() {
		return creationDate;
	}
	
	
}
