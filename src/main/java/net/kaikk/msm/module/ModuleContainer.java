package net.kaikk.msm.module;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import org.apache.log4j.Logger;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;
import com.typesafe.config.ConfigFactory;

import net.kaikk.msm.util.JarFile;

/**
 * A container for a module. This contains useful methods for initializing modules. This is also passed with the InitializationEvent.
 * 
 * @author Kai
 *
 */
public class ModuleContainer implements Comparable<ModuleContainer> {
	private Object moduleInstance;

	public ModuleContainer(Object moduleInstance) {
		ModulesManager.assertModule(moduleInstance);
		this.moduleInstance = moduleInstance;
	}

	/**
	 * @return the module id
	 */
	public String id() {
		return this.getModuleAnnotation().id();
	}

	/**
	 * @return the module version
	 */
	public String version() {
		return this.getModuleAnnotation().version();
	}

	/**
	 * This module has been loaded before the specified list of modules.
	 * 
	 * @return a list modules id
	 */
	public String[] loadBefore() {
		return this.getModuleAnnotation().loadBefore();
	}

	/**
	 * This module has been loaded after the specified list of modules.
	 * 
	 * @return a list modules id
	 */
	public String[] loadAfter() {
		return this.getModuleAnnotation().loadAfter();
	}

	/**
	 * This module has been loaded after the specified list of required modules.
	 * 
	 * @return a list modules id
	 */
	public String[] dependencies() {
		return this.getModuleAnnotation().dependencies();
	}

	/**
	 * @return the module instance
	 */
	public Object getModuleInstance() {
		return moduleInstance;
	}
	
	/**
	 * @return the module default logger
	 */
	public Logger getLogger() {
		return Logger.getLogger(moduleInstance.getClass().getSimpleName());
	}
	
	
	/**
	 * Copies the specified resource from the module jar to the specified destination.
	 * 
	 * <p> The {@link StandardCopyOption} enumeration type defines the
	 * <i>standard</i> options.
	 * 
	 * @see Files#copy(InputStream, Path, CopyOption...)
	 * 
	 * @param resourceName the resource name
	 * @param destination the destination of the resource
	 * @param copyOptions one or more copy options
	 * @throws IOException
	 */
	public void copyResourceFromJar(String resourceName, Path destination, CopyOption... copyOptions) throws IOException {
		try (JarFile jar = this.getModuleJar()) {
			final InputStream is = jar.getResourceAsStream(resourceName);
			Files.copy(is, destination, copyOptions);
		}
	}
	
	/**
	 * Returns this module's config file.<br>
	 * This is a HOCON formatted config file named "config.conf" on the module's default folder.<br>
	 * If the config file does not exist on the module's folder, the default "config.conf" file from the module jar will be copied to the module folder.
	 * 
	 * @return the config for this module
	 * @throws IOException if the default config file could not be copied from the module's jar
	 * @throws ConfigException if the config file could not be parsed
	 */
	public Config getConfig() throws IOException, ConfigException {
		return this.getConfig("config.conf");
	}
	
	/**
	 * Returns the specified HOCON formatted config file.<br>
	 * This will search for a HOCON formatted config file on the module default folder.<br>
	 * If the config file does not exist, a file with the same name will be copied from the module jar to the default module folder.
	 * 
	 * @param fileName the config file name
	 * @return the specified config
	 * @throws IOException if the default config file could not be copied from the module's jar
	 * @throws ConfigException if the config file could not be parsed
	 */
	public Config getConfig(final String fileName) throws IOException, ConfigException {
		final File file = new File(this.getModuleFolder(), fileName);
		this.getModuleFolder().mkdirs();
		if (!file.exists()) {
			// copy default
			this.copyResourceFromJar(fileName, file.toPath());
		}
		return ConfigFactory.parseFile(file);
	}
	
	/**
	 * Gets the module jar. 
	 * 
	 * @return a ModuleJar object that represent the Module jar file
	 * @throws IOException
	 */
	public JarFile getModuleJar() throws IOException {
		try {
			return new JarFile(new File(this.getModuleJarLocation().toURI()));
		} catch (URISyntaxException e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * @return the module default folder
	 */
	public File getModuleFolder() {
		return new File("modules"+File.separator+this.id());
	}
	
	/**
	 * @return the module jar
	 */
	public URL getModuleJarLocation() {
		return moduleInstance.getClass().getProtectionDomain().getCodeSource().getLocation();
	}
	
	private Module getModuleAnnotation() {
		return moduleInstance.getClass().getAnnotation(Module.class);
	}
	
	@Override
	public final int compareTo(final ModuleContainer o) {
		for (final String s : this.dependencies()) {
			if (s.equals(o.id())) {
				return 1;
			}
		}
		
		for (final String s : o.dependencies()) {
			if (s.equals(this.id())) {
				return -1;
			}
		}
		
		for (final String s : this.loadAfter()) {
			if (s.equals(o.id())) {
				return 1;
			}
		}
		
		for (final String s : o.loadAfter()) {
			if (s.equals(this.id())) {
				return -1;
			}
		}
		
		for (final String s : this.loadBefore()) {
			if (s.equals(o.id())) {
				return -1;
			}
		}
		
		for (final String s : o.loadBefore()) {
			if (s.equals(this.id())) {
				return 1;
			}
		}
		
		return this.id().compareTo(o.id());
	}

	@Override
	public int hashCode() {
		return this.id().hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof ModuleContainer)) {
			return false;
		}
		return this.id().equalsIgnoreCase(((ModuleContainer)obj).id());
	}
	
	
}
