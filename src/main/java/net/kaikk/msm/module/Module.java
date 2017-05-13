package net.kaikk.msm.module;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Defines a module.
 * 
 * @author Kai
 *
 */
@Retention(RUNTIME)
@Target(TYPE)
public @interface Module {
	final static String[] annotationClassName = {Module.class.getName()};
	
	/**
	 * The module id. Use only alphanumeric characters. 
	 * 
	 * @return the module id
	 */
	String id();
	
	/**
	 * @return the module version
	 */
	String version() default "";
	
	
	/**
	 * This module dependencies. This module will be loaded after the specified modules.<br>
	 * Modules on this list are required for this plugin to be loaded.
	 * 
	 * @return a list of modules id
	 */
	String[] dependencies() default {};
	
	
	/**
	 * Load this module after the specified modules.<br>
	 * Modules on this list are optional.
	 * 
	 * @return a list of modules id
	 */
	String[] loadAfter() default {};
	
	/**
	 * Load this module before the specified modules.<br>
	 * Modules on this list are optional.
	 * 
	 * @return a list of modules id
	 */
	String[] loadBefore() default {};
}
