/*
 * Copyright (c) 2013-2022 Chris Newland.
 * Licensed under https://github.com/chriswhocodes/jitwatch/blob/master/LICENSE-BSD
 * Instructions: https://github.com/chriswhocodes/jitwatch/wiki
 */
package com.chrisnewland.jitwatch.process.compiler;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import com.chrisnewland.jitwatch.logger.ILogListener;
import com.chrisnewland.jitwatch.process.IExternalProcess;

public interface ICompiler extends IExternalProcess
{
	public boolean compile(List<File> sourceFiles, List<String> classpathEntries, List<String> vmOptions, File outputDir, Map<String, String> environment, ILogListener logListener) throws IOException;
}