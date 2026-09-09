/*
 * Copyright (c) 2013-2022 Chris Newland.
 * Licensed under https://github.com/chriswhocodes/jitwatch/blob/master/LICENSE-BSD
 * Instructions: https://github.com/chriswhocodes/jitwatch/wiki
 */
package com.chrisnewland.jitwatch.process.compiler;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.chrisnewland.jitwatch.logger.ILogListener;
import com.chrisnewland.jitwatch.process.AbstractProcess;

public class CompilerJava extends AbstractProcess implements ICompiler
{
	private Path compilerPath;

	private final String COMPILER_NAME = "javac" + getExecutableSuffix();

	public CompilerJava(String languageHomeDir) throws FileNotFoundException
	{
		super();

		compilerPath = Paths.get(languageHomeDir, "..", "bin", COMPILER_NAME);

		if (!compilerPath.toFile().exists())
		{
			compilerPath = Paths.get(languageHomeDir, "bin", COMPILER_NAME);

			if (!compilerPath.toFile().exists())
			{
				throw new FileNotFoundException("Could not find " + COMPILER_NAME);
			}
		}

		compilerPath = compilerPath.normalize();
	}

	@Override
	public boolean compile(List<File> sourceFiles, List<String> classpathEntries, List<String> vmOptions, File outputDir, Map<String, String> environment, ILogListener logListener)
			throws IOException
	{
		List<String> commands = new ArrayList<>();

		String outputDirPath = outputDir.getAbsolutePath().toString();

		commands.add(compilerPath.toString());

		List<String> compileOptions = Arrays.asList(new String[] { "-g", "-d", outputDirPath });
		commands.addAll(vmOptions);

		commands.addAll(compileOptions);

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