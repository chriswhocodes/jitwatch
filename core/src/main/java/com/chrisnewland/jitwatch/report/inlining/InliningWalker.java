/*
 * Copyright (c) 2017 Chris Newland.
 * Licensed under https://github.com/chriswhocodes/jitwatch/blob/master/LICENSE-BSD
 * Instructions: https://github.com/chriswhocodes/jitwatch/wiki
 */
package com.chrisnewland.jitwatch.report.inlining;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.chrisnewland.jitwatch.model.AnnotationException;
import com.chrisnewland.jitwatch.model.Compilation;
import com.chrisnewland.jitwatch.model.IMetaMember;
import com.chrisnewland.jitwatch.model.IParseDictionary;
import com.chrisnewland.jitwatch.model.IReadOnlyJITDataModel;
import com.chrisnewland.jitwatch.model.LogParseException;
import com.chrisnewland.jitwatch.model.Tag;
import com.chrisnewland.jitwatch.model.bytecode.BCAnnotationType;
import com.chrisnewland.jitwatch.model.bytecode.BytecodeAnnotationBuilder;
import com.chrisnewland.jitwatch.model.bytecode.BytecodeAnnotationList;
import com.chrisnewland.jitwatch.model.bytecode.BytecodeAnnotations;
import com.chrisnewland.jitwatch.model.bytecode.LineAnnotation;
import com.chrisnewland.jitwatch.report.AbstractReportBuilder;
import com.chrisnewland.jitwatch.report.Report;
import com.chrisnewland.jitwatch.report.ReportType;

public class InliningWalker extends AbstractReportBuilder
{
	private BytecodeAnnotationBuilder bcAnnotationBuilder;

	private IMetaMember member;

	public InliningWalker(IReadOnlyJITDataModel model, IMetaMember member)
	{
		super(model);

		this.member = member;

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

							for (LineAnnotation la : lineAnnotations)
							{
								if (filterLineAnnotation(la, member))
								{
									ReportType reportType = (la.getType() == BCAnnotationType.INLINE_SUCCESS) ? ReportType.INLINE_SUCCESS : ReportType.INLINE_FAILURE;

									Report report = new Report(currentMember, compilation.getIndex(), bci, la.getAnnotation(),
											reportType, 0, la.getMetaData());

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

	private boolean filterLineAnnotation(LineAnnotation la, IMetaMember child)
	{
		boolean result = false;

		if (la.getType() == BCAnnotationType.INLINE_FAIL || la.getType() == BCAnnotationType.INLINE_SUCCESS)
		{
			Object metaData = la.getMetaData();

			if (metaData != null && metaData instanceof IMetaMember)
			{
				if (child.equals(metaData))
				{
					result = true;
				}
			}
		}

		return result;
	}

	@Override
	public void visitTag(Tag parseTag, IParseDictionary parseDictionary) throws LogParseException
	{
	}
}