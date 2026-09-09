/*
 * Copyright (c) 2018 Chris Newland.
 * Licensed under https://github.com/chriswhocodes/jitwatch/blob/master/LICENSE-BSD
 * Instructions: https://github.com/chriswhocodes/jitwatch/wiki
 */
package com.chrisnewland.jitwatch.parser;

import com.chrisnewland.jitwatch.core.IJITListener;
import com.chrisnewland.jitwatch.parser.hotspot.HotSpotLogParser;
import com.chrisnewland.jitwatch.parser.j9.J9LogParser;
import com.chrisnewland.jitwatch.parser.zing.ZingLogParser;

public class ParserFactory
{
	private ParserFactory()
	{
	}

	public static ILogParser getParser(ParserType parserType, IJITListener jitListener)
	{
		switch (parserType)
		{
		case HOTSPOT:
			return new HotSpotLogParser(jitListener);
		case J9:
			return new J9LogParser(jitListener);
		case ZING:
			return new ZingLogParser(jitListener);
		default:
			throw new RuntimeException("Unknown parser " + parserType);
		}
	}
}