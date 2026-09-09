/*
 * Copyright (c) 2013-2017 Chris Newland.
 * Licensed under https://github.com/chriswhocodes/jitwatch/blob/master/LICENSE-BSD
 * Instructions: https://github.com/chriswhocodes/jitwatch/wiki
 */
package com.chrisnewland.jitwatch.ui.viewer;

import java.util.List;

import com.chrisnewland.jitwatch.ui.main.JITWatchUI;
import com.chrisnewland.jitwatch.ui.triview.Viewer;
import com.chrisnewland.jitwatch.util.UserInterfaceUtil;

import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public abstract class AbstractTextViewerStage extends Stage
{
	private Viewer viewer;

	public AbstractTextViewerStage(final JITWatchUI parent, String title, boolean highlighting)
	{
		initStyle(StageStyle.DECORATED);

		viewer = new Viewer(parent, highlighting);

		setTitle(title);

		Scene scene = UserInterfaceUtil.getScene(viewer, 640, 480);

		setScene(scene);
	}

	protected void setContent(List<Label> items, int maxLineLength)
	{
		viewer.setContent(items);

		int x = Math.min(80, maxLineLength);
		int y = Math.min(30, items.size());

		x = Math.max(x, 20);
		y = Math.max(y, 20);

		setWidth(x * 12);
		setHeight(y * 19);
	}
}