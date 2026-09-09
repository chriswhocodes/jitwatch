/*
 * Copyright (c) 2017 Chris Newland.
 * Licensed under https://github.com/chriswhocodes/jitwatch/blob/master/LICENSE-BSD
 * Instructions: https://github.com/chriswhocodes/jitwatch/wiki
 */
package com.chrisnewland.jitwatch.ui.report.inlining;

import static com.chrisnewland.jitwatch.core.JITWatchConstants.S_EMPTY;

import com.chrisnewland.jitwatch.model.Compilation;
import com.chrisnewland.jitwatch.report.Report;
import com.chrisnewland.jitwatch.report.ReportType;
import com.chrisnewland.jitwatch.ui.report.AbstractReportRowBean;

public class InliningRowBean extends AbstractReportRowBean
{
	public InliningRowBean(Report report)
	{
		super(report);
	}

	public String getCompilation()
	{
		Compilation compilation = report.getCaller().getCompilation(report.getCompilationIndex());

		return compilation != null ? compilation.getSignature() : S_EMPTY;
	}
	
	public String getMetaClass()
	{
		return report.getCaller().getMetaClass().getFullyQualifiedName();
	}
	
	public String getMember()
	{
		return report.getCaller().toStringUnqualifiedMethodName(false, false);
	}
	
	public String getSuccess()
	{
		return (report.getType() == ReportType.INLINE_SUCCESS) ? "Yes" : "No";
	}
	
	public String getReason()
	{
		String[] lines = report.getText().split("\n");
		
		String result = "Unknown";
		
		String search = "Inlined: ";
		
		for (String line : lines)
		{
			if (line.startsWith(search))
			{
				result = line.substring(search.length()).trim();
			}
		}
		
		return result;
	}
}