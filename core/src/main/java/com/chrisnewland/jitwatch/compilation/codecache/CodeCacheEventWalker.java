/*
 * Copyright (c) 2017-2021 Chris Newland.
 * Licensed under https://github.com/chriswhocodes/jitwatch/blob/master/LICENSE-BSD
 * Instructions: https://github.com/chriswhocodes/jitwatch/wiki
 */
package com.chrisnewland.jitwatch.compilation.codecache;

import com.chrisnewland.jitwatch.compilation.AbstractCompilationWalker;
import com.chrisnewland.jitwatch.model.CodeCacheEvent;
import com.chrisnewland.jitwatch.model.CodeCacheEvent.CodeCacheEventType;
import com.chrisnewland.jitwatch.model.Compilation;
import com.chrisnewland.jitwatch.model.IMetaMember;
import com.chrisnewland.jitwatch.model.IReadOnlyJITDataModel;
import com.chrisnewland.freelogj.Logger;
import com.chrisnewland.freelogj.LoggerFactory;

public class CodeCacheEventWalker extends AbstractCompilationWalker
{
	private CodeCacheWalkerResult result = new CodeCacheWalkerResult();

	private static final Logger logger = LoggerFactory.getLogger(CodeCacheEventWalker.class);

	public CodeCacheEventWalker(IReadOnlyJITDataModel model)
	{
		super(model);
	}

	@Override
	public void reset()
	{
		result.reset();
	}

	@Override
	public void visit(IMetaMember metaMember)
	{
		if (metaMember != null && metaMember.isCompiled())
		{
			for (Compilation compilation : metaMember.getCompilations())
			{
				if (compilation.isFailed())
				{
					continue;
				}

				String address = compilation.getNativeAddress(); // hex string

				if (address != null)
				{
					long addressLong = 0;

					long stamp = compilation.getStampTaskCompilationStart();

					try
					{
						if (address.startsWith("0x"))
						{
							addressLong = Long.decode(address);
						}
						else
						{
							addressLong = Long.parseLong(address, 16);
						}
					}
					catch (NumberFormatException exception)
					{

						logger.error("Couldn't decode address {} on compilation {}", address, compilation);
						continue; // don't allow a zero address
					}

					// intrinsic has no size info
					int nativeCodeSize = compilation.getNativeSize();

					CodeCacheEvent event = new CodeCacheEvent(CodeCacheEventType.COMPILATION, stamp, nativeCodeSize, 0);
					event.setNativeAddress(addressLong);
					event.setCompilation(compilation);

					result.addEvent(event);
				}
			}
		}
	}

	public CodeCacheWalkerResult getResult()
	{
		return result;
	}
}
