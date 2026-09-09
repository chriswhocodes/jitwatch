package com.chrisnewland.jitwatch.ui.main;

import com.chrisnewland.jitwatch.model.IMetaMember;

public interface ICompilationChangeListener
{
	void compilationChanged(IMetaMember member);
}