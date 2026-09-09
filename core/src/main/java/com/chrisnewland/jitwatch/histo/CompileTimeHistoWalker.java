/*
 * Copyright (c) 2016-2017 Chris Newland.
 * Licensed under https://github.com/chriswhocodes/jitwatch/blob/master/LICENSE-BSD
 * Instructions: https://github.com/chriswhocodes/jitwatch/wiki
 */
package com.chrisnewland.jitwatch.histo;

import com.chrisnewland.jitwatch.model.Compilation;
import com.chrisnewland.jitwatch.model.IMetaMember;
import com.chrisnewland.jitwatch.model.IReadOnlyJITDataModel;

public class CompileTimeHistoWalker extends AbstractHistoVisitable
{
	public CompileTimeHistoWalker(IReadOnlyJITDataModel model, long resolution) // TODO filter by compile level?
	{
		super(model, resolution);
	}

	@Override
	public void visit(IMetaMember mm)
	{
		for (Compilation compilation : mm.getCompilations())
		{
			if (!compilation.isC2N())
			{
				histo.addValue(compilation.getCompilationDuration());
			}
		}
	}
}