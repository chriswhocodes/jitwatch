/*
 * Copyright (c) 2013-2022 Chris Newland.
 * Licensed under https://github.com/chriswhocodes/jitwatch/blob/master/LICENSE-BSD
 * Instructions: https://github.com/chriswhocodes/jitwatch/wiki
 */
package com.chrisnewland.jitwatch.process.compiler;

import static com.chrisnewland.jitwatch.core.JITWatchConstants.S_EMPTY;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import com.chrisnewland.jitwatch.logger.ILogListener;
import com.chrisnewland.jitwatch.process.AbstractProcess;

public class CompilerJRuby extends AbstractProcess implements ICompiler
{
	// TODO this is broken. Not sure if possible with ProcessBuilder
	// http://blog.headius.com/2013/06/the-pain-of-broken-subprocess.html

	private Path compilerPath;

	private final String COMPILER_NAME = "jrubyc" + (isWindows() ? ".bat" : S_EMPTY);

	public CompilerJRuby(String languageHomeDir) throws FileNotFoundException
	{
		super();

		compilerPath = Paths.get(languageHomeDir, "bin", COMPILER_NAME);

		if (!compilerPath.toFile().exists())
		{
			throw new FileNotFoundException("Could not find " + COMPILER_NAME);
		}

		compilerPath = compilerPath.normalize();
	}

	@Override
	public boolean compile(List<File> sourceFiles, List<String> classpathEntries, List<String> vmOptions, File outputDir, Map<String, String> environment, ILogListener logListener)
			throws IOException
	{
		List<String> commands = new ArrayList<>();

		String outputDirPath = outputDir.getAbsolutePath().toString();

		Set<String> uniqueCPSet = new HashSet<>(classpathEntries);
		uniqueCPSet.add(outputDirPath);

		commands.add(compilerPath.toString());

		// List<String> compileOptions = Arrays.asList(new String[] { "-g",
		// "-d", outputDirPath });
		//
		// commands.addAll(compileOptions);
		//
		commands.addAll(vmOptions);

		if (classpathEntries.size() > 0)
		{
			commands.add("-cp");

			commands.add(makeClassPath(classpathEntries));
		}

		for (File sourceFile : sourceFiles)
		{
			commands.add(sourceFile.getAbsolutePath());
		}

		return runCommands(commands, environment, logListener);
	}
}