package net.kaikk.msm.util;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipFile;

/**
 * Represents a jar file.
 * 
 * @author Kai
 *
 */
public class JarFile implements Closeable, AutoCloseable {
	final ZipFile jar;
	
	public JarFile(File moduleJarFile) throws IOException {
		jar = new ZipFile(moduleJarFile);
	}
	
	public InputStream getResourceAsStream(String resourceName) throws IOException {
		return jar.getInputStream(jar.getEntry(resourceName));
	}
	
	@Override
	public void close() throws IOException {
		jar.close();
	}
}
