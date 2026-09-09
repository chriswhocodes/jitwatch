/*
 * Copyright (c) 2013-2017 Chris Newland.
 * Licensed under https://github.com/chriswhocodes/jitwatch/blob/master/LICENSE-BSD
 * Instructions: https://github.com/chriswhocodes/jitwatch/wiki
 */
package com.chrisnewland.jitwatch.ui.report.cell;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Button;
import javafx.scene.control.TableCell;

import com.chrisnewland.jitwatch.model.IMetaMember;
import com.chrisnewland.jitwatch.report.Report;
import com.chrisnewland.jitwatch.ui.main.IStageAccessProxy;
import com.chrisnewland.jitwatch.ui.report.IReportRowBean;

public class LinkedBCICell extends TableCell<IReportRowBean, Report>
{
	private Button btnTriView;

	private static IStageAccessProxy triViewAccessor;

	public static void setTriViewAccessor(IStageAccessProxy triViewAccessor)
	{
		LinkedBCICell.triViewAccessor = triViewAccessor;
	}

	public LinkedBCICell()
	{
		btnTriView = new Button("View");

		setGraphic(btnTriView);
	}

	@Override
	protected void updateItem(final Report report, boolean empty)
	{
		if (report != null && report.getCaller() != null)
		{
			final IMetaMember member = report.getCaller();

			btnTriView.setOnAction(new EventHandler<ActionEvent>()
			{
				@Override
				public void handle(ActionEvent e)
				{
					if (report.getCompilationIndex() != -1)
					{
						member.setSelectedCompilation(report.getCompilationIndex());
					}

					triViewAccessor.openTriView(member, report.getBytecodeOffset());
				}
			});

			btnTriView.setText("View BCI " + report.getBytecodeOffset());

			btnTriView.setVisible(true);
		}
		else
		{
			btnTriView.setVisible(false);
		}
	}
}