/*
 * Copyright (c) 2018-2021 Chris Newland.
 * Licensed under https://github.com/chriswhocodes/jitwatch/blob/master/LICENSE-BSD
 * Instructions: https://github.com/chriswhocodes/jitwatch/wiki
 */
package com.chrisnewland.jitwatch.parser;

import static com.chrisnewland.jitwatch.core.JITWatchConstants.ATTR_COMPILER;
import static com.chrisnewland.jitwatch.core.JITWatchConstants.ATTR_COMPILE_ID;
import static com.chrisnewland.jitwatch.core.JITWatchConstants.ATTR_COMPILE_KIND;
import static com.chrisnewland.jitwatch.core.JITWatchConstants.ATTR_FREE_CODE_CACHE;
import static com.chrisnewland.jitwatch.core.JITWatchConstants.ATTR_METHOD;
import static com.chrisnewland.jitwatch.core.JITWatchConstants.ATTR_NMSIZE;
import static com.chrisnewland.jitwatch.core.JITWatchConstants.ATTR_STAMP;
import static com.chrisnewland.jitwatch.core.JITWatchConstants.ATTR_STAMP_COMPLETED;
import static com.chrisnewland.jitwatch.core.JITWatchConstants.C1;
import static com.chrisnewland.jitwatch.core.JITWatchConstants.C2;
import static com.chrisnewland.jitwatch.core.JITWatchConstants.C2N;
import static com.chrisnewland.jitwatch.core.JITWatchConstants.C_QUOTE;
import static com.chrisnewland.jitwatch.core.JITWatchConstants.C_SPACE;
import static com.chrisnewland.jitwatch.core.JITWatchConstants.DEBUG_LOGGING;
import static com.chrisnewland.jitwatch.core.JITWatchConstants.J9;
import static com.chrisnewland.jitwatch.core.JITWatchConstants.ZING;
import static com.chrisnewland.jitwatch.core.JITWatchConstants.FALCON;
import static com.chrisnewland.jitwatch.core.JITWatchConstants.JVMCI;
import static com.chrisnewland.jitwatch.core.JITWatchConstants.S_DOT;
import static com.chrisnewland.jitwatch.core.JITWatchConstants.S_SLASH;
import static com.chrisnewland.jitwatch.core.JITWatchConstants.TAG_CODE_CACHE;
import static com.chrisnewland.jitwatch.core.JITWatchConstants.TAG_TASK;
import static com.chrisnewland.jitwatch.core.JITWatchConstants.TAG_TASK_DONE;

import java.io.File;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.chrisnewland.jitwatch.core.IJITListener;
import com.chrisnewland.jitwatch.core.JITWatchConfig;
import com.chrisnewland.jitwatch.core.TagProcessor;
import com.chrisnewland.jitwatch.model.CodeCacheEvent;
import com.chrisnewland.jitwatch.model.CodeCacheEvent.CodeCacheEventType;
import com.chrisnewland.jitwatch.model.Compilation;
import com.chrisnewland.jitwatch.model.CompilerThread;
import com.chrisnewland.jitwatch.model.EventType;
import com.chrisnewland.jitwatch.model.IMetaMember;
import com.chrisnewland.jitwatch.model.JITDataModel;
import com.chrisnewland.jitwatch.model.JITEvent;
import com.chrisnewland.jitwatch.model.LogParseException;
import com.chrisnewland.jitwatch.model.MetaClass;
import com.chrisnewland.jitwatch.model.ParsedClasspath;
import com.chrisnewland.jitwatch.model.SplitLog;
import com.chrisnewland.jitwatch.model.Tag;
import com.chrisnewland.jitwatch.model.Task;
import com.chrisnewland.jitwatch.util.ClassUtil;
import com.chrisnewland.jitwatch.util.ParseUtil;
import com.chrisnewland.freelogj.Logger;
import com.chrisnewland.freelogj.LoggerFactory;

public abstract class AbstractLogParser implements ILogParser
{
	protected static final Logger logger = LoggerFactory.getLogger(AbstractLogParser.class);

	protected JITDataModel model;

	protected CompilerThread currentCompilerThread = null;

	protected String vmCommand = null;

	protected boolean reading = false;

	protected boolean hasParseError = false;
	protected String errorDialogTitle;
	protected String errorDialogBody;

	protected IMetaMember currentMember = null;

	protected IJITListener jitListener = null;
	protected ILogParseErrorListener errorListener = null;

	protected boolean inHeader = false;

	protected long parseLineNumber;
	protected long processLineNumber;

	protected JITWatchConfig config = new JITWatchConfig();

	protected TagProcessor tagProcessor;

	protected SplitLog splitLog = new SplitLog();

	public AbstractLogParser(IJITListener jitListener)
	{
		model = new JITDataModel();

		this.jitListener = jitListener;
	}

	@Override
	public void setConfig(JITWatchConfig config)
	{
		this.config = config;
	}

	@Override
	public JITWatchConfig getConfig()
	{
		return config;
	}

	@Override
	public SplitLog getSplitLog()
	{
		return splitLog;
	}

	@Override
	public ParsedClasspath getParsedClasspath()
	{
		return config.getParsedClasspath();
	}

	protected void configureDisposableClassLoader()
	{
		if (DEBUG_LOGGING)
		{
			logger.debug("configureDisposableClassLoader()");
		}

		List<String> configuredClassLocations = config.getConfiguredClassLocations();
		List<String> parsedClassLocations = getParsedClasspath().getClassLocations();

		int configuredClasspathCount = configuredClassLocations.size();
		int parsedClasspathCount = parsedClassLocations.size();

		List<URL> classpathURLList = new ArrayList<>(configuredClasspathCount + parsedClasspathCount);

		for (String filename : configuredClassLocations)
		{
			URI uri = new File(filename).toURI();

			jitListener.handleLogEntry("Adding configured classpath: " + uri.toString());

			if (DEBUG_LOGGING)
			{
				logger.debug("adding to classpath {}", uri.toString());
			}

			try
			{
				classpathURLList.add(uri.toURL());
			}
			catch (MalformedURLException e)
			{
				logger.error("Could not create URL: {} ", uri, e);
			}
		}

		for (String filename : getParsedClasspath().getClassLocations())
		{
			if (!configuredClassLocations.contains(filename))
			{
				URI uri = new File(filename).toURI();

				jitListener.handleLogEntry("Adding parsed classpath: " + uri.toString());

				try
				{
					classpathURLList.add(uri.toURL());
				}
				catch (MalformedURLException e)
				{
					logger.error("Could not create URL: {} ", uri, e);
				}
			}
		}

		ClassUtil.initialise(classpathURLList);
	}

	protected void logEvent(JITEvent event)
	{
		if (jitListener != null)
		{
			jitListener.handleJITEvent(event);
		}
	}

	protected void logError(String entry)
	{
		if (jitListener != null)
		{
			jitListener.handleErrorEntry(entry);
		}
	}

	@Override
	public JITDataModel getModel()
	{
		return model;
	}

	@Override
	public void reset()
	{
		if (DEBUG_LOGGING)
		{
			logger.debug("Log parser reset()");
		}

		ClassUtil.clear();

		getModel().reset();

		splitLog.clear();

		hasParseError = false;
		errorDialogTitle = null;
		errorDialogBody = null;

		reading = false;

		inHeader = false;

		currentMember = null;

		vmCommand = null;

		parseLineNumber = 0;
		processLineNumber = 0;

		tagProcessor = new TagProcessor();
	}

	@Override
	public void stopParsing()
	{
		reading = false;
	}

	public IMetaMember findMemberWithSignature(String logSignature)
	{
		IMetaMember result = null;

		try
		{
			result = ParseUtil.findMemberWithSignature(model, logSignature);
		}
		catch (LogParseException ex)
		{
			if (DEBUG_LOGGING)
			{
				logger.debug("Could not parse signature: {}", logSignature);
				logger.debug("Exception was {}", ex.getMessage());
			}

			logError("Could not parse line " + processLineNumber + " : " + logSignature + " : " + ex.getMessage());
		}

		return result;
	}

	@Override
	public boolean hasParseError()
	{
		return hasParseError;
	}

	@Override
	public String getVMCommand()
	{
		return vmCommand;
	}

	@Override
	public void discardParsedLogs()
	{
		splitLog.clear();
		splitLog = new SplitLog();
	}

	protected void addToClassModel(String fqClassName)
	{
		if (DEBUG_LOGGING)
		{
			logger.debug("addToClassModel {}", fqClassName);
		}

		Class<?> clazz = null;

		MetaClass metaClass = model.getPackageManager().getMetaClass(fqClassName);

		if (metaClass != null)
		{
			return;
		}

		try
		{
			clazz = ClassUtil.loadClassWithoutInitialising(fqClassName);

			if (clazz != null)
			{
				model.buildAndGetMetaClass(clazz);
			}
		}
		catch (ClassNotFoundException cnf)
		{
			if (!ParseUtil.possibleLambdaMethod(fqClassName))
			{
				logError("ClassNotFoundException: '" + fqClassName + C_QUOTE);
			}
		}
		catch (NoClassDefFoundError ncdf)
		{
			logError("NoClassDefFoundError: '" + fqClassName + C_SPACE + "requires " + ncdf.getMessage() + C_QUOTE);
		}
		catch (UnsupportedClassVersionError ucve)
		{
			hasParseError = true;
			errorDialogTitle = "UnsupportedClassVersionError for class " + fqClassName;
			errorDialogBody = "Could not load " + fqClassName + " as the class file version is too recent for this JVM.";

			logError(
					"UnsupportedClassVersionError! Tried to load a class file with an unsupported format (later version than this JVM)");
			logger.error("Class file for {} created in a later JVM version", fqClassName, ucve);
		}
		catch (Throwable t)
		{
			// Possibly a VerifyError
			logger.error("Could not addClassToModel {}", fqClassName, t);
			logError("Exception: '" + fqClassName + C_QUOTE);
		}
	}

	private void logSplitStats()
	{
		logger.debug("Header lines        : {}", splitLog.getHeaderLines().size());
		logger.debug("ClassLoader lines   : {}", splitLog.getClassLoaderLines().size());
		logger.debug("LogCompilation lines: {}", splitLog.getCompilationLines().size());
		logger.debug("Assembly lines      : {}", splitLog.getAssemblyLines().size());
	}

	@Override
	public void processLogFile(Reader logFileReader, ILogParseErrorListener errorListener)
	{
		reset();

		configureDisposableClassLoader();

		// tell listener to reset any data
		jitListener.handleReadStart();

		this.errorListener = errorListener;

		splitLogFile(logFileReader);

		if (DEBUG_LOGGING)
		{
			logSplitStats();
		}

		parseLogFile();

		jitListener.handleReadComplete();
	}

	protected void handleTagQueued(Tag tag)
	{
		handleMethodLine(tag, EventType.QUEUE);
	}

	protected void handleTagNMethod(Tag tag)
	{
		Map<String, String> tagAttributes = tag.getAttributes();

		String attrCompiler = tagAttributes.get(ATTR_COMPILER);

		renameCompilationCompletedTimestamp(tag);

		if (attrCompiler != null && attrCompiler.length() > 0)
		{
			if (C1.equalsIgnoreCase(attrCompiler))
			{
				handleMethodLine(tag, EventType.NMETHOD_C1);
			}
			else if (C2.equalsIgnoreCase(attrCompiler))
			{
				handleMethodLine(tag, EventType.NMETHOD_C2);
			}
			else if (J9.equalsIgnoreCase(attrCompiler))
			{
				handleMethodLine(tag, EventType.NMETHOD_J9);
			}
			else if (ZING.equalsIgnoreCase(attrCompiler))
			{
				handleMethodLine(tag, EventType.NMETHOD_ZING);
			}
			else if (FALCON.equalsIgnoreCase(attrCompiler))
			{
				handleMethodLine(tag, EventType.NMETHOD_FALCON);
			}
			else if (JVMCI.equalsIgnoreCase(attrCompiler))
			{
				handleMethodLine(tag, EventType.NMETHOD_JVMCI);
			}
			else
			{
				logger.error("Unexpected Compiler attribute: {} {}", attrCompiler, tag.toString(true));
				logError("Unexpected Compiler attribute: " + attrCompiler);
			}
		}
		else
		{
			String attrCompileKind = tagAttributes.get(ATTR_COMPILE_KIND);

			if (attrCompileKind != null && C2N.equalsIgnoreCase(attrCompileKind))
			{
				handleMethodLine(tag, EventType.NMETHOD_C2N);
			}
			else
			{
				logError("Missing Compiler attribute " + tag);
			}
		}
	}

	protected void handleTagTask(Task task)
	{
		handleMethodLine(task, EventType.TASK);

		Tag tagCodeCache = task.getFirstNamedChild(TAG_CODE_CACHE);
		Tag tagTaskDone = task.getFirstNamedChild(TAG_TASK_DONE);

		if (tagTaskDone != null)
		{
			handleTaskDone(tagTaskDone, currentMember);

			if (tagCodeCache != null)
			{
				long stamp = ParseUtil.parseStampFromTag(tagTaskDone);
				long freeCodeCache = ParseUtil.parseLongAttributeFromTag(tagCodeCache, ATTR_FREE_CODE_CACHE);
				long nativeCodeSize = ParseUtil.parseLongAttributeFromTag(tagTaskDone, ATTR_NMSIZE);

				storeCodeCacheEventDetail(CodeCacheEventType.COMPILATION, stamp, nativeCodeSize, freeCodeCache);
			}
		}
		else
		{
			logger.error("{} not found in {}", TAG_TASK_DONE, task);
		}
	}

	protected void storeCodeCacheEvent(CodeCacheEventType eventType, Tag tag)
	{
		storeCodeCacheEventDetail(eventType, ParseUtil.parseStampFromTag(tag), 0, 0);
	}

	private void storeCodeCacheEventDetail(CodeCacheEventType eventType, long stamp, long nativeCodeSize, long freeCodeCache)
	{
		CodeCacheEvent codeCacheEvent = new CodeCacheEvent(eventType, stamp, nativeCodeSize, freeCodeCache);

		model.addCodeCacheEvent(codeCacheEvent);
	}

	private void handleMethodLine(Tag tag, EventType eventType)
	{
		Map<String, String> attrs = tag.getAttributes();

		String attrMethod = attrs.get(ATTR_METHOD);

		if (attrMethod != null)
		{
			attrMethod = attrMethod.replace(S_SLASH, S_DOT);

			handleMember(attrMethod, attrs, eventType, tag);
		}
	}

	private Compilation createCompilation(IMetaMember member)
	{
		int nextIndex = member.getCompilations().size();

		Compilation compilation = new Compilation(member, nextIndex);

		return compilation;
	}

	protected void setTagTaskQueued(Tag tagTaskQueued, IMetaMember metaMember)
	{
		Compilation compilation = createCompilation(metaMember);

		compilation.setTagTaskQueued(tagTaskQueued);

		metaMember.storeCompilation(compilation);
	}

	protected void setTagNMethod(Tag tagNMethod, IMetaMember member)
	{
		member.setCompiled(true);

		String compileID = tagNMethod.getAttributes().get(ATTR_COMPILE_ID);

		Compilation compilation = member.getCompilationByCompileID(compileID);

		if (compilation != null)
		{
			compilation.setTagNMethod(tagNMethod);
		}
		else
		{
			// check if C2N stub
			String compileKind = tagNMethod.getAttributes().get(ATTR_COMPILE_KIND);

			if (C2N.equalsIgnoreCase(compileKind))
			{
				compilation = createCompilation(member);

				compilation.setTagNMethod(tagNMethod);

				member.storeCompilation(compilation);
			}
			else
			{
				logger.warn("Didn't find compilation with ID {}", compileID);
			}
		}

		member.getMetaClass().getPackage().setHasCompiledClasses();
	}

	protected void setTagTask(Task tagTask, IMetaMember member)
	{
		String compileID = tagTask.getAttributes().get(ATTR_COMPILE_ID);

		Compilation compilation = member.getCompilationByCompileID(compileID);

		if (compilation != null)
		{
			compilation.setTagTask(tagTask);

			if (currentCompilerThread != null)
			{
				currentCompilerThread.addCompilation(compilation);
			}
			else
			{
				logger.warn("No compiler thread set when I saw {}", tagTask.toString());
			}
		}
		else
		{
			logger.warn("Didn't find compilation with ID {}", compileID);
		}
	}

	private void setTagTaskDone(String compileID, Tag tagTaskDone, IMetaMember member)
	{
		Compilation compilation = member.getCompilationByCompileID(compileID);

		if (compilation != null)
		{
			compilation.setTagTaskDone(tagTaskDone);
		}
		else
		{
			logger.warn("Didn't find compilation with ID {}", compileID);
		}
	}

	private void handleMember(String signature, Map<String, String> attrs, EventType type, Tag tag)
	{
		IMetaMember metaMember = findMemberWithSignature(signature);

		long stampTime = ParseUtil.getStamp(attrs);

		if (metaMember != null)
		{
			switch (type)
			{
			case QUEUE:
			{
				setTagTaskQueued(tag, metaMember);

				JITEvent queuedEvent = new JITEvent(stampTime, type, metaMember);
				model.addEvent(queuedEvent);
				logEvent(queuedEvent);
			}
				break;
			case NMETHOD_C1:
			case NMETHOD_C2:
			case NMETHOD_C2N:
			case NMETHOD_J9:
			case NMETHOD_ZING:
			case NMETHOD_FALCON:
			case NMETHOD_JVMCI:
			{
				setTagNMethod(tag, metaMember);
				metaMember.getMetaClass().incCompiledMethodCount();

				JITEvent compiledEvent = new JITEvent(stampTime, type, metaMember);
				model.addEvent(compiledEvent);
				logEvent(compiledEvent);
			}
				break;
			case TASK:
			{
				setTagTask((Task) tag, metaMember);
				currentMember = metaMember;
				model.updateStats(metaMember, attrs);
			}
				break;
			default:
				break;
			}
		}
	}

	protected void handleTaskDone(Tag tagTaskDone, IMetaMember member)
	{
		Map<String, String> attrs = tagTaskDone.getAttributes();

		if (attrs.containsKey(ATTR_NMSIZE))
		{
			long nmsize = Long.parseLong(attrs.get(ATTR_NMSIZE));
			model.addNativeBytes(nmsize);
		}

		if (member != null)
		{
			Tag parent = tagTaskDone.getParent();

			String compileID = null;

			if (TAG_TASK.equals(parent.getName()))
			{
				compileID = parent.getAttributes().get(ATTR_COMPILE_ID);

				if (compileID != null)
				{
					setTagTaskDone(compileID, tagTaskDone, member);
				}
				else
				{
					logger.warn("No compile_id attribute found on task");
				}
			}
			else
			{
				logger.warn("Unexpected parent of task_done: {}", parent.getName());
			}

			// prevents attr overwrite by next task_done if next member not
			// found due to classpath issues
			currentMember = null;
		}
	}

	private void renameCompilationCompletedTimestamp(Tag tag)
	{
		String compilationCompletedStamp = tag.getAttributes().get(ATTR_STAMP);

		if (compilationCompletedStamp != null)
		{
			tag.getAttributes().remove(ATTR_STAMP);
			tag.getAttributes().put(ATTR_STAMP_COMPLETED, compilationCompletedStamp);
		}
	}

	protected abstract void parseLogFile();

	protected abstract void splitLogFile(Reader logFileReader);

	protected abstract void handleTag(Tag tag);
}
