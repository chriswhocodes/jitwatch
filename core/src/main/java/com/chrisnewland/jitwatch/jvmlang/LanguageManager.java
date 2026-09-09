/*
 * Copyright (c) 2013-2015 Chris Newland.
 * Licensed under https://github.com/chriswhocodes/jitwatch/blob/master/LICENSE-BSD
 * Instructions: https://github.com/chriswhocodes/jitwatch/wiki
 */
package com.chrisnewland.jitwatch.jvmlang;

import static com.chrisnewland.jitwatch.core.JITWatchConstants.C_DOT;
import static com.chrisnewland.jitwatch.core.JITWatchConstants.VM_LANGUAGE_CLOJURE;
import static com.chrisnewland.jitwatch.core.JITWatchConstants.VM_LANGUAGE_GROOVY;
import static com.chrisnewland.jitwatch.core.JITWatchConstants.VM_LANGUAGE_JAVA;
import static com.chrisnewland.jitwatch.core.JITWatchConstants.VM_LANGUAGE_JAVASCRIPT;
import static com.chrisnewland.jitwatch.core.JITWatchConstants.VM_LANGUAGE_JRUBY;
import static com.chrisnewland.jitwatch.core.JITWatchConstants.VM_LANGUAGE_KOTLIN;
import static com.chrisnewland.jitwatch.core.JITWatchConstants.VM_LANGUAGE_SCALA;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

import com.chrisnewland.jitwatch.core.JITWatchConfig;
import com.chrisnewland.jitwatch.logger.ILogListener;
import com.chrisnewland.jitwatch.process.compiler.CompilerGroovy;
import com.chrisnewland.jitwatch.process.compiler.CompilerJRuby;
import com.chrisnewland.jitwatch.process.compiler.CompilerJava;
import com.chrisnewland.jitwatch.process.compiler.CompilerJavaScript;
import com.chrisnewland.jitwatch.process.compiler.CompilerKotlin;
import com.chrisnewland.jitwatch.process.compiler.CompilerScala;
import com.chrisnewland.jitwatch.process.compiler.ICompiler;
import com.chrisnewland.jitwatch.process.runtime.IRuntime;
import com.chrisnewland.jitwatch.process.runtime.RuntimeGroovy;
import com.chrisnewland.jitwatch.process.runtime.RuntimeJRuby;
import com.chrisnewland.jitwatch.process.runtime.RuntimeJava;
import com.chrisnewland.jitwatch.process.runtime.RuntimeJavaScript;
import com.chrisnewland.jitwatch.process.runtime.RuntimeKotlin;
import com.chrisnewland.jitwatch.process.runtime.RuntimeScala;

public class LanguageManager
{
	private JITWatchConfig config;
	private ILogListener logListener;

	public LanguageManager(JITWatchConfig config, ILogListener logListener)
	{
		this.config = config;
		this.logListener = logListener;
	}

	public ICompiler getCompiler(String vmLanguage, String languageHomeDir)
	{
		ICompiler result = null;

		if (languageHomeDir != null)
		{
			try
			{
				switch (vmLanguage)
				{
				case VM_LANGUAGE_CLOJURE:
					break;
				case VM_LANGUAGE_GROOVY:
					result = new CompilerGroovy(languageHomeDir);
					break;
				case VM_LANGUAGE_JAVA:
					result = new CompilerJava(languageHomeDir);
					break;
				case VM_LANGUAGE_JAVASCRIPT:
					result = new CompilerJavaScript(languageHomeDir);
					break;
				case VM_LANGUAGE_JRUBY:
					result = new CompilerJRuby(languageHomeDir);
					break;
				case VM_LANGUAGE_KOTLIN:
					result = new CompilerKotlin(languageHomeDir);
					break;
				case VM_LANGUAGE_SCALA:
					result = new CompilerScala(languageHomeDir);
					break;
				}
			}
			catch (FileNotFoundException fnfe)
			{
				logListener.handleErrorEntry("Could not find " + vmLanguage + " compiler in '" + languageHomeDir + "'");
			}
		}

		return result;
	}

	public IRuntime getRuntime(String vmLanguage, String languageHomeDir)
	{
		IRuntime result = null;

		if (languageHomeDir != null)
		{
			try
			{
				switch (vmLanguage)
				{
				case VM_LANGUAGE_CLOJURE:
					break;
				case VM_LANGUAGE_GROOVY:
					result = new RuntimeGroovy(languageHomeDir);
					break;
				case VM_LANGUAGE_JAVA:
					result = new RuntimeJava(languageHomeDir);
					break;
				case VM_LANGUAGE_JAVASCRIPT:
					result = new RuntimeJavaScript(languageHomeDir);
					break;
				case VM_LANGUAGE_JRUBY:
					result = new RuntimeJRuby(languageHomeDir);
					break;
				case VM_LANGUAGE_KOTLIN:
					result = new RuntimeKotlin(languageHomeDir);
					break;
				case VM_LANGUAGE_SCALA:
					result = new RuntimeScala(languageHomeDir);
					break;
				}
			}
			catch (FileNotFoundException fnfe)
			{
				logListener.handleErrorEntry("Could not find " + vmLanguage + " runtime in '" + languageHomeDir + "'");
			}
		}

		return result;
	}

	public static boolean isCompilable(String language, File sourceFile)
	{
		return language.equals(getLanguageFromFile(sourceFile));
	}

	public static String getFileExtension(File file)
	{
		String result = null;

		if (file != null)
		{
			String filename = file.getName();

			int lastDotPos = filename.lastIndexOf(C_DOT);

			if (lastDotPos != -1)
			{
				result = filename.substring(lastDotPos + 1);
			}
		}

		return result;
	}

	public static String getLanguageFromFile(File sourceFile)
	{
		String result = null;

		String fileExtension = getFileExtension(sourceFile);

		if (fileExtension != null)
		{
			fileExtension = fileExtension.toLowerCase();

			switch (fileExtension)
			{
			case "java":
				result = VM_LANGUAGE_JAVA;
				break;
			case "scala":
				result = VM_LANGUAGE_SCALA;
				break;
			case "rb":
				result = VM_LANGUAGE_JRUBY;
				break;
			case "js":
				result = VM_LANGUAGE_JAVASCRIPT;
				break;
			case "kt":
				result = VM_LANGUAGE_KOTLIN;
				break;
			case "groovy":
			case "gvy":
			case "gy":
				result = VM_LANGUAGE_GROOVY;
				break;
			case "clj":
				result = VM_LANGUAGE_CLOJURE;
				break;
			}
		}

		return result;
	}

	public static List<String> getKnownFilenameExtensions()
	{
		List<String> result = new ArrayList<>();

		result.add("java");
		result.add("scala");
		result.add("rb");
		result.add("js");
		result.add("kt");
		result.add("groovy");
		result.add("gvy");
		result.add("gy");
		result.add("clj");

		return result;
	}

	public static boolean isLanguageEnabled(String vmLanguage)
	{
		switch (vmLanguage)
		{
		case VM_LANGUAGE_CLOJURE:
			return false;
		case VM_LANGUAGE_GROOVY:
			return true;
		case VM_LANGUAGE_JAVA:
			return true;
		case VM_LANGUAGE_JAVASCRIPT:
			return true;
		case VM_LANGUAGE_JRUBY:
			return false;
		case VM_LANGUAGE_KOTLIN:
			return true;
		case VM_LANGUAGE_SCALA:
			return true;
		default:
			return false;
		}
	}

}