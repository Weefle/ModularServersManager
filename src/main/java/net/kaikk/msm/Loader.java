package net.kaikk.msm;

import java.io.File;

import net.kaikk.msm.util.JarFilenameFilter;
import net.kaikk.msm.util.Utils;

public class Loader {
	public static void main(String[] args) throws Throwable {
		// load libraries
		System.out.println("Loading libraries...");
		{
			final File libraries = new File("libraries");
			libraries.mkdirs();
			
			Utils.extendSystemClassLoaderWithJar(libraries.listFiles(JarFilenameFilter.instance));
		
			// libraries dependency check
			Utils.checkDependencies("com.typesafe.config.Config", "jline.Terminal", "com.google.common.collect.Multimap", "com.impetus.annovention.Discoverer", "javassist.Loader", "org.apache.log4j.Logger");
		}

		System.out.println("ModularServersManager v."+ModularServersManager.VERSION+" by KaiNoMood");
		
		new ModularServersManager();
	}
}
