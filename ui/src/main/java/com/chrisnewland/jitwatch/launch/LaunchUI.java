/*
 * Copyright (c) 2013-2015 Chris Newland.
 * Licensed under https://github.com/chriswhocodes/jitwatch/blob/master/LICENSE-BSD
 * Instructions: https://github.com/chriswhocodes/jitwatch/wiki
 */
package com.chrisnewland.jitwatch.launch;

import com.chrisnewland.jitwatch.ui.main.JITWatchUI;

public final class LaunchUI
{
	private LaunchUI()
	{
	}

	public static void main(String[] args)
	{
		new JITWatchUI(args);
	}
}