package net.kaikk.msm.module;

import java.io.File;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

import com.impetus.annovention.ClasspathDiscoverer;
import com.impetus.annovention.Discoverer;
import com.impetus.annovention.listener.ClassAnnotationDiscoveryListener;

import net.kaikk.msm.ModularServersManager;
import net.kaikk.msm.command.CommandContainer;
import net.kaikk.msm.command.CommandExecutor;
import net.kaikk.msm.command.internal.AttachCommand;
import net.kaikk.msm.command.internal.DetachCommand;
import net.kaikk.msm.command.internal.HaltAndCatchFireCommand;
import net.kaikk.msm.command.internal.HaltCommand;
import net.kaikk.msm.command.internal.HelpCommand;
import net.kaikk.msm.command.internal.KillAllServersCommand;
import net.kaikk.msm.command.internal.KillServerCommand;
import net.kaikk.msm.command.internal.ReloadConfigCommand;
import net.kaikk.msm.command.internal.RestartAllServersCommand;
import net.kaikk.msm.command.internal.RestartServerCommand;
import net.kaikk.msm.command.internal.SendAllCommand;
import net.kaikk.msm.command.internal.SendCommand;
import net.kaikk.msm.command.internal.ServerListCommand;
import net.kaikk.msm.command.internal.StartAllServersCommand;
import net.kaikk.msm.command.internal.StartServerCommand;
import net.kaikk.msm.command.internal.StopAllServersCommand;
import net.kaikk.msm.command.internal.StopServerCommand;
import net.kaikk.msm.command.internal.UptimeCommand;
import net.kaikk.msm.command.internal.VersionCommand;
import net.kaikk.msm.event.Cancellable;
import net.kaikk.msm.event.Event;
import net.kaikk.msm.event.EventHandler;
import net.kaikk.msm.event.LoggedEvent;
import net.kaikk.msm.event.Order;
import net.kaikk.msm.event.module.InitializationEvent;
import net.kaikk.msm.util.JarFilenameFilter;
import net.kaikk.msm.util.Tristate;
import net.kaikk.msm.util.Utils;


/**
 * The modules manager allows modules to perform various operations, like registering new commands and calling events.
 * 
 * @author Kai
 *
 */
public class ModulesManager {
	protected final ModularServersManager instance;
	protected final Map<String,ModuleContainer> modules = new ConcurrentHashMap<>();
	protected final Map<Class<? extends Event>, TreeSet<RegisteredListener>> listeners = new ConcurrentHashMap<>();
	protected final Map<Class<? extends Event>, TreeSet<RegisteredListener>> postListeners = new ConcurrentHashMap<>();
	protected final Map<String,CommandContainer> commands = new ConcurrentHashMap<>();
	protected final Map<String,CommandContainer> aliasesCommands = new ConcurrentHashMap<>();
	
	public ModulesManager(ModularServersManager instance) {
		this.instance = instance;
		
		// Internal Commands
		this.internalCommand(new HaltAndCatchFireCommand(), "hacf");
		this.internalCommand(new HaltCommand(), "halt");
		this.internalCommand(new HelpCommand(), "help");
		this.internalCommand(new AttachCommand(), "attach");
		this.internalCommand(new DetachCommand(), "detach");
		this.internalCommand(new StartServerCommand(), "start");
		this.internalCommand(new StopServerCommand(), "stop");
		this.internalCommand(new RestartServerCommand(), "restart");
		this.internalCommand(new KillServerCommand(), "kill");
		this.internalCommand(new ServerListCommand(), "list");
		this.internalCommand(new ReloadConfigCommand(), "reloadconfig");
		this.internalCommand(new StartAllServersCommand(), "startall");
		this.internalCommand(new StopAllServersCommand(), "stopall");
		this.internalCommand(new RestartAllServersCommand(), "restartall");
		this.internalCommand(new KillAllServersCommand(), "killall");
		this.internalCommand(new SendCommand(), "send");
		this.internalCommand(new SendAllCommand(), "sendall");
		this.internalCommand(new UptimeCommand(), "uptime");
		this.internalCommand(new VersionCommand(), "version");
	}
	
	protected void internalCommand(final CommandExecutor commandExecutor, String command, String...aliases) {
		final CommandContainer container = new CommandContainer(null, commandExecutor, command, aliases);
		commands.put(command, container);
		aliasesCommands.put(command, container);
		for (String alias : aliases) {
			aliasesCommands.put(alias, container);
		}
	}
	
	/**
	 * Registers a new command.<p>
	 * 
	 * Use {@link #registerCommand(ModuleContainer, CommandExecutor, String, String...)} if possible.
	 * 
	 * @param module the module instance
	 * @param commandExecutor the command executor
	 * @param command the command name
	 * @param aliases an optional list of aliases for this command
	 */
	public void registerCommand(final Object module, final CommandExecutor commandExecutor, final String command, final String...aliases) {
		this.registerCommand(this.getModuleContainer(module), commandExecutor, command, aliases);
	}

	/**
	 * Registers a new command.
	 * 
	 * @param moduleContainer the module container
	 * @param commandExecutor the command executor
	 * @param command the command name
	 * @param aliases an optional list of aliases for this command
	 */
	public void registerCommand(final ModuleContainer moduleContainer, final CommandExecutor commandExecutor, final String command, final String...aliases) {
		final CommandContainer commandContainer = new CommandContainer(moduleContainer, commandExecutor, command, aliases);
		commands.put(commandContainer.getCommand(), commandContainer);
		for (String alias : commandContainer.getAliases()) {
			aliasesCommands.put(alias, commandContainer);
		}
	}
	
	/**
	 * Gets the specified command
	 * 
	 * @param command the command name
	 * @return the command container
	 */
	public CommandContainer getCommand(final String command) {
		return this.commands.get(command.toLowerCase());
	}

	/**
	 * Remove the specified command. Notice that aliases for this same command won't be removed. Use unregisterCommand method for that.
	 * @param command
	 */
	public void removeCommand(final String command) {
		this.commands.remove(command.toLowerCase());
	}
	
	/**
	 * Remove the specified alias. Notice that the main command and other aliases for this same command won't be removed. Use unregisterCommand method for that.
	 * @param alias
	 */
	public void removeAlias(final String alias) {
		this.aliasesCommands.remove(alias.toLowerCase());
	}
	
	/**
	 * Unregisters the specified command and all the aliases
	 * @param commandContainer
	 */
	public void unregisterCommand(final CommandContainer commandContainer) {
		this.commands.remove(commandContainer.getCommand(), commandContainer);
		this.aliasesCommands.remove(commandContainer.getCommand(), commandContainer);
		for (final String alias : commandContainer.getAliases()) {
			this.aliasesCommands.remove(alias, commandContainer);
		}
	}
	
	/**
	 * Unregisters all commands and all the aliases registered with the specified module
	 * @param moduleContainer
	 */
	public void unregisterCommands(final ModuleContainer moduleContainer) {
		Iterator<CommandContainer> it = this.commands.values().iterator();
		while (it.hasNext()) {
			final CommandContainer cm = it.next();
			if (moduleContainer.equals(cm.getModuleContainer())) {
				it.remove();
			}
		}
		
		it = this.aliasesCommands.values().iterator();
		while (it.hasNext()) {
			final CommandContainer cm = it.next();
			if (moduleContainer.equals(cm.getModuleContainer())) {
				it.remove();
			}
		}
	}
	
	/**
	 * Registers the event handlers on the specified event listener.<p>
	 * Use {@link #registerEvents(Object, ModuleContainer)} if possible.
	 * 
	 * @param listener the listener
	 * @param module the module registering the listener
	 */
	public void registerEvents(final Object listener, final Object module) {
		this.registerEvents(listener, this.getModuleContainer(module));
	}
	
	/**
	 * Registers the event handlers on the specified event listener.
	 * 
	 * @param listener the listener
	 * @param moduleContainer the module container registering the listener
	 */
	@SuppressWarnings("unchecked")
	public void registerEvents(final Object listener, final ModuleContainer moduleContainer) {
		for (final Method method : listener.getClass().getMethods()) {
			try {
				final EventHandler annotation = method.getAnnotation(EventHandler.class);
				if (annotation != null) {
					final Class<?>[] paramTypes = method.getParameterTypes();
					if (!Event.class.isAssignableFrom(paramTypes[0])) {
						throw new IllegalArgumentException("method "+method.getName()+" is not a valid listener.");
					}
					if (annotation.order() != Order.POST) {
						TreeSet<RegisteredListener> events = listeners.get(paramTypes[0]);
						if (events == null) {
							events = new TreeSet<>();
							listeners.put((Class<? extends Event>) paramTypes[0], events);
						}
						events.add(new RegisteredListener(listener, moduleContainer, method, annotation.order(), annotation.runWhenCancelled()));
					} else {
						TreeSet<RegisteredListener> events = postListeners.get(paramTypes[0]);
						if (events == null) {
							events = new TreeSet<>();
							postListeners.put((Class<? extends Event>) paramTypes[0], events);
						}
						events.add(new RegisteredListener(listener, moduleContainer, method, annotation.order(), annotation.runWhenCancelled()));
					}
				}
			} catch (Throwable t) {
				instance.getLogger().error("Skipping Event Listener "+moduleContainer.id()+":"+listener.getClass().getSimpleName()+": "+t.getMessage());
			}
		}
	}

	/**
	 * Unregister all listeners for the specified event
	 * @param event
	 */
	public void unregisterListeners(final Class<? extends Event> event) {
		listeners.remove(event);
		postListeners.remove(event);
	}


	/**
	 * Unregister all listeners on the specified listener object
	 * @param listener
	 */
	public void unregisterListeners(final Object listener) {
		for (final TreeSet<RegisteredListener> treeSet : listeners.values()) {
			final Iterator<RegisteredListener> iterator = treeSet.iterator();
			while(iterator.hasNext()) {
				final RegisteredListener registeredListener = iterator.next();
				if (registeredListener.listener.equals(listener)) {
					iterator.remove();
				}
			}
		}
		for (final TreeSet<RegisteredListener> treeSet : postListeners.values()) {
			final Iterator<RegisteredListener> iterator = treeSet.iterator();
			while(iterator.hasNext()) {
				final RegisteredListener registeredListener = iterator.next();
				if (registeredListener.listener.equals(listener)) {
					iterator.remove();
				}
			}
		}
	}

	/**
	 * Unregister all listeners for the specified module
	 * @param container
	 */
	public void unregisterListeners(final ModuleContainer container) {
		for (final TreeSet<RegisteredListener> treeSet : listeners.values()) {
			final Iterator<RegisteredListener> iterator = treeSet.iterator();
			while(iterator.hasNext()) {
				final RegisteredListener registeredListener = iterator.next();
				if (registeredListener.moduleContainer.equals(container)) {
					iterator.remove();
				}
			}
		}
		
		for (final TreeSet<RegisteredListener> treeSet : postListeners.values()) {
			final Iterator<RegisteredListener> iterator = treeSet.iterator();
			while(iterator.hasNext()) {
				final RegisteredListener registeredListener = iterator.next();
				if (registeredListener.moduleContainer.equals(container)) {
					iterator.remove();
				}
			}
		}
	}

	/**
	 * Call the specified event
	 * @param event
	 */
	public void callEvent(final Event event) {
		this.callEvent(event, event.getClass());
	}

	/**
	 * Call the specified event only for not Order.POST listeners
	 * @param event
	 */
	public void callEventPre(final Event event) {
		this.callEventPre(event, event.getClass());
	}

	/**
	 * Call the specified event only for Order.POST listeners
	 * @param event
	 */
	public void callEventPost(final Event event) {
		this.callEventPost(event, event.getClass());
	}
	
	protected void callEvent(final Event event, final Class<?> clazz) {
		// call event for the specified class
		this.callEventPre(event, clazz);
		this.callEventPost(event, clazz);
		
		// recursively call the event for all superclasses that implement Event
		final Class<?> superClass = clazz.getSuperclass();
		if (superClass != null && Event.class.isAssignableFrom(superClass)) {
			this.callEvent(event, superClass);
		}
		
		// recursively call the event for all superinterfaces that implement the Event interface
		for (final Class<?> interf : clazz.getInterfaces()) {
			if (Event.class.isAssignableFrom(interf)) {
				this.callEvent(event, interf);
			}
		}
	}
	
	protected void callEventPre(final Event event, final Class<?> clazz) {
		final TreeSet<RegisteredListener> treeSet = this.listeners.get(clazz);
		if (treeSet != null) {
			for (final RegisteredListener registeredListener : treeSet) {
				processEventHandler(event, registeredListener);
			}
		}
	}
	
	protected void callEventPost(final Event event, final Class<?> clazz) {
		final TreeSet<RegisteredListener> treeSet = this.postListeners.get(clazz);
		if (treeSet != null) {
			for (final RegisteredListener registeredListener : treeSet) {
				processEventHandler(event, registeredListener);
			}
		}
	}
	
	/**
	 * Call the specified event. The runnable will run before all the Order.POST listeners
	 * @param event
	 * @param runnable
	 */
	public void callEvent(final Event event, final Runnable runnable) {
		this.callEventPre(event, event.getClass());		
		runnable.run();
		this.callEventPost(event, event.getClass());
	}
	
	protected void processEventHandler(final Event event, final RegisteredListener registeredListener) {
		try {
			if (event instanceof InitializationEvent) {
				instance.getLogger().info("Initializing module \""+registeredListener.moduleContainer.id()+"\"");
			} else if (event instanceof LoggedEvent) {
				instance.getLogger().info("Calling event \""+event.getClass().getSimpleName()+"\" for "+registeredListener.moduleContainer.id()+" @ "+registeredListener.method.getClass().getSimpleName()+"#"+registeredListener.method.getName());
			}
			
			// method arguments
			final Object[] args = new Object[registeredListener.method.getParameterTypes().length];
			args[0] = event;
			for (int i = 1; i < registeredListener.method.getParameterTypes().length; i++) {
				final Class<?> param = registeredListener.method.getParameterTypes()[i];
				if (ModuleContainer.class.isAssignableFrom(param)) {
					args[i] = registeredListener.moduleContainer;
				}
			}
			
			if (event instanceof Cancellable && registeredListener.runWhenCancelled != Tristate.UNDEFINED) {
				final Cancellable cancellable = (Cancellable) event;
				if (!cancellable.isCancelled() ^ registeredListener.runWhenCancelled == Tristate.TRUE) {
					registeredListener.method.invoke(registeredListener.listener, args);
				}
			} else {
				registeredListener.method.invoke(registeredListener.listener, args);
			}
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}

	/**
	 * Load all modules from the specified directory.
	 * 
	 * @param modulesDir
	 */
	public void loadModules(final File modulesDir) {
		modulesDir.mkdirs();
		if (!modulesDir.isDirectory()) {
			throw new IllegalArgumentException("Modules directory "+modulesDir.getName()+" is invalid");
		}
		
		Utils.extendSystemClassLoaderWithJar(modulesDir.listFiles(JarFilenameFilter.instance));
		
		final TreeSet<ModuleContainer> foundModules = new TreeSet<>();
		
		final Discoverer discoverer = new ClasspathDiscoverer();
		discoverer.addAnnotationListener(new ClassAnnotationDiscoveryListener() {
			@Override
			public String[] supportedAnnotations() {
				return Module.annotationClassName;
			}
			
			@Override
			public void discovered(final String clazz, final String annotation) {
				try {
					final Class<?> moduleClass = Class.forName(clazz, true, ClassLoader.getSystemClassLoader());
					final Module moduleAnnotation = moduleClass.getAnnotation(Module.class);

					instance.getLogger().info("Found module \""+moduleAnnotation.id()+"\"");
					
					final ModuleContainer container = new ModuleContainer(moduleClass.newInstance());
					
					if (!foundModules.add(container)) {
						instance.getLogger().info("Found duplicate module: "+container.id()+" "+container.version());
					}
				} catch (Throwable e) {
					e.printStackTrace();
				}
			}
		});
		discoverer.discover(true, false, false, false, true, false);
		
		// check dependencies
		instance.getLogger().info("Checking modules dependencies...");
		for (ModuleContainer container : foundModules) {
			for (String dependency : container.dependencies()) {
				boolean missing = true;
				for (ModuleContainer otherContainer : foundModules) {
					if (otherContainer.id().equals(dependency)) {
						missing = false;
						break;
					}
				}
				
				if (missing) {
					instance.getLogger().fatal("Module \""+container.id()+"\" is missing module dependency: "+dependency);
				}
			}
		}
		
		// registering modules
		for (ModuleContainer container : foundModules) {
			modules.put(container.id(), container);
			registerEvents(container.getModuleInstance(), container.getModuleInstance());
		}
		
		// call initialization event
		this.callEvent(new InitializationEvent(this.instance, this));
	}

	/**
	 * Provides an unmodifiable collection of all the registered commands
	 * 
	 * @return a collection of commands
	 */
	public Collection<String> getCommands() {
		return Collections.unmodifiableCollection(commands.keySet());
	}
	
	/**
	 * Provides an unmodifiable maps of all commands. The key is the command name.
	 * 
	 * @return a map of commands
	 */
	public Map<String,CommandContainer> getCommandsMap() {
		return Collections.unmodifiableMap(this.commands);
	}

	/**
	 * Gets the module container for the specified module instance.
	 * 
	 * @param module the module instance
	 * @return the module container for the specified module instance
	 */
	public ModuleContainer getModuleContainer(final Object module) {
		return this.modules.get(getModuleId(module));
	}

	protected static String getModuleId(final Object module) {
		final Module annotation = module.getClass().getAnnotation(Module.class);
		if (annotation == null) {
			throw new IllegalArgumentException(module+" is not a Module");
		}
		return annotation.id();
	}
	
	protected static boolean isModule(final Object object) {
		return object.getClass().isAnnotationPresent(Module.class);
	}
	
	protected static void assertModule(final Object object) {
		if (!isModule(object)) {
			throw new IllegalArgumentException(object.getClass().getName()+" is not a module");
		}
	}

	protected static class RegisteredListener implements Comparable<RegisteredListener> {
		protected final Object listener;
		protected final ModuleContainer moduleContainer;
		protected final Method method;
		protected final Tristate runWhenCancelled;
		protected final Order order;

		protected RegisteredListener(final Object listener, final ModuleContainer module, final Method method, final Order order, final Tristate runWhenCancelled) {
			this.listener = listener;
			this.moduleContainer = module;
			this.method = method;
			this.order = order;
			this.runWhenCancelled = runWhenCancelled;
		}

		@Override
		public int compareTo(final RegisteredListener o) {
			final int x = this.order.compareTo(o.order);
			if (x != 0) {
				return -x;
			}
			
			return this.moduleContainer.compareTo(o.moduleContainer);
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((listener == null) ? 0 : listener.hashCode());
			result = prime * result + ((method == null) ? 0 : method.hashCode());
			result = prime * result + ((moduleContainer == null) ? 0 : moduleContainer.hashCode());
			result = prime * result + ((order == null) ? 0 : order.hashCode());
			result = prime * result + ((runWhenCancelled == null) ? 0 : runWhenCancelled.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null) {
				return false;
			}
			if (!(obj instanceof RegisteredListener)) {
				return false;
			}
			RegisteredListener other = (RegisteredListener) obj;
			if (listener == null) {
				if (other.listener != null) {
					return false;
				}
			} else if (!listener.equals(other.listener)) {
				return false;
			}
			if (method == null) {
				if (other.method != null) {
					return false;
				}
			} else if (!method.equals(other.method)) {
				return false;
			}
			if (moduleContainer == null) {
				if (other.moduleContainer != null) {
					return false;
				}
			} else if (!moduleContainer.equals(other.moduleContainer)) {
				return false;
			}
			if (order != other.order) {
				return false;
			}
			if (runWhenCancelled != other.runWhenCancelled) {
				return false;
			}
			return true;
		}
		
		
	}
}
