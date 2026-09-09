/*
 * Copyright (c) 2017 Chris Newland.
 * Licensed under https://github.com/chriswhocodes/jitwatch/blob/master/LICENSE-BSD
 * Instructions: https://github.com/chriswhocodes/jitwatch/wiki
 */
package com.chrisnewland.jitwatch.compilation;

import com.chrisnewland.jitwatch.model.IParseDictionary;
import com.chrisnewland.jitwatch.model.IReadOnlyJITDataModel;
import com.chrisnewland.jitwatch.model.LogParseException;
import com.chrisnewland.jitwatch.model.Tag;
import com.chrisnewland.jitwatch.treevisitor.ITreeVisitable;
import com.chrisnewland.jitwatch.treevisitor.TreeVisitor;

public abstract class AbstractCompilationWalker extends AbstractCompilationVisitable implements ITreeVisitable
{
    protected IReadOnlyJITDataModel model;

	public AbstractCompilationWalker(IReadOnlyJITDataModel model)
	{
		this.model = model;
	}

	public void walkCompilations()
	{
		TreeVisitor.walkTree(model, this);
	}

	@Override
	public void visitTag(Tag toVisit, IParseDictionary parseDictionary) throws LogParseException
	{		
	}
}