/*
 * Copyright (c) 2013-2016 Chris Newland.
 * Licensed under https://github.com/chriswhocodes/jitwatch/blob/master/LICENSE-BSD
 * Instructions: https://github.com/chriswhocodes/jitwatch/wiki
 */
package com.chrisnewland.jitwatch.model.bytecode;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.chrisnewland.jitwatch.model.IMetaMember;

public class BytecodeAnnotations
{
	private Map<IMetaMember, BytecodeAnnotationList> memberAnnotationMap = new HashMap<>();

	public void addAnnotation(IMetaMember member, int bci, LineAnnotation annotation)
	{
		BytecodeAnnotationList memberList = memberAnnotationMap.get(member);

		if (memberList == null)
		{
			memberList = new BytecodeAnnotationList();
			memberAnnotationMap.put(member, memberList);
		}

		memberList.addAnnotation(bci, annotation);
	}
	
	public BytecodeAnnotationList getAnnotationList(IMetaMember member)
	{
		return memberAnnotationMap.get(member);
	}
	
	public void clear()
	{
		memberAnnotationMap.clear();
	}
	
	public Set<IMetaMember> getMembers()
	{
		return memberAnnotationMap.keySet();
	}
}