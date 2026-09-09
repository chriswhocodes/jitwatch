/*
 * Copyright (c) 2016-2017 Chris Newland.
 * Licensed under https://github.com/chriswhocodes/jitwatch/blob/master/LICENSE-BSD
 * Instructions: https://github.com/chriswhocodes/jitwatch/wiki
 */
package com.chrisnewland.jitwatch.toplist;

import com.chrisnewland.jitwatch.model.Compilation;
import com.chrisnewland.jitwatch.model.IMetaMember;
import com.chrisnewland.jitwatch.model.IReadOnlyJITDataModel;

public class CompileTimeTopListVisitable extends AbstractTopListVisitable
{
	public CompileTimeTopListVisitable(IReadOnlyJITDataModel model, boolean sortHighToLow)
	{
		super(model, sortHighToLow);
	}

	@Override
	public void visit(IMetaMember mm)
	{
		for (Compilation compilation : mm.getCompilations())
		{
			if (!compilation.isC2N())
			{
				topList.add(new MemberScore(mm, compilation.getCompilationDuration()));
			}
		}	
	}
}