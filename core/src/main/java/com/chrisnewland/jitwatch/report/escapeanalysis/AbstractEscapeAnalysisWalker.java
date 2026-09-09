/*
 * Copyright (c) 2017 Chris Newland.
 * Licensed under https://github.com/chriswhocodes/jitwatch/blob/master/LICENSE-BSD
 * Instructions: https://github.com/chriswhocodes/jitwatch/wiki
 */
package com.chrisnewland.jitwatch.report.escapeanalysis;

import static com.chrisnewland.jitwatch.core.JITWatchConstants.TAG_CAST_UP;
import static com.chrisnewland.jitwatch.core.JITWatchConstants.TAG_DEPENDENCY;
import static com.chrisnewland.jitwatch.core.JITWatchConstants.TAG_DIRECT_CALL;
import static com.chrisnewland.jitwatch.core.JITWatchConstants.TAG_INLINE_SUCCESS;
import static com.chrisnewland.jitwatch.core.JITWatchConstants.TAG_INTRINSIC;
import static com.chrisnewland.jitwatch.core.JITWatchConstants.TAG_KLASS;
import static com.chrisnewland.jitwatch.core.JITWatchConstants.TAG_OBSERVE;
import static com.chrisnewland.jitwatch.core.JITWatchConstants.TAG_PARSE_DONE;
import static com.chrisnewland.jitwatch.core.JITWatchConstants.TAG_PHASE_DONE;
import static com.chrisnewland.jitwatch.core.JITWatchConstants.TAG_PREDICTED_CALL;
import static com.chrisnewland.jitwatch.core.JITWatchConstants.TAG_TYPE;
import static com.chrisnewland.jitwatch.core.JITWatchConstants.TAG_UNCOMMON_TRAP;
import static com.chrisnewland.jitwatch.core.JITWatchConstants.TAG_VIRTUAL_CALL;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.chrisnewland.jitwatch.core.JITWatchConstants.TAG_ASSERT_NULL;
import com.chrisnewland.jitwatch.model.AnnotationException;
import com.chrisnewland.jitwatch.model.Compilation;
import com.chrisnewland.jitwatch.model.IMetaMember;
import com.chrisnewland.jitwatch.model.IParseDictionary;
import com.chrisnewland.jitwatch.model.IReadOnlyJITDataModel;
import com.chrisnewland.jitwatch.model.LogParseException;
import com.chrisnewland.jitwatch.model.Tag;
import com.chrisnewland.jitwatch.model.bytecode.BytecodeAnnotationBuilder;
import com.chrisnewland.jitwatch.model.bytecode.BytecodeAnnotationList;
import com.chrisnewland.jitwatch.model.bytecode.BytecodeAnnotations;
import com.chrisnewland.jitwatch.model.bytecode.LineAnnotation;
import com.chrisnewland.jitwatch.report.AbstractReportBuilder;
import com.chrisnewland.jitwatch.report.Report;
import com.chrisnewland.jitwatch.report.ReportType;

public abstract class AbstractEscapeAnalysisWalker extends AbstractReportBuilder
{
	private BytecodeAnnotationBuilder bcAnnotationBuilder;

	public AbstractEscapeAnalysisWalker(IReadOnlyJITDataModel model)
	{
		super(model);

		ignoreTags.add(TAG_KLASS);
		ignoreTags.add(TAG_TYPE);
		ignoreTags.add(TAG_DEPENDENCY);
		ignoreTags.add(TAG_PARSE_DONE);
		ignoreTags.add(TAG_DIRECT_CALL);
		ignoreTags.add(TAG_PHASE_DONE);
		ignoreTags.add(TAG_INLINE_SUCCESS);
		ignoreTags.add(TAG_UNCOMMON_TRAP);
		ignoreTags.add(TAG_INTRINSIC);
		ignoreTags.add(TAG_PREDICTED_CALL);
		ignoreTags.add(TAG_VIRTUAL_CALL);
		ignoreTags.add(TAG_CAST_UP);
		ignoreTags.add(TAG_OBSERVE);
		ignoreTags.add(TAG_ASSERT_NULL);

		bcAnnotationBuilder = new BytecodeAnnotationBuilder(false);
	}

	@Override
	protected void findNonMemberReports()
	{
	}

	@Override
	public void visit(IMetaMember metaMember)
	{
		if (metaMember != null && metaMember.isCompiled())
		{
			for (Compilation compilation : metaMember.getCompilations())
			{
				try
				{
					BytecodeAnnotations annotations = bcAnnotationBuilder.buildBytecodeAnnotations(metaMember,
							compilation.getIndex(), model);

					Set<IMetaMember> membersWithAnnotations = annotations.getMembers();

					for (IMetaMember currentMember : membersWithAnnotations)
					{
						BytecodeAnnotationList annotationsForMember = annotations.getAnnotationList(currentMember);

						for (Map.Entry<Integer, List<LineAnnotation>> entry : annotationsForMember.getEntries())
						{
							List<LineAnnotation> lineAnnotations = entry.getValue();

							int bci = entry.getKey();

							boolean inlineAtBCI = hasInlineSuccessAnnotation(lineAnnotations);

							for (LineAnnotation la : lineAnnotations)
							{
								if (filterLineAnnotation(la))
								{
									ReportType type = inlineAtBCI ? ReportType.ELIMINATED_ALLOCATION_INLINE
											: ReportType.ELIMINATED_ALLOCATION_DIRECT;

									Report report = new Report(currentMember, compilation.getIndex(), bci, la.getAnnotation(), type,
											0, la.getMetaData());

									reportList.add(report);
								}
							}
						}
					}
				}
				catch (AnnotationException e)
				{
					e.printStackTrace();
				}
			}
		}
	}
	
	protected abstract boolean filterLineAnnotation(LineAnnotation la);

	private boolean hasInlineSuccessAnnotation(List<LineAnnotation> annotations)
	{
		boolean result = false;

		for (LineAnnotation la : annotations)
		{
			switch (la.getType())
			{
			case INLINE_SUCCESS:
				result = true;
				break;
			default:
				break;
			}
		}

		return result;
	}

	@Override
	public void visitTag(Tag parseTag, IParseDictionary parseDictionary) throws LogParseException
	{
	}
}