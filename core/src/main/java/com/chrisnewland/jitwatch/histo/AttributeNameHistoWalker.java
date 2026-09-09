/*
 * Copyright (c) 2013-2015 Chris Newland.
 * Licensed under https://github.com/chriswhocodes/jitwatch/blob/master/LICENSE-BSD
 * Instructions: https://github.com/chriswhocodes/jitwatch/wiki
 */
package com.chrisnewland.jitwatch.histo;

import com.chrisnewland.jitwatch.model.IMetaMember;
import com.chrisnewland.jitwatch.model.IReadOnlyJITDataModel;

public class AttributeNameHistoWalker extends AbstractHistoVisitable
{
	private boolean isCompileAttribute;
	private String attributeName;
	
	public AttributeNameHistoWalker(IReadOnlyJITDataModel model, boolean isCompileAttribute, String attributeName, long resolution)
	{
		super(model, resolution);

		this.isCompileAttribute = isCompileAttribute;
		this.attributeName = attributeName;
	}	
	
	@Override
	public void visit(IMetaMember mm)
	{		
		String attrValue = null;

		if (isCompileAttribute)
		{
			attrValue = mm.getCompiledAttribute(attributeName);
		}
		else
		{
			attrValue = mm.getQueuedAttribute(attributeName);
		}

		if (attrValue != null)
		{
			long val = Long.valueOf(attrValue);

			histo.addValue(val);
		}		
	}
}
