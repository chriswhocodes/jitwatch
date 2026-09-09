/*
 * Copyright (c) 2013-2016 Chris Newland.
 * Licensed under https://github.com/chriswhocodes/jitwatch/blob/master/LICENSE-BSD
 * Instructions: https://github.com/chriswhocodes/jitwatch/wiki
 */
package com.chrisnewland.jitwatch.test;

import static com.chrisnewland.jitwatch.core.JITWatchConstants.DEBUG_MEMBER_CREATION;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;

import com.chrisnewland.jitwatch.model.AbstractMetaMember;
import com.chrisnewland.jitwatch.model.MetaClass;
import com.chrisnewland.jitwatch.model.bytecode.BytecodeInstruction;

public class HelperMetaMethod extends AbstractMetaMember
{
	public HelperMetaMethod(String methodName, MetaClass metaClass, Class<?>[] params, Class<?> returnType)
			throws NoSuchMethodException, SecurityException
	{
		super(methodName);

		Method dummyMethodObject = java.lang.String.class.getDeclaredMethod("length", new Class<?>[0]);

		this.metaClass = metaClass;

		this.returnType = returnType;
		this.paramTypes = Arrays.asList(params);

		// Can include non-method modifiers such as volatile so AND with
		// acceptable values
		this.modifier = dummyMethodObject.getModifiers() & Modifier.methodModifiers();

		this.isVarArgs = dummyMethodObject.isVarArgs();

		checkPolymorphicSignature(dummyMethodObject);

		metaClass.addMember(this);
		
		if (DEBUG_MEMBER_CREATION)
		{
			logger.debug("Created HelperMetaMethod: {}", toString());
		}
	}

	private List<BytecodeInstruction> instructions;

	public void setInstructions(List<BytecodeInstruction> instructions)
	{
		this.instructions = instructions;
	}

	@Override
	public List<BytecodeInstruction> getInstructions()
	{
		return instructions;
	}
}