/*
 * Copyright (c) 2013-2016 Chris Newland.
 * Licensed under https://github.com/chriswhocodes/jitwatch/blob/master/LICENSE-BSD
 * Instructions: https://github.com/chriswhocodes/jitwatch/wiki
 */
package com.chrisnewland.jitwatch.jarscan.invokecount;

import java.util.List;
import com.chrisnewland.jitwatch.jarscan.IJarScanOperation;
import com.chrisnewland.jitwatch.model.bytecode.BytecodeInstruction;
import com.chrisnewland.jitwatch.model.bytecode.MemberBytecode;
import com.chrisnewland.jitwatch.model.bytecode.Opcode;
import com.chrisnewland.jitwatch.util.ParseUtil;

public class InvokeCountOperation implements IJarScanOperation
{
	private InvokeMethodCountMap opcodeInvokeCountMap;

	private int limitPerInvoke;
	
	public InvokeCountOperation(int limitPerInvoke)
	{
		opcodeInvokeCountMap = new InvokeMethodCountMap();
		
		this.limitPerInvoke = limitPerInvoke;
	}

	@Override
	public String getReport()
	{
		return opcodeInvokeCountMap.toString(limitPerInvoke);
	}
	
	private void count(String className, BytecodeInstruction instruction)
	{			
		String comment = instruction.getCommentWithMemberPrefixStripped();
		
		String methodSig = ParseUtil.bytecodeMethodCommentToReadableString(className, comment);
	
		opcodeInvokeCountMap.countInvocationOfMethod(instruction.getOpcode(), methodSig);
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
			case INVOKEDYNAMIC:
			case INVOKEINTERFACE:
			case INVOKESPECIAL:
			case INVOKESTATIC:
			case INVOKEVIRTUAL:
				count(className, instruction);
				break;
				
			default:
				break;
			}
		}

	}
}
