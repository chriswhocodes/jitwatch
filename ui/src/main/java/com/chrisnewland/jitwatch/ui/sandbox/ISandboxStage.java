/*
 * Copyright (c) 2013-2015 Chris Newland.
 * Licensed under https://github.com/chriswhocodes/jitwatch/blob/master/LICENSE-BSD
 * Instructions: https://github.com/chriswhocodes/jitwatch/wiki
 */
package com.chrisnewland.jitwatch.ui.sandbox;

import java.io.File;

import javafx.stage.Stage;

import com.chrisnewland.jitwatch.model.IMetaMember;
import com.chrisnewland.jitwatch.parser.ILogParseErrorListener;

public interface ISandboxStage extends ILogParseErrorListener
{
	void openTriView(IMetaMember member);

	void showOutput(String output);

	void showError(String error);

	void runFile(EditorPane editor);

	void addSourceFolder(File dir);

	Stage getStageForChooser();

	void log(String msg);

	void setModified(EditorPane pane, boolean isModified);
}