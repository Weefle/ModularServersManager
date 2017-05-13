package net.kaikk.msm.event.module;

import net.kaikk.msm.ModularServersManager;
import net.kaikk.msm.event.Event;
import net.kaikk.msm.module.ModulesManager;

/**
 * This event is exclusively called for a module being loaded.<p>
 * This is usually used on the the module main instance to initialize the module. Example:
 * <pre>
 * &#64;Module(id="ExampleModule", version = "1.0")
 * public class ExampleModule {
 *   &#64;EventHandler
 *   public void onInit(InitializationEvent event, ModuleContainer container) {
 *     // initialize your module here
 *   }
 * }
 * </pre>
 *
 */
public class InitializationEvent implements Event {
	private final ModularServersManager instance;
	private final ModulesManager manager;
	
	public InitializationEvent(ModularServersManager instance, ModulesManager manager) {
		this.instance = instance;
		this.manager = manager;
	}
	
	public ModularServersManager getInstance() {
		return instance;
	}
	public ModulesManager getManager() {
		return manager;
	}
}
