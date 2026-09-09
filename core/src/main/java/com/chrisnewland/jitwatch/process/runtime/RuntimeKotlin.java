/*
 * Copyright (c) 2013-2022 Chris Newland.
 * Licensed under https://github.com/chriswhocodes/jitwatch/blob/master/LICENSE-BSD
 * Instructions: https://github.com/chriswhocodes/jitwatch/wiki
 */
package com.chrisnewland.jitwatch.process.runtime;

import static com.chrisnewland.jitwatch.core.JITWatchConstants.S_EMPTY;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import com.chrisnewland.jitwatch.loader.ResourceLoader;
import com.chrisnewland.jitwatch.logger.ILogListener;
import com.chrisnewland.jitwatch.process.AbstractProcess;
import com.chrisnewland.jitwatch.process.compiler.CompilerKotlin;

public class RuntimeKotlin extends AbstractProcess implements IRuntime
{
	private Path runtimePath;

	//TODO how did this used to work?
	private final Path pathToRuntimeJar = Paths.get(CompilerKotlin.KOTLIN_EXECUTABLE_JAR);

	private final String RUNTIME_NAME = "java" + getExecutableSuffix();

	public RuntimeKotlin(String languageHomeDir) throws FileNotFoundException
	{
		super();

		// Kotlin is executed on the current running Java VM
		runtimePath = Paths.get(System.getProperty("java.home"), "bin", RUNTIME_NAME);

		if (!runtimePath.toFile().exists())
		{
			throw new FileNotFoundException("Could not find " + RUNTIME_NAME);
		}

		runtimePath = runtimePath.normalize();
	}

	@Override
	public boolean execute(Path workingDir, String className, List<String> classpathEntries, List<String> vmOptions, Map<String, String> environment, ILogListener logListener)
	{
		List<String> commands = new ArrayList<>();

		commands.add(runtimePath.toString());

		if (vmOptions.size() > 0)
		{
			commands.addAll(vmOptions);
		}

		if (classpathEntries.size() > 0)
		{
			commands.add("-cp");
			commands.add(makeClassPath(classpathEntries));
		}

		commands.add("-jar");
		commands.add(pathToRuntimeJar.toString());

		return runCommands(commands, workingDir, environment,logListener);
	}

	@Override
	public String getClassToExecute(File fileToRun)
	{
		// Main class is in the jar manifest
		return S_EMPTY;
	}

	@Override
	public String getClassForTriView(File fileToRun)
	{
		Properties manifest = ResourceLoader.readManifestFromZip(pathToRuntimeJar.toFile());

		String mainClass = manifest.getProperty("Main-Class");

		return mainClass;
	}
}