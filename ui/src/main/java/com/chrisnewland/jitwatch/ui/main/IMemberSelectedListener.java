/*
 * Copyright (c) 2017 Chris Newland.
 * Licensed under https://github.com/chriswhocodes/jitwatch/blob/master/LICENSE-BSD
 * Instructions: https://github.com/chriswhocodes/jitwatch/wiki
 */
package com.chrisnewland.jitwatch.ui.main;

import com.chrisnewland.jitwatch.model.IMetaMember;

public interface IMemberSelectedListener
{
	void selectMember(IMetaMember member, boolean updateTree, boolean updateTriView);
	
	void selectCompilation(IMetaMember member, int compilationIndex);
}