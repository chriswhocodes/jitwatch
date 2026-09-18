/*
 * Copyright (c) 2013-2021 Chris Newland.
 * Licensed under https://github.com/chriswhocodes/jitwatch/blob/master/LICENSE-BSD
 * Instructions: https://github.com/chriswhocodes/jitwatch/wiki
 */
package com.chrisnewland.jitwatch.sandbox;

import static com.chrisnewland.jitwatch.core.JITWatchConstants.S_DOLLAR;
import static com.chrisnewland.jitwatch.core.JITWatchConstants.S_EMPTY;
import static com.chrisnewland.jitwatch.core.JITWatchConstants.S_SPACE;
import static com.chrisnewland.jitwatch.core.JITWatchConstants.VM_LANGUAGE_JAVA;
import static com.chrisnewland.jitwatch.core.JITWatchConstants.VM_LANGUAGE_JAVASCRIPT;
import static com.chrisnewland.jitwatch.core.JITWatchConstants.VM_LANGUAGE_SCALA;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import com.chrisnewland.jitwatch.core.JITWatchConfig;
import com.chrisnewland.jitwatch.core.JITWatchConfig.BackgroundCompilation;
import com.chrisnewland.jitwatch.core.JITWatchConfig.CompressedOops;
import com.chrisnewland.jitwatch.core.JITWatchConfig.OnStackReplacement;
import com.chrisnewland.jitwatch.core.JITWatchConfig.TieredCompilation;
import com.chrisnewland.jitwatch.core.JITWatchConstants;
import com.chrisnewland.jitwatch.jvmlang.LanguageManager;
import com.chrisnewland.jitwatch.logger.ILogListener;
import com.chrisnewland.jitwatch.model.IMetaMember;
import com.chrisnewland.jitwatch.model.IReadOnlyJITDataModel;
import com.chrisnewland.jitwatch.model.MetaClass;
import com.chrisnewland.jitwatch.parser.ILogParser;
import com.chrisnewland.jitwatch.process.IExternalProcess;
import com.chrisnewland.jitwatch.process.compiler.ICompiler;
import com.chrisnewland.jitwatch.process.runtime.IRuntime;
import com.chrisnewland.jitwatch.ui.sandbox.ISandboxStage;
import com.chrisnewland.jitwatch.util.DisassemblyUtil;
import com.chrisnewland.jitwatch.util.FileUtil;
import com.chrisnewland.jitwatch.util.StringUtil;
import com.chrisnewland.freelogj.Logger;
import com.chrisnewland.freelogj.LoggerFactory;

public class Sandbox
{
	private static final Logger logger = LoggerFactory.getLogger(Sandbox.class);

	private ILogListener logListener;
	private ISandboxStage sandboxStage;

	public static final Path SANDBOX_DIR;
	public static final Path SANDBOX_SOURCE_DIR;
	public static final Path SANDBOX_CLASS_DIR;

	public static final Path PATH_STD_ERR;
	public static final Path PATH_STD_OUT;

	private static final String SANDBOX_LOGFILE = "sandbox.log";
	
	private static final String LAMBDA_PROXY_DUMP_DIRNAME = "lambda-proxy-dump";

	private static final String JDK21_LAMBDA_DUMP_DIRNAME = "DUMP_LAMBDA_PROXY_CLASS_FILES";
	private static final String JDK21_HIDDEN_DUMP_DIRNAME = "DUMP_CLASS_FILES";

	private File sandboxLogFile = new File(SANDBOX_DIR.toFile(), SANDBOX_LOGFILE);
	
	// Set during executeClass(); read in runJITWatch() to register dump dirs.
	private Path effectiveDumpWorkingDir = null;
	private int lastJdkMajor = 0;

	private ILogParser logParser;

	private LanguageManager languageManager;

	private IExternalProcess lastProcess;

	static
	{
		String userDir = System.getProperty("user.dir");

		SANDBOX_DIR = Paths.get(userDir, "sandbox");
		SANDBOX_SOURCE_DIR = Paths.get(SANDBOX_DIR.toString(), "sources");
		SANDBOX_CLASS_DIR = Paths.get(SANDBOX_DIR.toString(), "classes");

		PATH_STD_ERR = new File(Sandbox.SANDBOX_DIR.toFile(), "sandbox.err").toPath();
		PATH_STD_OUT = new File(Sandbox.SANDBOX_DIR.toFile(), "sandbox.out").toPath();

		initialise();
	}

	private static void initialise()
	{
		File sandboxSources = SANDBOX_SOURCE_DIR.toFile();

		if (!sandboxSources.exists())
		{
			logger.info("Creating Sandbox source directory {}", sandboxSources);

			sandboxSources.mkdirs();

			if (sandboxSources.exists())
			{
				copyExamples();
			}
		}

		File sandboxClasses = SANDBOX_CLASS_DIR.toFile();

		if (!sandboxClasses.exists())
		{
			sandboxClasses.mkdirs();
		}
	}

	public void reset()
	{
		logger.info("Resetting Sandbox to default settings");
		FileUtil.emptyDir(SANDBOX_DIR.toFile());
		initialise();
	}

	private static void copyExamples()
	{
		boolean runningFromJar = FileUtil.isRunningFromJar();

		logger.info("isRunningFromJar:{}", runningFromJar);

		File dstDir = SANDBOX_SOURCE_DIR.toFile();

		logger.info("Copying Sandbox examples to {}", dstDir);

		if (runningFromJar)
		{
			FileUtil.copyFilesFromJarToDir("examples", dstDir);
		}
		else
		{
			File srcDir = new File("core/src/main/resources/examples");

			FileUtil.copyFilesToDir(srcDir, dstDir);
		}
	}

	public Sandbox(ILogParser parser, ILogListener logger, ISandboxStage sandboxStage)
	{
		this.logParser = parser;
		this.logListener = logger;
		this.sandboxStage = sandboxStage;

		languageManager = new LanguageManager(logParser.getConfig(), logListener);
	}

	public void runSandbox(String language, List<File> compileList, File fileToRun) throws Exception
	{
		logListener.handleLogEntry("Running Sandbox");
		logListener.handleLogEntry("Language is " + language);

		String languagePath = logParser.getConfig().getVMLanguagePath(language);

		if (S_EMPTY.equals(languagePath) && (VM_LANGUAGE_JAVA.equals(language) || VM_LANGUAGE_JAVASCRIPT.equals(language)))
		{
			languagePath = System.getProperty("java.home");

			logListener.handleLogEntry("Using runtime JVM for " + language);
		}

		logListener.handleLogEntry(language + " home dir: " + languagePath);

		ICompiler compiler = languageManager.getCompiler(language, languagePath);

		if (compiler == null)
		{
			logListener.handleErrorEntry(language + " compiler path not set. Please click Configure Sandbox and set up the path.");
			return;
		}

		IRuntime runtime = languageManager.getRuntime(language, languagePath);

		if (runtime == null)
		{
			logListener.handleErrorEntry(language + " runtime path not set. Please click Configure Sandbox and set up the path.");
			return;
		}

		logListener.handleLogEntry("Compiling: " + StringUtil.listToString(compileList));

		lastProcess = compiler;

		String compilationSwitchesText = logParser.getConfig().getExtraVMCompilationSwitches().trim();

		List<String> compilationSwitches = new ArrayList<>();

		if (!compilationSwitchesText.isEmpty())
		{
			compilationSwitches.addAll(Arrays.asList(compilationSwitchesText.split("\\s+")));
		}

		boolean compiledOK = compiler.compile(compileList, buildUniqueClasspath(logParser.getConfig()), compilationSwitches, SANDBOX_CLASS_DIR.toFile(),
				Collections.<String, String>emptyMap(), logListener);

		logListener.handleLogEntry("Compilation success: " + compiledOK);

		if (compiledOK)
		{
			String fqClassNameToRun = runtime.getClassToExecute(fileToRun);

			lastProcess = runtime;

			long start = System.currentTimeMillis();

			boolean executionSuccess = executeClass(fqClassNameToRun, runtime, logParser.getConfig().isSandboxIntelMode(), languagePath);

			long stop = System.currentTimeMillis();

			logListener.handleLogEntry("Execution success: " + executionSuccess + " in " + (stop - start) + "ms");

			if (executionSuccess)
			{
				runJITWatch();

				if (!logParser.hasParseError())
				{
					String fqClassNameForTriView = runtime.getClassForTriView(fileToRun);

					showTriView(language, fqClassNameForTriView);
				}
			}
			else
			{
				sandboxStage.showError(runtime.getErrorStream());
			}
		}
		else
		{
			sandboxStage.showError(compiler.getErrorStream());
		}
	}

	public IExternalProcess getLastProcess()
	{
		return lastProcess;
	}

	private List<String> buildUniqueClasspath(JITWatchConfig config)
	{
		List<String> classpath = new ArrayList<>();

		classpath.add(SANDBOX_CLASS_DIR.toString());

		for (String path : config.getConfiguredClassLocations())
		{
			if (!classpath.contains(path))
			{
				classpath.add(path);
			}
		}

		return classpath;
	}

	private boolean executeClass(String fqClassName, IRuntime runtime, boolean intelMode, String jdkHome) throws Exception
	{
		List<String> classpath = buildUniqueClasspath(logParser.getConfig());

		List<String> options = new ArrayList<>();
		options.add("-XX:+UnlockDiagnosticVMOptions");
		//options.add("-XX:+TraceClassLoading");
		options.add("-XX:+LogCompilation");
		options.add("-XX:LogFile=" + sandboxLogFile.getCanonicalPath());

		if (logParser.getConfig().isPrintAssembly())
		{
			options.add("-XX:+PrintAssembly");

			if (intelMode)
			{
				options.add("-XX:PrintAssemblyOptions=intel");
			}
		}

		boolean isDisableInlining = logParser.getConfig().isDisableInlining();

		if (isDisableInlining)
		{
			options.add("-XX:-Inline");
		}

		TieredCompilation tieredMode = logParser.getConfig().getTieredCompilationMode();

		if (tieredMode == TieredCompilation.FORCE_TIERED)
		{
			options.add("-XX:+TieredCompilation");
		}
		else if (tieredMode == TieredCompilation.FORCE_NO_TIERED)
		{
			options.add("-XX:-TieredCompilation");
		}

		CompressedOops oopsMode = logParser.getConfig().getCompressedOopsMode();

		if (oopsMode == CompressedOops.FORCE_COMPRESSED)
		{
			options.add("-XX:+UseCompressedOops");
		}
		else if (oopsMode == CompressedOops.FORCE_NO_COMPRESSED)
		{
			options.add("-XX:-UseCompressedOops");
		}

		BackgroundCompilation backgroundCompilationMode = logParser.getConfig().getBackgroundCompilationMode();

		if (backgroundCompilationMode == BackgroundCompilation.FORCE_BACKGROUND_COMPILATION)
		{
			options.add("-XX:+BackgroundCompilation");
		}
		else if (backgroundCompilationMode == BackgroundCompilation.FORCE_NO_BACKGROUND_COMPILATION)
		{
			options.add("-XX:-BackgroundCompilation");
		}

		OnStackReplacement onStackReplacementMode = logParser.getConfig().getOnStackReplacementMode();

		if (onStackReplacementMode == OnStackReplacement.FORCE_ON_STACK_REPLACEMENT)
		{
			options.add("-XX:+UseOnStackReplacement");
		}
		else if (onStackReplacementMode == OnStackReplacement.FORCE_NO_ON_STACK_REPLACEMENT)
		{
			options.add("-XX:-UseOnStackReplacement");
		}

		if (!isDisableInlining && logParser.getConfig().getFreqInlineSize() != JITWatchConstants.DEFAULT_FREQ_INLINE_SIZE)
		{
			options.add("-XX:FreqInlineSize=" + logParser.getConfig().getFreqInlineSize());
		}

		if (!isDisableInlining && logParser.getConfig().getMaxInlineSize() != JITWatchConstants.DEFAULT_MAX_INLINE_SIZE)
		{
			options.add("-XX:MaxInlineSize=" + logParser.getConfig().getMaxInlineSize());
		}

		if (logParser.getConfig().getCompileThreshold() != JITWatchConstants.DEFAULT_COMPILER_THRESHOLD)
		{
			options.add("-XX:CompileThreshold=" + logParser.getConfig().getCompileThreshold());
		}

		if (logParser.getConfig().getExtraVMRuntimeSwitches().length() > 0)
		{
			String extraSwitchString = logParser.getConfig().getExtraVMRuntimeSwitches();
			String[] switches = extraSwitchString.split(S_SPACE);

			for (String sw : switches)
			{
				options.add(sw);
			}
		}

		logListener.handleLogEntry("Executing: " + fqClassName);
		logListener.handleLogEntry("Classpath: " + StringUtil.listToString(classpath, File.pathSeparatorChar));
		logListener.handleLogEntry("VM options: " + StringUtil.listToString(options));

		Map<String, String> environment = new LinkedHashMap<>();

		if (DisassemblyUtil.downloadedDisassemblerPresent())
		{
			environment.put(DisassemblyUtil.getDynamicLibraryPath(),
					Paths.get(DisassemblyUtil.getDisassemblerFilename()).toAbsolutePath().getParent().toString());
		}

		Path workingDirPath = null;

		if (!logParser.getConfig().getSandboxWorkingDir().isEmpty())
		{
			workingDirPath = Paths.get(logParser.getConfig().getSandboxWorkingDir());
		}
		
		if (logParser.getConfig().isCaptureDynamicClasses())
		{
			lastJdkMajor = detectJdkMajorVersion(jdkHome);

			if (workingDirPath == null)
			{
				workingDirPath = SANDBOX_DIR;
			}

			effectiveDumpWorkingDir = workingDirPath;

			if (lastJdkMajor >= 21)
			{
				options.add("-Djdk.invoke.LambdaMetafactory.dumpProxyClassFiles");
				options.add("-Djdk.invoke.MethodHandle.dumpClassFiles");
			}
			else
			{
				File lambdaDumpDir = new File(SANDBOX_DIR.toFile(), LAMBDA_PROXY_DUMP_DIRNAME);
				lambdaDumpDir.mkdirs();
				options.add("-Djdk.internal.lambda.dumpProxyClasses=" + lambdaDumpDir.getAbsolutePath());
				// -------------------------------------------------------------------------------------
				// NB: once ClassAct gets integrated, the classes dumped by way of this flag  are from 
				//     `java.lang.invoke` so any attempt at traditional ClassLoader loading throws 
				//     SecurityException.  ClassAct employment should(?) be able to glean their contents 
				//     for MetaClass creation and respective user interface participation.
				options.add("-Djava.lang.invoke.MethodHandle.DUMP_CLASS_FILES=true");
			}

		}
		else
		{
			effectiveDumpWorkingDir = null;
			lastJdkMajor = 0;
		}

		return runtime.execute(workingDirPath, fqClassName, classpath, options, environment, logListener);
	}
	
	private int detectJdkMajorVersion(String jdkHome)
	{
		File releaseFile = new File(jdkHome, "release");

		if (releaseFile.exists())
		{
			try (BufferedReader reader = new BufferedReader(new FileReader(releaseFile)))
			{
				String line;

				while ((line = reader.readLine()) != null)
				{
					if (line.startsWith("JAVA_VERSION="))
					{
						String version = line.substring("JAVA_VERSION=".length()).replace("\"", "").trim();
						String[] parts = version.split("\\.");
						int first = Integer.parseInt(parts[0]);

						if (first == 1 && parts.length > 1)
						{
							return Integer.parseInt(parts[1]);
						}

						return first;
					}
				}
			}
			catch (Exception e)
			{
				logger.warn("Could not read JDK release file at {}: {}", jdkHome, e.getMessage());
			}
		}

		String specVersion = System.getProperty("java.specification.version", "1.8");

		if (specVersion.startsWith("1."))
		{
			try 
			{ 
				return Integer.parseInt(specVersion.substring(2)); 
			} catch (NumberFormatException ignored) {}
		}
		else
		{
			try 
			{ 
				return Integer.parseInt(specVersion); 
			} catch (NumberFormatException ignored) {}
		}

		return 8;
	}

	private void runJITWatch() throws IOException
	{
		JITWatchConfig config = logParser.getConfig();

		List<String> sourceLocations = new ArrayList<>(config.getSourceLocations());
		List<String> classLocations = new ArrayList<>(config.getConfiguredClassLocations());

		String sandboxSourceDirString = SANDBOX_SOURCE_DIR.toString();
		String sandboxClassDirString = SANDBOX_CLASS_DIR.toString();

		boolean configChanged = false;

		if (!sourceLocations.contains(sandboxSourceDirString))
		{
			configChanged = true;
			sourceLocations.add(sandboxSourceDirString);
		}

		if (!classLocations.contains(sandboxClassDirString))
		{
			configChanged = true;
			classLocations.add(sandboxClassDirString);
		}

		File jdkSrcZip = FileUtil.getJDKSourceZip();

		if (jdkSrcZip != null)
		{
			String jdkSourceZipString = jdkSrcZip.toPath().toString();

			if (!sourceLocations.contains(jdkSourceZipString))
			{
				configChanged = true;
				sourceLocations.add(jdkSourceZipString);
			}
		}
		
		if (config.isCaptureDynamicClasses() && effectiveDumpWorkingDir != null)
		{
			File workingDirDumpFile = effectiveDumpWorkingDir.toFile();
			if (lastJdkMajor >= 21)
			{
				String lambdaDump = new File(workingDirDumpFile, JDK21_LAMBDA_DUMP_DIRNAME).toString();
				String hiddenDump = new File(workingDirDumpFile, JDK21_HIDDEN_DUMP_DIRNAME).toString();

				if (!classLocations.contains(lambdaDump)) 
				{ 
					classLocations.add(lambdaDump); 
					configChanged = true; 
				}
				if (!classLocations.contains(hiddenDump)) 
				{ 
					classLocations.add(hiddenDump); 
					configChanged = true; 
				}
			}
			else
			{
				String lambdaDump = new File(SANDBOX_DIR.toFile(), LAMBDA_PROXY_DUMP_DIRNAME).toString();
				String hiddenDump = new File(workingDirDumpFile, JDK21_HIDDEN_DUMP_DIRNAME).toString();

				if (!classLocations.contains(lambdaDump)) 
				{ 
					classLocations.add(lambdaDump); configChanged = true; 
				}
				if (!classLocations.contains(hiddenDump)) 
				{ 
					classLocations.add(hiddenDump); configChanged = true; 
				}
			}
		}

		config.setSourceLocations(sourceLocations);
		config.setClassLocations(classLocations);

		if (configChanged)
		{
			config.saveConfig();
		}

		logListener.handleLogEntry("Parsing JIT log: " + sandboxLogFile.toString());

		logParser.processLogFile(sandboxLogFile, sandboxStage);

		logListener.handleLogEntry("Parsing complete");
	}

	private void showTriView(String language, String openClassInTriView)
	{
		IReadOnlyJITDataModel model = logParser.getModel();

		IMetaMember triViewMember = getMemberForClass(openClassInTriView, model);

		if (triViewMember == null && VM_LANGUAGE_SCALA.equals(language) && openClassInTriView.endsWith(S_DOLLAR))
		{
			// Scala and nothing found for Foo$ so try Foo
			triViewMember = getMemberForClass(openClassInTriView.substring(0, openClassInTriView.length() - 1), model);
		}

		sandboxStage.openTriView(triViewMember);
	}

	private IMetaMember getMemberForClass(String openClassInTriView, IReadOnlyJITDataModel model)
	{
		IMetaMember triViewMember = null;

		logListener.handleLogEntry("Looking up class: " + openClassInTriView);

		MetaClass metaClass = model.getPackageManager().getMetaClass(openClassInTriView);

		if (metaClass != null)
		{
			logListener.handleLogEntry("looking for compiled members of " + metaClass.getFullyQualifiedName());

			// select first compiled member if any
			List<IMetaMember> memberList = metaClass.getMetaMembers();

			for (IMetaMember mm : memberList)
			{
				logListener.handleLogEntry("Checking JIT compilation status of " + mm.toString());

				if (triViewMember == null)
				{
					// take the first member encountered
					triViewMember = mm;
				}

				if (mm.isCompiled())
				{
					// override with the first JIT-compiled member
					triViewMember = mm;
					break;
				}
			}
		}

		return triViewMember;
	}
}
