/*
 * Copyright (c) 2013-2017 Chris Newland.
 * Licensed under https://github.com/chriswhocodes/jitwatch/blob/master/LICENSE-BSD
 * Instructions: https://github.com/chriswhocodes/jitwatch/wiki
 */
package com.chrisnewland.jitwatch.ui.triview.bytecode;

import com.chrisnewland.jitwatch.model.bytecode.BytecodeInstruction;
import com.chrisnewland.jitwatch.ui.triview.InstructionLabel;

public class BytecodeLabel extends InstructionLabel
{
	private BytecodeInstruction instruction;
	
	public BytecodeLabel(BytecodeInstruction instr, int maxOffset, int line)
	{
		super(instr.toString(maxOffset, line));
		instruction = instr;
	}
		
	public BytecodeInstruction getInstruction()
	{
		return instruction;
	}
}