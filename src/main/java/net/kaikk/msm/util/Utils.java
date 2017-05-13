package net.kaikk.msm.util;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

public class Utils {
	public static final String[] EMPTY_STRING_ARRAY = new String[]{};
	
	public static void extendSystemClassLoaderWithJar(final File...files) {
		try {
			final URLClassLoader sysClassLoader = (URLClassLoader) ClassLoader.getSystemClassLoader();
			final Method method = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
			method.setAccessible(true);
			for (final File file : files) {
				final URL url = file.toURI().toURL();
				if (!arrayContains(url, sysClassLoader.getURLs())) {
					method.invoke(sysClassLoader, url);
				}
				
			}
		} catch (Throwable e) {
			new RuntimeException(e);
		}
	}
	
	
	public static void extendSystemClassLoaderWithJar(final URL...urls) {
		try {
			final URLClassLoader sysClassLoader = (URLClassLoader) ClassLoader.getSystemClassLoader();
			final Method method = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
			method.setAccessible(true);
			for (final URL url : urls) {
				if (!arrayContains(url, sysClassLoader.getURLs())) {
					method.invoke(sysClassLoader, url);
				}
			}
		} catch (Throwable e) {
			new RuntimeException(e);
		}
	}
	
	public static boolean arrayContains(Object needle, Object[] haystack) {
		for (final Object o : haystack) {
			if (needle.equals(o)) {
				return true;
			}
		}
		return false;
	}
	
	public static void checkDependencies(String... classNames) {
		final List<String> missingDependencies = new ArrayList<>();
		for (String className : classNames) {
			try {
				Class.forName(className);
			} catch (ClassNotFoundException e) {
				missingDependencies.add(className);
			}
		}
		
		if (!missingDependencies.isEmpty()) {
			System.err.println("Missing libraries dependency:\n- "+String.join("\n- ", missingDependencies));
			System.exit(1);
		}
	}
	
	public static String stackTraceToString(Throwable t) {
		final StringWriter sw = new StringWriter();
		try {
			final PrintWriter pw = new PrintWriter(sw);
			t.printStackTrace(pw);
		} finally {
			try {
				sw.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return sw.toString();
	}
	
	/**
	 * Makes a string from the array starting from the specified position. A space will be used as separator
	 * 
	 * @param arrayString the array
	 * @param position the initial position
	 * @return a merged string
	 */
	public static String mergeStringArrayFromIndex(String[] arrayString, int position) {
		return mergeStringArrayFromIndex(arrayString, ' ', position);
	}
	
	/**
	 * Makes a string from the array starting from the specified position. The separator will be used between the merged array values.
	 * 
	 * @param arrayString the array
	 * @param separator the separator
	 * @param position the initial position
	 * @return a merged string
	 */
	public static String mergeStringArrayFromIndex(String[] arrayString, char separator, int position) {
		final StringBuilder sb = new StringBuilder();

		for(;position<arrayString.length;position++){
			sb.append(arrayString[position]);
			sb.append(separator);
		}

		if (sb.length()!=0) {
			sb.deleteCharAt(sb.length()-1);
		}
		return sb.toString();
	}
}
