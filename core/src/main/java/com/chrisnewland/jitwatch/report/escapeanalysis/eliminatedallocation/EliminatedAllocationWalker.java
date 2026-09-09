/*
 * Copyright (c) 2016-2017 Chris Newland.
 * Licensed under https://github.com/chriswhocodes/jitwatch/blob/master/LICENSE-BSD
 * Instructions: https://github.com/chriswhocodes/jitwatch/wiki
 */
package com.chrisnewland.jitwatch.report.escapeanalysis.eliminatedallocation;

import com.chrisnewland.jitwatch.model.IReadOnlyJITDataModel;
import com.chrisnewland.jitwatch.model.bytecode.BCAnnotationType;
import com.chrisnewland.jitwatch.model.bytecode.LineAnnotation;
import com.chrisnewland.jitwatch.report.escapeanalysis.AbstractEscapeAnalysisWalker;

public class EliminatedAllocationWalker extends AbstractEscapeAnalysisWalker
{
	public EliminatedAllocationWalker(IReadOnlyJITDataModel model)
	{
		super(model);
	}

	@Override
	protected boolean filterLineAnnotation(LineAnnotation la)
	{
		return la.getType() == BCAnnotationType.ELIMINATED_ALLOCATION;
	}
}