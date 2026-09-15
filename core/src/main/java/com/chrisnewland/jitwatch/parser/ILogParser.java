/*
 * Copyright (c) 2013-2015 Chris Newland.
 * Licensed under https://github.com/chriswhocodes/jitwatch/blob/master/LICENSE-BSD
 * Instructions: https://github.com/chriswhocodes/jitwatch/wiki
 */
package com.chrisnewland.jitwatch.parser;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;

import com.chrisnewland.jitwatch.core.JITWatchConfig;
import com.chrisnewland.jitwatch.model.JITDataModel;
import com.chrisnewland.jitwatch.model.ParsedClasspath;
import com.chrisnewland.jitwatch.model.SplitLog;

public interface ILogParser
{
	void setConfig(JITWatchConfig config);

	default void processLogFile(File logFile, ILogParseErrorListener listener) throws IOException{
		processLogFile(new FileReader(logFile), listener);
	}

	void processLogFile(Reader logFileReader, ILogParseErrorListener listener) throws IOException;

	SplitLog getSplitLog();

	void stopParsing();

	ParsedClasspath getParsedClasspath();

	JITDataModel getModel();

	JITWatchConfig getConfig();

	void reset();

	boolean hasParseError();
	
	default boolean isHiddenClassWarningNeeded()
	{
		return false;
	}

	default String getHiddenClassWarningMessage() {
		return "";
	}

	String getVMCommand();

	void discardParsedLogs();
}