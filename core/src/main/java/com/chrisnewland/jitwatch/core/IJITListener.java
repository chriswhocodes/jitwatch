/*
 * Copyright (c) 2013-2017 Chris Newland.
 * Licensed under https://github.com/chriswhocodes/jitwatch/blob/master/LICENSE-BSD
 * Instructions: https://github.com/chriswhocodes/jitwatch/wiki
 */
package com.chrisnewland.jitwatch.core;

import com.chrisnewland.jitwatch.logger.ILogListener;
import com.chrisnewland.jitwatch.model.JITEvent;

public interface IJITListener extends ILogListener
{
	void handleJITEvent(JITEvent event);
	void handleReadStart();
	void handleReadComplete();
}