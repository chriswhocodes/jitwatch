/*
 * Copyright (c) 2013-2017 Chris Newland.
 * Licensed under https://github.com/chriswhocodes/jitwatch/blob/master/LICENSE-BSD
 * Instructions: https://github.com/chriswhocodes/jitwatch/wiki
 */
package com.chrisnewland.jitwatch.ui.graphing;

import static com.chrisnewland.jitwatch.core.JITWatchConstants.ATTR_COMPILER;
import static com.chrisnewland.jitwatch.core.JITWatchConstants.ATTR_COMPILE_KIND;
import static com.chrisnewland.jitwatch.core.JITWatchConstants.ATTR_DECOMPILES;
import static com.chrisnewland.jitwatch.core.JITWatchConstants.ATTR_LEVEL;
import static com.chrisnewland.jitwatch.core.JITWatchConstants.C_CLOSE_PARENTHESES;
import static com.chrisnewland.jitwatch.core.JITWatchConstants.C_OPEN_PARENTHESES;
import static com.chrisnewland.jitwatch.core.JITWatchConstants.C_SPACE;
import static com.chrisnewland.jitwatch.core.JITWatchConstants.S_CLOSE_PARENTHESES;
import static com.chrisnewland.jitwatch.core.JITWatchConstants.TAG_NMETHOD;
import static com.chrisnewland.jitwatch.core.JITWatchConstants.TAG_TASK_QUEUED;
import static com.chrisnewland.jitwatch.util.UserInterfaceUtil.fix;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import com.chrisnewland.jitwatch.model.Compilation;
import com.chrisnewland.jitwatch.model.IMetaMember;
import com.chrisnewland.jitwatch.model.JITEvent;
import com.chrisnewland.jitwatch.model.JITStats;
import com.chrisnewland.jitwatch.model.Tag;
import com.chrisnewland.jitwatch.ui.main.JITWatchUI;
import com.chrisnewland.jitwatch.util.ParseUtil;
import com.chrisnewland.jitwatch.util.UserInterfaceUtil;

import javafx.scene.Scene;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;

public class TimeLineStage extends AbstractGraphStage
{
	private IMetaMember selectedMember = null;
	private int compilationIndex = 0;
	private static final double MARKET_DIAMETER = 10;
	private boolean labelLeft = true;

	private boolean drawnQueueEvent = false;

	public TimeLineStage(final JITWatchUI parent)
	{
		super(parent, JITWatchUI.WINDOW_WIDTH, JITWatchUI.WINDOW_HEIGHT, true);

		StackPane root = new StackPane();
		Scene scene = UserInterfaceUtil.getScene(root, width, height);

		canvas.widthProperty().bind(root.widthProperty());
		canvas.heightProperty().bind(root.heightProperty());

		root.getChildren().add(canvas);

		setTitle("Compilations Timeline");

		setScene(scene);
		show();

		redraw();
	}

	@Override
	public final void redraw()
	{
		labelLeft = true;

		super.baseRedraw();

		gc.setFont(STANDARD_FONT);

		if (selectedMember != mainUI.getSelectedMember())
		{
			selectedMember = mainUI.getSelectedMember();
		}

		List<JITEvent> events = mainUI.getJITDataModel().getEventListCopy();

		compilationIndex = 0;

		if (events.size() > 0)
		{
			Collections.sort(events, new Comparator<JITEvent>()
			{
				@Override
				public int compare(JITEvent e1, JITEvent e2)
				{
					return Long.compare(e1.getStamp(), e2.getStamp());
				}
			});

			JITEvent firstEvent = events.get(0);
			minX = firstEvent.getStamp();

			Tag endOfLogTag = mainUI.getJITDataModel().getEndOfLogTag();

			JITEvent lastEvent = events.get(events.size() - 1);
			
			if (endOfLogTag != null)
			{
				maxX = getStampFromTag(endOfLogTag);
				
				long lastEventPlusPadding = (long)(lastEvent.getStamp() * 1.1);
				
				maxX = Math.min(maxX, lastEventPlusPadding);
			}
			else
			{
				maxX = lastEvent.getStamp();
			}

			minY = 0;

			calculateMaxCompiles(events);

			drawAxes();

			drawEvents(events);

			showSelectedMemberLabel();
		}
		else
		{
			gc.fillText("No compilation information processed", fix(10), fix(10));
		}
	}

	private void calculateMaxCompiles(List<JITEvent> events)
	{
		maxY = events.size();
	}

	private void drawMemberEvents(List<Compilation> compilations, long stamp, double yPos)
	{
		if (compilationIndex >= compilations.size())
		{
			return;
		}

		Compilation compilation = compilations.get(compilationIndex);

		if (!compilation.isC2N())
		{
			if (!drawnQueueEvent)
			{
				Tag tagTaskQueued = compilation.getTagTaskQueued();

				if (tagTaskQueued != null)
				{
					long tagTime = compilation.getStampTaskQueued();

					if (tagTime == stamp)
					{
						drawMemberEvent(compilation, tagTaskQueued, stamp, yPos);
						drawnQueueEvent = true;
					}
				}
			}

			Tag tagNMethod = compilation.getTagNMethod();

			if (tagNMethod != null)
			{
				long tagTime = compilation.getStampTaskCompilationStart();

				if (tagTime == stamp)
				{
					drawMemberEvent(compilation, tagNMethod, stamp, yPos);

					compilationIndex++;
					drawnQueueEvent = false;
				}
			}
		}
	}

	private void drawMemberEvent(Compilation compilation, Tag tag, long stamp, double yPos)
	{
		long journalEventTime = ParseUtil.getStamp(tag.getAttributes());

		gc.setFill(Color.BLUE);

		double smX = graphGapLeft + normaliseX(journalEventTime);

		double blobX = fix(smX - MARKET_DIAMETER / 2);
		double blobY = fix(yPos - MARKET_DIAMETER / 2);

		gc.fillOval(blobX, blobY, fix(MARKET_DIAMETER), fix(MARKET_DIAMETER));

		String label = buildLabel(tag, journalEventTime, compilation);

		double labelX;
		double labelY;

		if (labelLeft)
		{
			labelX = blobX - getApproximateStringWidth(label) - 16;
			labelY = Math.min(blobY - getStringHeight(), graphGapTop + chartHeight - 32);

		}
		else
		{
			labelX = blobX + 16;
			labelY = Math.min(blobY, graphGapTop + chartHeight - 32);
		}

		labelLeft = !labelLeft;

		drawLabel(label, labelX, labelY, getLabelColour(tag));
	}

	private Color getLabelColour(Tag tag)
	{
		Color result = Color.WHITE;

		String tagName = tag.getName();

		if (TAG_NMETHOD.equals(tagName))
		{
			if (tag.getAttributes().containsKey(ATTR_DECOMPILES))
			{
				result = Color.ORANGERED;
			}
			else
			{
				result = Color.LIMEGREEN;
			}
		}
		else if (TAG_TASK_QUEUED.equals(tagName))
		{
			result = Color.YELLOW;
		}

		return result;
	}

	private void showSelectedMemberLabel()
	{
		if (selectedMember != null)
		{
			gc.setFont(MEMBER_FONT);
			drawLabel(selectedMember.toString(), 56, 40);
		}
	}

	private void drawLabel(String text, double xPos, double yPos)
	{
		drawLabel(text, xPos, yPos, Color.WHITE);
	}

	private String buildLabel(Tag nextJournalEvent, long journalEventTime, Compilation compilation)
	{
		StringBuilder selectedItemBuilder = new StringBuilder();

		String tagName = nextJournalEvent.getName();

		if (TAG_TASK_QUEUED.equals(tagName))
		{
			selectedItemBuilder.append("Queued");
		}
		else
		{
			Map<String, String> eventAttributes = nextJournalEvent.getAttributes();

//			if (eventAttributes.containsKey(ATTR_DECOMPILES))
//			{
//				selectedItemBuilder.append("Recompiled");
//			}
//			else
//			{
//				selectedItemBuilder.append("Compiled");
//			}

			String compiler = eventAttributes.get(ATTR_COMPILER);

			if (compiler == null)
			{
				compiler = "Unknown";
			}

			selectedItemBuilder.append(compiler);

			String compileKind = eventAttributes.get(ATTR_COMPILE_KIND);

			if (compileKind != null)
			{
				selectedItemBuilder.append(C_SPACE).append(C_OPEN_PARENTHESES).append(compileKind.toUpperCase())
						.append(C_CLOSE_PARENTHESES);
			}

			String level = eventAttributes.get(ATTR_LEVEL);

			if (level != null)
			{
				selectedItemBuilder.append(" (Level ").append(level).append(C_CLOSE_PARENTHESES);
			}

			if (!compilation.isC2N())
			{
				long duration = compilation.getCompilationDuration();
				
				if (duration != 0)
				{
					selectedItemBuilder.append(" in ").append(duration).append("ms");
				}
			}
		}

		return selectedItemBuilder.toString();
	}

	private void drawEvents(List<JITEvent> events)
	{
		Color colourMarker = Color.BLUE;
		double lineWidth = 2.0;

		int cumC = 0;

		double lastCX = graphGapLeft + normaliseX(minX);
		double lastCY = graphGapTop + normaliseY(0);

		for (JITEvent event : events)
		{
			long stamp = event.getStamp();

			cumC++;

			double x = graphGapLeft + normaliseX(stamp);

			double y = graphGapTop + normaliseY(cumC);

			if (selectedMember != null)
			{
				List<Compilation> compilations = selectedMember.getCompilations();

				if (!compilations.isEmpty())
				{
					drawMemberEvents(compilations, stamp, y);
				}
			}

			gc.setStroke(colourMarker);
			gc.setLineWidth(lineWidth);
			gc.strokeLine(fix(lastCX), fix(lastCY), fix(x), fix(y));

			lastCX = x;
			lastCY = y;
		}

		continueLineToEndOfXAxis(lastCX, lastCY, colourMarker, lineWidth);

		showStatsLegend(gc);
	}

	private void showStatsLegend(GraphicsContext gc)
	{
		JITStats stats = mainUI.getJITDataModel().getJITStats();

		StringBuilder compiledStatsBuilder = new StringBuilder();
		compiledStatsBuilder.append("Total Compilations: ").append(stats.getTotalCompiledMethods());
		compiledStatsBuilder.append(" (C1: ").append(stats.getCountC1()).append(S_CLOSE_PARENTHESES);
		compiledStatsBuilder.append(" (C2: ").append(stats.getCountC2()).append(S_CLOSE_PARENTHESES);
		compiledStatsBuilder.append(" (C2N: ").append(stats.getCountC2N()).append(S_CLOSE_PARENTHESES);
		compiledStatsBuilder.append(" (OSR: ").append(stats.getCountOSR()).append(S_CLOSE_PARENTHESES);

		setStrokeForText();
		gc.fillText(compiledStatsBuilder.toString(), fix(graphGapLeft), fix(2));
	}
}