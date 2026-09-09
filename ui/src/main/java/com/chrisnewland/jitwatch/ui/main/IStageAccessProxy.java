/*
 * Copyright (c) 2013-2017 Chris Newland.
 * Licensed under https://github.com/chriswhocodes/jitwatch/blob/master/LICENSE-BSD
 * Instructions: https://github.com/chriswhocodes/jitwatch/wiki
 */
package com.chrisnewland.jitwatch.ui.main;

import javafx.stage.Stage;

import com.chrisnewland.jitwatch.core.JITWatchConfig;
import com.chrisnewland.jitwatch.model.IMetaMember;

public interface IStageAccessProxy
{
	void openTriView(IMetaMember member);
	
	void openTriView(IMetaMember member, int highlightBCI);
		
	void openBrowser(String title, String html, String stylesheet);
	
	void openTextViewer(String title, String contents, boolean lineNumbers, boolean highlighting);
	
	void openCompileChain(IMetaMember member);
		
	void openInlinedIntoReport(IMetaMember member);

	Stage getStageForDialog();
	
	JITWatchConfig getConfig();
}