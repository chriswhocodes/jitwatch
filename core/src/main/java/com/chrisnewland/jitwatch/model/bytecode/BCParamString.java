/*
 * Copyright (c) 2013-2015 Chris Newland.
 * Licensed under https://github.com/chriswhocodes/jitwatch/blob/master/LICENSE-BSD
 * Instructions: https://github.com/chriswhocodes/jitwatch/wiki
 */
package com.chrisnewland.jitwatch.model.bytecode;

public class BCParamString implements IBytecodeParam
{
	private String value;

	public BCParamString(String value)
	{
		this.value = value;
	}

	@Override
	public String toString()
	{
		return value;
	}
	
	@Override
	public String getValue()
	{
		return value;
	}
}