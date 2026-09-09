/*
 * Copyright (c) 2013-2016 Chris Newland.
 * Licensed under https://github.com/chriswhocodes/jitwatch/blob/master/LICENSE-BSD
 * Instructions: https://github.com/chriswhocodes/jitwatch/wiki
 */
package com.chrisnewland.jitwatch.ui.report.cell;

import com.chrisnewland.jitwatch.ui.report.IReportRowBean;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;

public class TextTableCell extends TableCell<IReportRowBean, String>
{
	private Label label;

	public TextTableCell()
	{
		label = new Label();
		setAlignment(Pos.TOP_LEFT);

		setGraphic(label);
	}

	@Override
	protected void updateItem(String value, boolean empty)
	{		
		if (value != null)
		{
			label.setText(value);
		}
	}
}