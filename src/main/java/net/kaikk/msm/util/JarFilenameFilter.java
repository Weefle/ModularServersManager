package net.kaikk.msm.util;

import java.io.File;
import java.io.FilenameFilter;

/**
 * Filters jar files by the ".jar" extension.
 * 
 * @author Kai
 *
 */
public class JarFilenameFilter implements FilenameFilter {
	public static final JarFilenameFilter instance = new JarFilenameFilter();
	
	@Override
	public boolean accept(File dir, String name) {
		return name.toLowerCase().endsWith(".jar");
	}
}

