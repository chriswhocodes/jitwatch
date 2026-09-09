/*
 * Copyright (c) 2016 Chris Newland.
 * Licensed under https://github.com/chriswhocodes/jitwatch/blob/master/LICENSE-BSD
 * Instructions: https://github.com/chriswhocodes/jitwatch/wiki
 */
package com.chrisnewland.jitwatch.toplist;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.chrisnewland.jitwatch.hotthrow.HotThrowFinder;
import com.chrisnewland.jitwatch.hotthrow.HotThrowResult;
import com.chrisnewland.jitwatch.model.IMetaMember;
import com.chrisnewland.jitwatch.model.IReadOnlyJITDataModel;

public class HotThrowTopListVisitable extends AbstractTopListVisitable
{
	private final Map<String, Integer> hotThrowMap;

	public HotThrowTopListVisitable(IReadOnlyJITDataModel model, boolean sortHighToLow)
	{
		super(model, sortHighToLow);
		hotThrowMap = new HashMap<>();
	}

	@Override
	public void visit(IMetaMember metaMember)
	{
		if (metaMember.isCompiled())
		{
			HotThrowFinder finder = new HotThrowFinder(model);

			Set<HotThrowResult> results = finder.findHotThrows(metaMember);

			for (HotThrowResult result : results)
			{
				String iMapping = result.getMember().toString() + " BCI:" + result.getBci() + " preallocated:"
						+ result.isPreallocated() + " => " + result.getExceptionType();

				int count = 0;

				if (hotThrowMap.containsKey(iMapping))
				{
					count = hotThrowMap.get(iMapping);
				}

				hotThrowMap.put(iMapping, count + 1);
			}
		}
	}

	@Override
	public void postProcess()
	{
		for (Map.Entry<String, Integer> entry : hotThrowMap.entrySet())
		{
			topList.add(new StringTopListScore(entry.getKey(), entry.getValue().longValue()));
		}
	}
}