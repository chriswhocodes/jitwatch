/*
 * Copyright (c) 2013-2021 Chris Newland.
 * Licensed under https://github.com/chriswhocodes/jitwatch/blob/master/LICENSE-BSD
 * Instructions: https://github.com/chriswhocodes/jitwatch/wiki
 */
package com.chrisnewland.jitwatch.histo;

import static com.chrisnewland.jitwatch.core.JITWatchConstants.ATTR_BYTES;
import static com.chrisnewland.jitwatch.core.JITWatchConstants.ATTR_HOLDER;
import static com.chrisnewland.jitwatch.core.JITWatchConstants.ATTR_NAME;
import static com.chrisnewland.jitwatch.core.JITWatchConstants.C_SLASH;
import static com.chrisnewland.jitwatch.core.JITWatchConstants.S_PARSE_HIR;
import static com.chrisnewland.jitwatch.core.JITWatchConstants.TAG_BC;
import static com.chrisnewland.jitwatch.core.JITWatchConstants.TAG_BRANCH;
import static com.chrisnewland.jitwatch.core.JITWatchConstants.TAG_INLINE_FAIL;
import static com.chrisnewland.jitwatch.core.JITWatchConstants.TAG_INLINE_SUCCESS;
import static com.chrisnewland.jitwatch.core.JITWatchConstants.TAG_KLASS;
import static com.chrisnewland.jitwatch.core.JITWatchConstants.TAG_METHOD;
import static com.chrisnewland.jitwatch.core.JITWatchConstants.TAG_PARSE;
import static com.chrisnewland.jitwatch.core.JITWatchConstants.TAG_PARSE_DONE;
import static com.chrisnewland.jitwatch.core.JITWatchConstants.TAG_PHASE;
import static com.chrisnewland.jitwatch.core.JITWatchConstants.TAG_TYPE;
import static com.chrisnewland.jitwatch.core.JITWatchConstants.TAG_UNCOMMON_TRAP;
import static com.chrisnewland.jitwatch.core.JITWatchConstants.TAG_CALL;
import static com.chrisnewland.jitwatch.core.JITWatchConstants.TAG_DEPENDENCY;

import static com.chrisnewland.jitwatch.core.JITWatchConstants.TAG_DIRECT_CALL;
import static com.chrisnewland.jitwatch.core.JITWatchConstants.TAG_PREDICTED_CALL;
import static com.chrisnewland.jitwatch.core.JITWatchConstants.TAG_OBSERVE;
import static com.chrisnewland.jitwatch.core.JITWatchConstants.TAG_VIRTUAL_CALL;
import static com.chrisnewland.jitwatch.core.JITWatchConstants.TAG_INTRINSIC;
import static com.chrisnewland.jitwatch.core.JITWatchConstants.TAG_CAST_UP;
import static com.chrisnewland.jitwatch.core.JITWatchConstants.TAG_ASSERT_NULL;
import static com.chrisnewland.jitwatch.core.JITWatchConstants.TAG_PHASE_DONE;


import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.chrisnewland.jitwatch.compilation.CompilationUtil;
import com.chrisnewland.jitwatch.model.Compilation;
import com.chrisnewland.jitwatch.model.IMetaMember;
import com.chrisnewland.jitwatch.model.IParseDictionary;
import com.chrisnewland.jitwatch.model.IReadOnlyJITDataModel;
import com.chrisnewland.jitwatch.model.LogParseException;
import com.chrisnewland.jitwatch.model.Tag;
import com.chrisnewland.freelogj.Logger;
import com.chrisnewland.freelogj.LoggerFactory;

public class InlineSizeHistoVisitable extends AbstractHistoVisitable
{
	private static final Logger logger = LoggerFactory.getLogger(InlineSizeHistoVisitable.class);

	private Set<String> inlinedCounted = new HashSet<>();

	public InlineSizeHistoVisitable(IReadOnlyJITDataModel model, long resolution)
	{
		super(model, resolution);
		
		ignoreTags.add(TAG_CALL);
		ignoreTags.add(TAG_DEPENDENCY);	
		ignoreTags.add(TAG_BC);
		ignoreTags.add(TAG_KLASS);
		ignoreTags.add(TAG_PARSE_DONE);
		ignoreTags.add(TAG_UNCOMMON_TRAP);
		ignoreTags.add(TAG_TYPE);
		ignoreTags.add(TAG_BRANCH);
		ignoreTags.add(TAG_DIRECT_CALL);
		ignoreTags.add(TAG_PREDICTED_CALL);
		ignoreTags.add(TAG_OBSERVE);
		ignoreTags.add(TAG_VIRTUAL_CALL);
		ignoreTags.add(TAG_INTRINSIC);
		ignoreTags.add(TAG_CAST_UP);
		ignoreTags.add(TAG_ASSERT_NULL);
		ignoreTags.add(TAG_PHASE_DONE);
	}

	@Override
	public void reset()
	{
		inlinedCounted.clear();
	}

	@Override
	public void visit(IMetaMember metaMember)
	{
		if (metaMember != null && metaMember.isCompiled())
		{			
			try
			{
				for (Compilation compilation : metaMember.getCompilations())
				{
					CompilationUtil.visitParseTagsOfCompilation(compilation, this);
				}
			}
			catch (LogParseException e)
			{
				logger.error("Could not build histo for {}", metaMember.getMemberName(), e);
			}
		}
	}

	private void processParseTag(Tag parseTag, IParseDictionary parseDictionary)
	{
		String currentMethod = null;
		String holder = null;
		String attrInlineBytes = null;

		for (Tag child : parseTag.getChildren())
		{
			String tagName = child.getName();
			Map<String, String> attrs = child.getAttributes();

			switch (tagName)
			{
			case TAG_METHOD:
			{
				currentMethod = attrs.get(ATTR_NAME);
				holder = attrs.get(ATTR_HOLDER);
				attrInlineBytes = attrs.get(ATTR_BYTES);
				break;
			}

			case TAG_INLINE_FAIL:
			{
				// clear method to prevent incorrect pickup by next inline
				// success
				currentMethod = null;
				holder = null;
				attrInlineBytes = null;
				
				break;
			}

			case TAG_INLINE_SUCCESS:
			{
				if (holder != null && currentMethod != null && attrInlineBytes != null)
				{
					Tag klassTag = parseDictionary.getKlass(holder);

					if (klassTag != null)
					{
						String fqName = klassTag.getAttributes().get(ATTR_NAME) + C_SLASH + currentMethod;

						if (!inlinedCounted.contains(fqName))
						{
							long inlinedByteCount = Long.parseLong(attrInlineBytes);
							histo.addValue(inlinedByteCount);

							inlinedCounted.add(fqName);
						}
					}
				}
				
				break;
			}
				
			case TAG_PARSE:
			{
				processParseTag(child, parseDictionary);
				break;
			}
				
  			case TAG_PHASE:
			{
				String phaseName = attrs.get(ATTR_NAME);
				
				if (S_PARSE_HIR.equals(phaseName))
				{
					processParseTag(child, parseDictionary);
				}
				else
				{
					logger.warn("Don't know how to handle phase {}", phaseName);
				}
				
				break;
			}

			default:
				handleOther(child);
				break;
			}
		}
	}

	@Override
	public void visitTag(Tag parseTag, IParseDictionary parseDictionary) throws LogParseException
	{
		processParseTag(parseTag, parseDictionary);
	}
}
