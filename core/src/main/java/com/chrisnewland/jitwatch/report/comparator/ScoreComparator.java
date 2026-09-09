/*
 * Copyright (c) 2016 Chris Newland.
 * Licensed under https://github.com/chriswhocodes/jitwatch/blob/master/LICENSE-BSD
 * Instructions: https://github.com/chriswhocodes/jitwatch/wiki
 */
package com.chrisnewland.jitwatch.report.comparator;

import java.util.Comparator;

import com.chrisnewland.jitwatch.report.Report;

public class ScoreComparator implements Comparator<Report>
{

	@Override
	public int compare(Report o1, Report o2)
	{
		return Integer.compare(o2.getScore(), o1.getScore());
	}
}