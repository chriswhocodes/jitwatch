/*
 * Copyright (c) 2013-2016 Chris Newland.
 * Licensed under https://github.com/chriswhocodes/jitwatch/blob/master/LICENSE-BSD
 * Instructions: https://github.com/chriswhocodes/jitwatch/wiki
 */
package com.chrisnewland.jitwatch.ui.main;

import com.chrisnewland.jitwatch.compilation.CompilationUtil;
import com.chrisnewland.jitwatch.model.Compilation;
import com.chrisnewland.jitwatch.model.Task;
import com.chrisnewland.jitwatch.util.StringUtil;

public class CompilationTableRow
{
	private final Compilation compilation;

	public CompilationTableRow(Compilation compilation)
	{
		this.compilation = compilation;
	}

	public int getIndex()
	{
		return compilation.getIndex();
	}

	public String getStampQueued()
	{
		return getTimestampOrNA(compilation.getStampTaskQueued());
	}

	public String getStampCompilationStart()
	{
		return getTimestampOrNA(compilation.getStampTaskCompilationStart());
	}
	
	public String getStampNMethodEmitted()
	{
		return getTimestampOrNA(compilation.getStampNMethodEmitted());
	}
	
	private String getTimestampOrNA(long stamp)
	{
		String result;

		if (stamp == 0)
		{
			result = "NA";
		}
		else
		{
			result = StringUtil.formatTimestamp(stamp, true);
		}

		return result;
	}

	public String getNative()
	{
		int nativeSize = compilation.getNativeSize();

		String result;

		if (nativeSize == 0)
		{
			result = "NA";
		}
		else
		{
			result = Integer.toString(nativeSize);
		}

		return result;
	}

	public String getCompiler()
	{
		String result = compilation.getCompiler();

		if (result == null)
		{
			result = "NA";

			Task task = compilation.getTagTask();

			if (task != null && CompilationUtil.isStaleTask(task))
			{
				result = "Stale task";
			}
		}

		return result;
	}

	public String getLevel()
	{
		String result = null;

		int level = compilation.getLevel();

		if (level == -1)
		{
			result = "NA";
		}
		else
		{
			result = "Level " + level;
		}

		return result;
	}

	public String getLevelDescription(String level)
	{
		switch (level)
		{
		case "0":
			return "Interpreter";
		case "1":
			return "C1 full optimization (no profiling)";
		case "2":
			return "C1 invocation and backedge counters";
		case "3":
			return "C1 full profiling (invocation and backedge counters + MDO)";
		case "4":
			return "Server compiler";
		default:
			return "Unknown compiler level";
		}
	}
}