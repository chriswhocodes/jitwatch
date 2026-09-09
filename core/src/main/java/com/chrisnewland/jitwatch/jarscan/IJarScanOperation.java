/*
 * Copyright (c) 2013-2016 Chris Newland.
 * Licensed under https://github.com/chriswhocodes/jitwatch/blob/master/LICENSE-BSD
 * Instructions: https://github.com/chriswhocodes/jitwatch/wiki
 */
package com.chrisnewland.jitwatch.jarscan;

import com.chrisnewland.jitwatch.model.bytecode.MemberBytecode;

public interface IJarScanOperation
{
	void processInstructions(String className, MemberBytecode memberBytecode);
		
	String getReport();
}