/*
 * Copyright (c) 2013-2016 Chris Newland.
 * Licensed under https://github.com/chriswhocodes/jitwatch/blob/master/LICENSE-BSD
 * Instructions: https://github.com/chriswhocodes/jitwatch/wiki
 */
package com.chrisnewland.jitwatch.jarscan.allocationcount;

import static com.chrisnewland.jitwatch.core.JITWatchConstants.S_DOUBLE_QUOTE;
import static com.chrisnewland.jitwatch.core.JITWatchConstants.S_EMPTY;
import static com.chrisnewland.jitwatch.core.JITWatchConstants.S_SLASH;
import static com.chrisnewland.jitwatch.core.JITWatchConstants.S_DOT;

import java.util.List;

import com.chrisnewland.jitwatch.jarscan.IJarScanOperation;
import com.chrisnewland.jitwatch.model.bytecode.BytecodeInstruction;
import com.chrisnewland.jitwatch.model.bytecode.IBytecodeParam;
import com.chrisnewland.jitwatch.model.bytecode.MemberBytecode;
import com.chrisnewland.jitwatch.model.bytecode.Opcode;

public class AllocationCountOperation implements IJarScanOperation
{
	private InstructionAllocCountMap opcodeAllocCountMap;

	private int limitPerAllocOpcode;

	public AllocationCountOperation(int limit)
	{
		opcodeAllocCountMap = new InstructionAllocCountMap();
		this.limitPerAllocOpcode = limit;
	}

	@Override
	public String getReport()
	{
		return opcodeAllocCountMap.toString(limitPerAllocOpcode);
	}

	private void count(Opcode opcode, String type)
	{
		opcodeAllocCountMap.count(opcode, type);
	}

	@Override
	public void processInstructions(String className, MemberBytecode memberBytecode)
	{
		List<BytecodeInstruction> instructions = memberBytecode.getInstructions();

		for (BytecodeInstruction instruction : instructions)
		{
			Opcode opcode = instruction.getOpcode();
			
			switch (opcode)
			{
			case NEWARRAY:
			{
				List<IBytecodeParam> params = instruction.getParameters();
				String type = params.get(0).toString();
				count(opcode, type);
			}
				break;

			case ANEWARRAY:
			case NEW:
			case MULTIANEWARRAY:
			{
				String comment = instruction.getComment();
				String type = comment.substring("// class ".length(), comment.length());
				type = type.replace(S_DOUBLE_QUOTE, S_EMPTY).replace(S_SLASH, S_DOT);
				count(opcode, type);
			}
				break;

			default:
				break;
			}
		}
	}
}