/*
 * Copyright (c) 2016 Chris Newland.
 * Licensed under https://github.com/chriswhocodes/jitwatch/blob/master/LICENSE-BSD
 * Instructions: https://github.com/chriswhocodes/jitwatch/wiki
 */
package com.chrisnewland.jitwatch.histo;

import com.chrisnewland.jitwatch.model.Compilation;
import com.chrisnewland.jitwatch.model.IMetaMember;
import com.chrisnewland.jitwatch.model.IReadOnlyJITDataModel;

public class NativeSizeHistoWalker extends AbstractHistoVisitable
{
	public NativeSizeHistoWalker(IReadOnlyJITDataModel model, long resolution)
	{
		super(model, resolution);
	}

	@Override
	public void visit(IMetaMember mm)
	{
		for (Compilation compilation : mm.getCompilations())
		{
			long nativeSize = compilation.getNativeSize();
	
			if (nativeSize != 0)
			{
				histo.addValue(nativeSize);
			}
		}
	}
}