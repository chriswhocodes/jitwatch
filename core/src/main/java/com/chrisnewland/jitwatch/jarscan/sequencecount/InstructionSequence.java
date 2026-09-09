/*
 * Copyright (c) 2013-2016 Chris Newland.
 * Licensed under https://github.com/chriswhocodes/jitwatch/blob/master/LICENSE-BSD
 * Instructions: https://github.com/chriswhocodes/jitwatch/wiki
 */
package com.chrisnewland.jitwatch.jarscan.sequencecount;

import java.util.ArrayList;
import java.util.List;

import com.chrisnewland.jitwatch.model.bytecode.Opcode;
import static com.chrisnewland.jitwatch.core.JITWatchConstants.S_COMMA;

public class InstructionSequence implements Comparable<InstructionSequence>
{
	private List<Opcode> sequence = new ArrayList<>();

	public InstructionSequence(List<Opcode> opcodeList)
	{
		sequence.addAll(opcodeList);
	}

	@Override
	public String toString()
	{
		StringBuilder builder = new StringBuilder();

		for (Opcode opcode : sequence)
		{
			builder.append(opcode.getMnemonic()).append(S_COMMA);
		}

		builder.delete(builder.length() - 1, builder.length());

		return builder.toString();
	}

	public Opcode getOpcodeAtIndex(int index)
	{
		return sequence.get(index);
	}
	
	@Override
	public int hashCode()
	{
		return toString().hashCode();
	}

	@Override
	public boolean equals(Object obj)
	{
		return toString().equals(obj.toString());
	}

	@Override
	public int compareTo(InstructionSequence o)
	{
		return toString().compareTo(o.toString());
	}
}