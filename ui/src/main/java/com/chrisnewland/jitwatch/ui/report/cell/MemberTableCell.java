/*
 * Copyright (c) 2013-2017 Chris Newland.
 * Licensed under https://github.com/chriswhocodes/jitwatch/blob/master/LICENSE-BSD
 * Instructions: https://github.com/chriswhocodes/jitwatch/wiki
 */
package com.chrisnewland.jitwatch.ui.report.cell;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.layout.VBox;

import static com.chrisnewland.jitwatch.core.JITWatchConstants.S_EMPTY;
import com.chrisnewland.jitwatch.model.Compilation;
import com.chrisnewland.jitwatch.model.IMetaMember;
import com.chrisnewland.jitwatch.report.Report;
import com.chrisnewland.jitwatch.ui.main.IStageAccessProxy;
import com.chrisnewland.jitwatch.ui.report.IReportRowBean;

public class MemberTableCell extends TableCell<IReportRowBean, Report>
{
	private VBox vBox;
	private Label lblMetaClass;
	private Label lblMetaMember;
	private Label lblCompilation;

	private Button btnTriView;

	private static IStageAccessProxy triViewAccessor;

	public static void setTriViewAccessor(IStageAccessProxy triViewAccessor)
	{
		MemberTableCell.triViewAccessor = triViewAccessor;
	}

	public MemberTableCell()
	{
		vBox = new VBox();

		lblMetaClass = new Label();
		lblMetaMember = new Label();
		lblCompilation = new Label();

		btnTriView = new Button("View");

		vBox.getChildren().add(lblMetaClass);
		vBox.getChildren().add(lblMetaMember);
		vBox.getChildren().add(lblCompilation);
		vBox.getChildren().add(btnTriView);

		vBox.setSpacing(5);

		setGraphic(vBox);
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

			lblMetaClass.setText(member.getMetaClass().getFullyQualifiedName());
			lblMetaMember.setText(member.toStringUnqualifiedMethodName(false, false));

			Compilation compilation = member.getCompilation(report.getCompilationIndex());

			String compilationText = compilation != null ? "Compilation: " + compilation.getSignature() : S_EMPTY;

			lblCompilation.setText(compilationText);

			btnTriView.setText("View BCI " + report.getBytecodeOffset());

			btnTriView.setVisible(true);
		}
		else
		{
			btnTriView.setVisible(false);
		}
	}
}