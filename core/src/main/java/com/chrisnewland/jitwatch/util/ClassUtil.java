/*
 * Copyright (c) 2013-2021 Chris Newland.
 * Licensed under https://github.com/chriswhocodes/jitwatch/blob/master/LICENSE-BSD
 * Instructions: https://github.com/chriswhocodes/jitwatch/wiki
 */
package com.chrisnewland.jitwatch.util;

import static com.chrisnewland.jitwatch.core.JITWatchConstants.*;
import com.chrisnewland.jitwatch.loader.DisposableURLClassLoader;
import com.chrisnewland.freelogj.Logger;
import com.chrisnewland.freelogj.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class ClassUtil
{
	private static DisposableURLClassLoader disposableClassLoader = new DisposableURLClassLoader(new ArrayList<URL>());

	private static final Logger logger = LoggerFactory.getLogger(ClassUtil.class);

	public static void main(String[] args) throws ClassNotFoundException
	{
		ClassUtil.loadClassWithoutInitialising(args[0]);
	}
	
	private ClassUtil()
	{
	}

	public static void initialise(final List<URL> urls)
	{
		if (DEBUG_LOGGING_CLASSPATH)
		{
			for (URL url : urls)
			{
				logger.debug("Adding classpath to DisposableURLClassLoader {}", url);
			}
		}

		disposableClassLoader = new DisposableURLClassLoader(urls);
	}

	public static Class<?> loadClassWithoutInitialising(String fqClassName) throws ClassNotFoundException
	{
		if (DEBUG_LOGGING_CLASSPATH)
		{
			logger.debug("loadClassWithoutInitialising '{}'", fqClassName);
		}

		try
		{
			return Class.forName(fqClassName, false, disposableClassLoader);
		}
		catch (SecurityException se)
		{
			// URLClassLoader (likely) found a class file for a java.* name and 
			// tried to defineClass, which the JVM always rejects for java.* 
			// packages.  Retain functioning work flow by throwing so downstream
			// can decide how to handle.
			throw new ClassNotFoundException(fqClassName, se);
		}
	}

	public static Class<?> loadClassWithoutInitialising(String fqClassName, ClassLoader classLoader) throws ClassNotFoundException
	{
		return Class.forName(fqClassName, false, classLoader);
	}
	
	public static ClassLoader getDisposableClassLoader()
	{
		return disposableClassLoader;
	}

	public static List<String> getCurrentClasspathElements()
	{
		String classPath = System.getProperty("java.class.path");

		String[] parts = classPath.split(File.pathSeparator);

		return Arrays.asList(parts);
	}

	public static void clear()
	{
		if (disposableClassLoader != null)
		{
			try
			{
				disposableClassLoader.close();
			}
			catch (IOException e)
			{
				logger.warn("Could not close the DisposableURLClassLoader", e);
			}
		}
		disposableClassLoader = null;
	}
}
