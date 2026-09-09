/*
 * Copyright (c) 2013-2015 Chris Newland.
 * Licensed under https://github.com/chriswhocodes/jitwatch/blob/master/LICENSE-BSD
 * Instructions: https://github.com/chriswhocodes/jitwatch/wiki
 */
package com.chrisnewland.jitwatch.histo;

import com.chrisnewland.jitwatch.compilation.AbstractCompilationVisitable;
import com.chrisnewland.jitwatch.model.IParseDictionary;
import com.chrisnewland.jitwatch.model.IReadOnlyJITDataModel;
import com.chrisnewland.jitwatch.model.LogParseException;
import com.chrisnewland.jitwatch.model.Tag;
import com.chrisnewland.jitwatch.treevisitor.TreeVisitor;

public abstract class AbstractHistoVisitable extends AbstractCompilationVisitable implements IHistoVisitable
{
	protected Histo histo;
	protected IReadOnlyJITDataModel model;
	protected long resolution;

	public AbstractHistoVisitable(IReadOnlyJITDataModel model, long resolution)
	{
		this.model = model;
		this.resolution = resolution;
	}

	@Override
	public Histo buildHistogram()
	{
		histo = new Histo(resolution);

		TreeVisitor.walkTree(model, this);

		return histo;
	}

	@Override
	public void reset()
	{
	}

	@Override
	public void visitTag(Tag toVisit, IParseDictionary parseDictionary) throws LogParseException
	{
	}	
}