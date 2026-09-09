/*
 * Copyright (c) 2013-2015 Chris Newland.
 * Licensed under https://github.com/chriswhocodes/jitwatch/blob/master/LICENSE-BSD
 * Instructions: https://github.com/chriswhocodes/jitwatch/wiki
 */
package com.chrisnewland.jitwatch.model;

import com.chrisnewland.jitwatch.model.bytecode.Opcode;

public interface IParseDictionary
{
	void putType(String id, Tag type);

	void putKlass(String id, Tag klass);

	void putMethod(String id, Tag method);
	
	void putBCIOpcode(String methodID, int bci, Opcode opcode);

	Tag getType(String id);

	Tag getKlass(String id);

	Tag getMethod(String id);
	
	String getParseMethod();
	
	BCIOpcodeMap getBCIOpcodeMap(String methodID);
}