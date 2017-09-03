package net.kaikk.msm;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.nio.file.Files;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.WriterAppender;

import com.google.common.collect.EvictingQueue;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigObject;
import com.typesafe.config.ConfigValue;

import jline.TerminalFactory;
import jline.console.ConsoleReader;
import jline.console.UserInterruptException;
import net.kaikk.msm.command.Actor;
import net.kaikk.msm.command.CommandContainer;
import net.kaikk.msm.event.command.SendConsoleMessageEvent;
import net.kaikk.msm.event.server.ServerAddEvent;
import net.kaikk.msm.event.server.ServerConsoleInputEvent;
import net.kaikk.msm.event.server.ServerRemoveEvent;
import net.kaikk.msm.module.ModulesManager;
import net.kaikk.msm.server.ConsoleLine;
import net.kaikk.msm.server.Server;
import net.kaikk.msm.util.Utils;

public class ModularServersManager {
	public static final long CREATION_TIME = System.currentTimeMillis();
	
	protected static ModularServersManager instance;
	protected Config config;
	
	protected final Map<String,Server> servers = new ConcurrentHashMap<String,Server>();
	
	protected final ConsoleReader consoleReader;
	protected final ModulesManager manager;
	protected final Logger logger;
	protected final Actor console;
	protected final File configFile = new File("config.conf");
	
	protected final Collection<ConsoleLine> lines = Collections.synchronizedCollection(EvictingQueue.create(1000));
	protected final WriterAppender appender;
	
	public ModularServersManager() throws Throwable {
		instance = this;

		// logger
		this.logger = Logger.getLogger("MSM");

		this.logger.info("ModularServersManager v."+AppInfo.VERSION+" by KaiNoMood");
		
		Writer writer = new Writer() {
			@Override
			public void write(char[] cbuf, int off, int len) throws IOException {
				final String s = new String(cbuf, off, len);
				if (s.trim().isEmpty()) {
					return;
				}
				lines.add(new ConsoleLine(s, false));
			}
			
			@Override
			public void flush() throws IOException {
				
			}
			
			@Override
			public void close() throws IOException {
				
			}
		};
		
		this.appender = new WriterAppender(new PatternLayout("[%c][%d{yyyy-MM-dd HH:mm:ss} %p] %m%n"), writer);
		this.appender.setName("CONSOLE_APPENDER");
		this.appender.setThreshold(org.apache.log4j.Level.INFO);
		this.logger.addAppender(appender);
		
		try {
			this.console = new ConsoleActor();
			
			// load config
			log("Loading configuration...");
			this.reloadConfig();
			
			// load modules
			log("Loading modules manager...");
			manager = new ModulesManager(this);
			manager.loadModules(new File("modules"));
			
			// start servers
			for (final Server server : this.servers.values()) {
				if (server.isAutostart()) {
					try {
						server.start();
					} catch (Throwable e) {
						logger.error("An error occurred while automatically starting server id \""+server.getId()+"\"");
						e.printStackTrace();
					}
				}
			}
			
			// console
			consoleReader = new ConsoleReader();
			this.consoleHandler();
		} catch (Throwable e) {
			logger.fatal(Utils.stackTraceToString(e));
			throw e;
		}
	}

	protected void consoleHandler() throws IOException {
		try {
			consoleReader.setHandleUserInterrupt(true);
            consoleReader.setExpandEvents(false);
            
            logger.info("Done! Type !help for a list of commands");
            
            String line = null;
            while(true) {
	            try {
					while ((line = consoleReader.readLine()) != null) {
						if (line.startsWith("!")) {
							this.processCommand(console, line.substring(1));
						} else if (console.getAttachedServer() == null) {
							if (!line.trim().isEmpty()) {
								logger.info("Type !help for a list of commands");
							}
						} else {
							if (!console.getAttachedServer().isAlive()) {
								if (!line.trim().isEmpty()) {
									logger.error(console.getAttachedServer().getId()+" is offline. Do \"!start "+console.getAttachedServer().getId()+"\" to start it.");
								}
							} else {
								final ServerConsoleInputEvent event = new ServerConsoleInputEvent(console.getAttachedServer(), console, line);
								manager.callEventPre(event);
								if (!event.isCancelled()) {
									console.getAttachedServer().getController().writeln(line, this.getConsole(), false);
								}
								manager.callEventPost(event);
							}
						}
					}
				} catch (UserInterruptException e) {
					if (this.console.getAttachedServer() != null) {
						this.console.getAttachedServer().stop();
					} else {
						this.halt();
					}
				} catch (Throwable e) {
					e.printStackTrace();
				}
            }
        } finally {
            try {
                TerminalFactory.get().restore();
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
	}
	
	/**
	 * Processes the specified command line.<br>
	 * The console will be used as actor.
	 * 
	 * @param commandLine the command line, without the ! prefix
	 * @return the result from the command executor, usually null.
	 */
	public Object processCommand(String commandLine) {
		return this.processCommand(console, commandLine);
	}
	
	/**
	 * Processes the specified command line as run by the specified sender.
	 * 
	 * @param sender whoever sent the command
	 * @param commandLine the command line, without the ! prefix
	 * @return the result from the command executor, usually null.
	 */
	public Object processCommand(Actor sender, String commandLine) {
		if (sender == null) {
			throw new NullPointerException();
		}
		
		final String[] cmdsArgs = commandLine.split(" ", 2);
		
		final String cmd = cmdsArgs[0].trim().toLowerCase();
		if (cmd.isEmpty()) {
			return null;
		}
		
		final String[] arguments = cmdsArgs.length > 1 ? cmdsArgs[1].split(" ") : Utils.EMPTY_STRING_ARRAY;
		
		final CommandContainer commandContainer = manager.getCommand(cmd);
		final SendConsoleMessageEvent event = new SendConsoleMessageEvent(cmd, arguments);
		
		if (commandContainer == null) {
			manager.callEvent(event);
			sender.sendMessage("Invalid command.");
			return null;
		} else {
			manager.callEventPre(event);
			Object o = null;
			if (!event.isCancelled()) {
				try {
					o = commandContainer.getExecutor().process(sender, cmd, arguments);
				} catch (Throwable e) {
					sender.sendMessage("An error occurred while running the specified command: "+e.getMessage());
					e.printStackTrace();
				}
			}
			manager.callEventPost(event);
			
			return o;
		}
	}
	
	/**
	 * Reloads the config.<br>
	 * If any server is removed from the config, the server process will be stopped and removed.<br>
	 * If the removed server couldn't be stop within 60 seconds, the process will be killed.<br>
	 * A server counts as removed if the server id for a currently loaded server is not on the config anymore.<p>
	 * 
	 * If new servers are added to the config, they will be loaded but not automatically started regardless their StartAutomatically setting.
	 * 
	 * @throws IOException if the default config could not be loaded from the jar file.
	 */
	public void reloadConfig() throws IOException {
		if (!configFile.exists()) {
			// copy default
			final InputStream is = getClass().getClassLoader().getResourceAsStream("msm-default-config.conf");
			Files.copy(is, configFile.toPath());
			logger.warn("Please edit the config.conf to configure ModularServersManager!");
			System.exit(-2);
		}
		
		this.config = ConfigFactory.parseFile(configFile);
		
		final ConfigObject configServers = this.config.getConfig("Servers").root();
		
		// unload servers that are not on the config anymore
		final Set<String> serversInConfig = configServers.keySet();
		final Iterator<Entry<String, Server>> it = servers.entrySet().iterator();
		while (it.hasNext()) {
			final Entry<String, Server> entry = it.next();
			if (!serversInConfig.contains(entry.getKey())) {
				// the server is not on the config anymore... stop (if running) and remove it
				final Server server = entry.getValue();
				logger.warn("Removing server id "+server.getId());
				if (server.isAlive()) {
					try {
						server.stop();
						if (!server.waitForServerClosed(60, TimeUnit.SECONDS)) {
							server.kill();
						}
					} catch (Throwable e) {
						e.printStackTrace();
					}
				}
				it.remove();
				manager.callEvent(new ServerRemoveEvent(server));
			}
		}
		
		for (final Entry<String, ConfigValue> entry : configServers.entrySet()) {
			final String id = entry.getKey();
			log("Loading server \""+id+"\" config...");
			
			final Config c = ((ConfigObject) entry.getValue()).toConfig();
			final String name = c.getString("Name");
			final String workingDirectory = c.getString("WorkingDirectory");
			final List<String> serverProcessCommand = c.getStringList("ServerProcessCommand");
			final String serverReadyLine = c.getString("ServerReadyLine");
			final int startTimeout = c.getInt("StartTimeout");
			final String stopCommand = c.getString("StopCommand");
			final int stopTimeout = c.getInt("StopTimeout");
			final boolean startAutomatically = c.getBoolean("StartAutomatically");
			final boolean restartAutomatically = c.getBoolean("RestartAutomatically");
			final List<String> externalCommandsBeforeStart = c.getStringList("ExternalCommandsBeforeStart");
			final List<String> externalCommandsAfterStop = c.getStringList("ExternalCommandsAfterStop");
			
			final Config watchDog = c.getObject("WatchDog").toConfig();
			final String watchDogCommand = watchDog.getString("Command");
			final String watchDogResponse = watchDog.getString("Response");
			final int watchDogInterval = watchDog.getInt("Interval");
			final int watchDogTimeout = watchDog.getInt("Timeout");
			
			Server server = this.servers.get(id);
			if (server != null) {
				// server has been already loaded... update data
				server.setName(name);
				server.setWorkingDirectory(workingDirectory);
				server.setServerProcessCommands(serverProcessCommand);
				server.setServerReadyLine(Pattern.compile(serverReadyLine));
				server.setStartTimeout(startTimeout);
				server.setStopCommand(stopCommand);
				server.setStopTimeout(stopTimeout);
				server.setAutostart(startAutomatically);
				server.setAutorestart(restartAutomatically);
				server.setCommandsBeforeStart(externalCommandsBeforeStart);
				server.setCommandsAfterStop(externalCommandsAfterStop);
				server.setWatchDogCommand(watchDogCommand);
				server.setWatchDogResponseLine(Pattern.compile(watchDogResponse));
				server.setWatchDogInterval(watchDogInterval);
				server.setWatchDogTimeout(watchDogTimeout);
			} else {
				// new server
				server = new Server(this, id, name, workingDirectory, serverProcessCommand, serverReadyLine, startTimeout, stopCommand, stopTimeout, watchDogCommand, watchDogResponse, watchDogInterval, watchDogTimeout, externalCommandsBeforeStart, externalCommandsAfterStop, startAutomatically, restartAutomatically);
				this.servers.put(id, server);
			}
			
			if (manager != null) {
				manager.callEvent(new ServerAddEvent(server));
			}
		}
	}
	
	/**
	 * Stops all running servers and then exit.
	 */
	public void halt() {
		instance.getLogger().fatal("Halt! Closing all running servers...");
		try {
			for (Server server : this.getServers().values()) {
				if (server.isAlive()) {
					server.stop();
				}
			}
			
			for (Server server : this.getServers().values()) {
				if (server.isAlive()) {
					server.waitForServerClosed(60, TimeUnit.SECONDS);
				}
			}
		} catch (Throwable t) {
			t.printStackTrace();
		}
		System.exit(0);
	}
	
	/**
	 * Forcefully kill all running servers and then immediately exit.
	 */
	public void haltAndCatchFire() {
		instance.getLogger().fatal("Halt and catch fire!");
		try {
			for (Server server : instance.getServers().values()) {
				if (server.isAlive()) {
					server.kill();
				}
			}
		} catch (Throwable t) {
			t.printStackTrace();
		}
		System.exit(-1);
	}
	
	static void log(String s) {
		instance.logger.info(s);
	}

	/**
	 * Provides access to the main instance of ModularServersManager.
	 * 
	 * @return the main instance.
	 */
	public static ModularServersManager instance() {
		return instance;
	}
	
	/**
	 * Provides access to the modules manager.<br>
	 * The modules manager allows modules to perform various operations, like registering new commands and calling events.
	 * 
	 * @return the modules manager.
	 */
	public static ModulesManager modulesManager() {
		return instance.manager;
	}

	/**
	 * The logger.
	 * 
	 * @return the logger.
	 */
	public Logger getLogger() {
		return logger;
	}

	/**
	 * Provides access to the modules manager.<br>
	 * The modules manager allows modules to perform various operations, like registering new commands and calling events.
	 * 
	 * @return the modules manager.
	 */
	public ModulesManager getManager() {
		return manager;
	}

	/**
	 * @return the main config.
	 */
	public Config getConfig() {
		return config;
	}

	/**
	 * @return the main console actor.
	 */
	public Actor getConsole() {
		return console;
	}

	/**
	 * Gets the server with the specified id.
	 * 
	 * @param id the server id.
	 * @return the server, null if it doesn't exist.
	 */
	public Server getServer(String id) {
		return this.servers.get(id);
	}

	/**
	 * Get a map of all servers. The map key is the server id.
	 * 
	 * @return
	 */
	public Map<String, Server> getServers() {
		return servers;
	}

	/**
	 * @return the main configuration file
	 */
	public File getConfigFile() {
		return configFile;
	}

	/**
	 * Provides a collection of the latest 1000 lines written by the logger.
	 * 
	 * @return a collection of logged lines
	 */
	public Collection<ConsoleLine> getLines() {
		return lines;
	}

	/**
	 * Provides a writer appender that can be used by custom loggers to log lines. 
	 * 
	 * @return
	 */
	public WriterAppender getLinesAppender() {
		return appender;
	}
	
	/**
	 * Redraws the current line written on the console.
	 * 
	 * @throws IOException
	 */
	public void consoleRedrawLine() throws IOException {
		this.consoleReader.redrawLine();
	}
}
