/*
 * Copyright (c) 2013-2016 Chris Newland.
 * Licensed under https://github.com/chriswhocodes/jitwatch/blob/master/LICENSE-BSD
 * Instructions: https://github.com/chriswhocodes/jitwatch/wiki
 */
package com.chrisnewland.jitwatch.logger;

public class NullLogListener implements ILogListener
{

	@Override
	public void handleLogEntry(String entry)
	{		
	}

	@Override
	public void handleErrorEntry(String entry)
	{		
	}
}