package com.chrisnewland.jitwatch.test;

import java.io.Reader;

import com.chrisnewland.jitwatch.core.IJITListener;
import com.chrisnewland.jitwatch.model.CompilerThread;
import com.chrisnewland.jitwatch.model.IMetaMember;
import com.chrisnewland.jitwatch.model.Tag;
import com.chrisnewland.jitwatch.model.Task;
import com.chrisnewland.jitwatch.parser.AbstractLogParser;

public class UnitTestLogParser extends AbstractLogParser
{
	public UnitTestLogParser(IJITListener jitListener)
	{
		super(jitListener);

		currentCompilerThread = new CompilerThread("1234", "TestCompilerThread");
	}

	@Override
	protected void parseLogFile()
	{
	}

	@Override
	protected void splitLogFile(Reader logFileReader)
	{
	}

	@Override
	protected void handleTag(Tag tag)
	{
	}

	@Override
	public void setTagTaskQueued(Tag tagTaskQueued, IMetaMember metaMember)
	{
		super.setTagTaskQueued(tagTaskQueued, metaMember);
	}

	@Override
	public void setTagNMethod(Tag tagNMethod, IMetaMember member)
	{
		super.setTagNMethod(tagNMethod, member);
	}

	@Override
	public void setTagTask(Task tagTask, IMetaMember member)
	{
		super.setTagTask(tagTask, member);
	}

	public void setTagTaskDone(Tag tagTaskDone, IMetaMember member)
	{
		super.handleTaskDone(tagTaskDone, member);
	}
}