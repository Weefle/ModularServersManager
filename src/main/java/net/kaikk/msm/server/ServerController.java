package net.kaikk.msm.server;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.BufferOverflowException;
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
import net.kaikk.msm.util.BufferedLineReader;

public class ServerController implements Runnable {
	protected final Server server;
	protected final Process process;
	protected final BufferedLineReader in;
	protected final BufferedLineReader err;
	protected final BufferedWriter out;
	protected final ScheduledFuture<?> schedule;
	protected final long creationDate;
	
	protected final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1, new ThreadFactory() {
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

		in = new BufferedLineReader(new InputStreamReader(process.getInputStream()), 102400);
		err = new BufferedLineReader(new InputStreamReader(process.getErrorStream()), 102400);
		out = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()), 102400);

		schedule = scheduler.scheduleAtFixedRate(this, 100, 100, TimeUnit.MILLISECONDS);
	}
	
	@Override
	public void run() {
		try {
			if(!process.isAlive()) {
				server.controller = null;
				schedule.cancel(true);
				try {
					server.lastExitCode = process.exitValue();
				} catch (IllegalThreadStateException ex) {
					// ignore
				}
				
				server.logger.info("Server process has terminated.");
				
				for (final String rawCmd : server.commandsAfterStop) {
					final String cmd = rawCmd
							.replace("%MSM_SERVER_ID", server.id+"")
							.replace("%MSM_SERVER_NAME", server.name)
							.replace("%MSM_SERVER_WORKING_DIR", server.workingDirectory);
					server.logger.info("Running external command: "+cmd);
					try {
						final Process extProcess = Runtime.getRuntime().exec(cmd, null, new File(server.workingDirectory));
						try (final BufferedLineReader in = new BufferedLineReader(new InputStreamReader(extProcess.getInputStream()), 102400);
							final BufferedLineReader err = new BufferedLineReader(new InputStreamReader(extProcess.getErrorStream()), 102400);) {
							while (extProcess.isAlive()) {
								String line = null;
								try {
									while ((line = in.nextLine()) != null || (line = err.nextLine()) != null) {
										server.logger.info(line);
									}
								} catch (BufferOverflowException e) {
									if (!in.isBufferEmpty()) {
										line = in.getBufferAndClear();
									} else if (!err.isBufferEmpty()) {
										line = err.getBufferAndClear();
									} else {
										throw e;
									}
									server.logger.info(line+"[LINE TOO LONG!]");
								}
							}
						}
						int ecode = extProcess.exitValue();
						if (ecode != 0) {
							server.logger.info("External command exited with code "+ecode);
						}
					} catch (Throwable e) {
						e.printStackTrace();
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

			String line, lineErr = null;
			try {
				while((line = in.nextLine()) != null || (lineErr = err.nextLine()) != null) {
					this.handleConsoleLine(line, lineErr);
				}
			} catch (BufferOverflowException e) {
				if (!in.isBufferEmpty()) {
					line = in.getBufferAndClear()+"[LINE TOO LONG!]";
				} else if (!err.isBufferEmpty()) {
					line = null;
					lineErr = err.getBufferAndClear()+"[LINE TOO LONG!]";
				} else {
					throw e;
				}
				this.handleConsoleLine(line, lineErr);
			}
			
			if (server.state == ServerState.STARTING && server.startTimeout > 0 && System.currentTimeMillis() - creationDate > server.startTimeout * 1000L) {
				// server start timed out
				server.logger.warn("Start timeout! Killing server process...");
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
						server.logger.warn("Stop timeout! Killing server process...");
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
	
	protected void handleConsoleLine(String line, String lineErr) {
		if (line != null) {
			// add to lines history
			final ConsoleLine serverConsoleLine = new ConsoleLine(line, false);
			server.lines.add(serverConsoleLine);
			
			// call event
			server.instance.getManager().callEvent(new ServerConsoleOutputEvent(server, serverConsoleLine));
		} else {
			line = lineErr;
			
			// add to lines history
			final ConsoleLine serverConsoleLine = new ConsoleLine(lineErr, true);
			server.lines.add(serverConsoleLine);
			
			// call event
			server.instance.getManager().callEvent(new ServerConsoleOutputEvent(server, serverConsoleLine, true));
		}

		// if attached server, show in console
		for (Actor actor : server.attachedActors) {
			actor.sendRawMessage("["+server.id+"]"+line);
		}
		
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
