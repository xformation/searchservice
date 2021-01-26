package com.synectiks.search.utils;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class to find all Classes from a package.
 * @author Rajesh Upadhyay
 */
public class ClassFinder {

	private static final Logger logger = LoggerFactory.getLogger(ClassFinder.class);

	private static final char PKG_SEPARATOR = '.';
	private static final char DIR_SEPARATOR = '/';
	private static final String CLS_SUFFIX = ".class";
	private static final String ERROR = "Unable to get resources from path '%s'. Are you sure the package '%s' exists?";

	public static List<Class<?>> find(String pkg, Class<?> superCls) {
		List<Class<?>> classes = new ArrayList<Class<?>>();
		List<Class<?>> list = find(pkg);
		for (Class<?> cls : list) {
			//System.out.println("=> " + cls.getCanonicalName());
			if (superCls.isAssignableFrom(cls)) {
				classes.add(cls);
			}
		}
		return classes;
	}

	public static List<Class<?>> find(String scannedPackage) {
		String scannedPath = scannedPackage.replace(PKG_SEPARATOR, DIR_SEPARATOR);
		URL scannedUrl = Thread.currentThread().getContextClassLoader()
				.getResource(scannedPath);
		logger.info("scannedUrl: " + scannedUrl);
		if (scannedUrl == null) {
			throw new IllegalArgumentException(
					String.format(ERROR, scannedPath, scannedPackage));
		}
		logger.info("scannedUrl files: " + scannedUrl.getFile());
		File scannedDir = new File(scannedUrl.getFile());
		List<Class<?>> classes = new ArrayList<Class<?>>();
		if (scannedDir != null) {
			for (File file : scannedDir.listFiles()) {
				classes.addAll(find(file, scannedPackage));
			}
		}
		return classes;
	}

	private static List<Class<?>> find(File file, String scannedPackage) {
		List<Class<?>> classes = new ArrayList<Class<?>>();
		String resource = scannedPackage + PKG_SEPARATOR + file.getName();
		if (file.isDirectory()) {
			for (File child : file.listFiles()) {
				classes.addAll(find(child, resource));
			}
		} else if (resource.endsWith(CLS_SUFFIX)) {
			int endIndex = resource.length() - CLS_SUFFIX.length();
			String className = resource.substring(0, endIndex);
			try {
				classes.add(Class.forName(className));
			} catch (ClassNotFoundException ignore) {
			}
		}
		return classes;
	}

}
