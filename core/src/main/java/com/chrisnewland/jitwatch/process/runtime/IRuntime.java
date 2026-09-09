/*
 * Copyright (c) 2013-2022 Chris Newland.
 * Licensed under https://github.com/chriswhocodes/jitwatch/blob/master/LICENSE-BSD
 * Instructions: https://github.com/chriswhocodes/jitwatch/wiki
 */
package com.chrisnewland.jitwatch.process.runtime;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import com.chrisnewland.jitwatch.logger.ILogListener;
import com.chrisnewland.jitwatch.process.IExternalProcess;

public interface IRuntime extends IExternalProcess
{
	public boolean execute(Path workingDir, String className, List<String> classpathEntries, List<String> vmOptions, Map<String, String> environment, ILogListener logListener);
	
	public String getClassToExecute(File fileToRun);
	
	public String getClassForTriView(File fileToRun);
}