/*
 * Copyright (c) 2013-2019 Chris Newland.
 * Licensed under https://github.com/chriswhocodes/jitwatch/blob/master/LICENSE-BSD
 * Instructions: https://github.com/chriswhocodes/jitwatch/wiki
 */
package com.chrisnewland.jitwatch.parser.hotspot;

import static com.chrisnewland.jitwatch.core.JITWatchConstants.ATTR_NAME;
import static com.chrisnewland.jitwatch.core.JITWatchConstants.ATTR_THREAD;
import static com.chrisnewland.jitwatch.core.JITWatchConstants.ATTR_TIME_MS;
import static com.chrisnewland.jitwatch.core.JITWatchConstants.C_AT;
import static com.chrisnewland.jitwatch.core.JITWatchConstants.C_OPEN_ANGLE;
import static com.chrisnewland.jitwatch.core.JITWatchConstants.C_OPEN_SQUARE_BRACKET;
import static com.chrisnewland.jitwatch.core.JITWatchConstants.DEBUG_LOGGING;
import static com.chrisnewland.jitwatch.core.JITWatchConstants.DEBUG_LOGGING_ASSEMBLY;
import static com.chrisnewland.jitwatch.core.JITWatchConstants.LOADED;
import static com.chrisnewland.jitwatch.core.JITWatchConstants.SKIP_BODY_TAGS;
import static com.chrisnewland.jitwatch.core.JITWatchConstants.SKIP_HEADER_TAGS;
import static com.chrisnewland.jitwatch.core.JITWatchConstants.S_AT;
import static com.chrisnewland.jitwatch.core.JITWatchConstants.S_FILE_COLON;
import static com.chrisnewland.jitwatch.core.JITWatchConstants.S_OPEN_ANGLE;
import static com.chrisnewland.jitwatch.core.JITWatchConstants.S_SLASH;
import static com.chrisnewland.jitwatch.core.JITWatchConstants.S_SPACE;
import static com.chrisnewland.jitwatch.core.JITWatchConstants.TAG_CLOSE_CDATA;
import static com.chrisnewland.jitwatch.core.JITWatchConstants.TAG_CODE_CACHE_FULL;
import static com.chrisnewland.jitwatch.core.JITWatchConstants.TAG_COMMAND;
import static com.chrisnewland.jitwatch.core.JITWatchConstants.TAG_HOTSPOT_LOG;
import static com.chrisnewland.jitwatch.core.JITWatchConstants.TAG_HOTSPOT_LOG_DONE;
import static com.chrisnewland.jitwatch.core.JITWatchConstants.TAG_NMETHOD;
import static com.chrisnewland.jitwatch.core.JITWatchConstants.TAG_OPEN_CDATA;
import static com.chrisnewland.jitwatch.core.JITWatchConstants.TAG_OPEN_CLOSE_CDATA;
import static com.chrisnewland.jitwatch.core.JITWatchConstants.TAG_PRINT_NMETHOD;
import static com.chrisnewland.jitwatch.core.JITWatchConstants.TAG_START_COMPILE_THREAD;
import static com.chrisnewland.jitwatch.core.JITWatchConstants.TAG_SWEEPER;
import static com.chrisnewland.jitwatch.core.JITWatchConstants.TAG_TASK;
import static com.chrisnewland.jitwatch.core.JITWatchConstants.TAG_TASK_QUEUED;
import static com.chrisnewland.jitwatch.core.JITWatchConstants.TAG_TTY;
import static com.chrisnewland.jitwatch.core.JITWatchConstants.TAG_VM_ARGUMENTS;
import static com.chrisnewland.jitwatch.core.JITWatchConstants.TAG_VM_VERSION;
import static com.chrisnewland.jitwatch.core.JITWatchConstants.TAG_WRITER;
import static com.chrisnewland.jitwatch.core.JITWatchConstants.TAG_XML;
import static com.chrisnewland.jitwatch.core.JITWatchConstants.DEBUG_LOGGING;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.chrisnewland.jitwatch.core.IJITListener;
import com.chrisnewland.jitwatch.loader.ByteArrayClassLoader;
import com.chrisnewland.jitwatch.model.CodeCacheEvent.CodeCacheEventType;
import com.chrisnewland.jitwatch.model.LogParseException;
import com.chrisnewland.jitwatch.model.NumberedLine;
import com.chrisnewland.jitwatch.model.Tag;
import com.chrisnewland.jitwatch.model.Task;
import com.chrisnewland.jitwatch.model.assembly.Architecture;
import com.chrisnewland.jitwatch.model.assembly.AssemblyProcessor;
import com.chrisnewland.jitwatch.parser.AbstractLogParser;
import com.chrisnewland.jitwatch.util.ClassUtil;
import com.chrisnewland.jitwatch.util.ParseUtil;
import com.chrisnewland.jitwatch.util.StringUtil;
import com.chrisnewland.jitwatch.util.VmVersionDetector;

public class HotSpotLogParser extends AbstractLogParser
{
    public HotSpotLogParser(IJITListener jitListener)
    {
        super(jitListener);
    }

    private static Architecture architecture;
    
    private boolean sawUnresolvableDefineHiddenClass = false;
    private boolean sawUnresolvableLambdaClass = false;
    private boolean hiddenClassDumpPresent = false;
    private boolean lambdaProxyDumpPresent = false;
    private String hiddenClassWarningMessage = "";

    private static final String WARN_SEP  = "+-----------------------------------------------------------+";
    private static final String WARN_HEAD = "| WARNING: Hidden/dynamic class files could not be located. |";

    private void checkIfErrorDialogNeeded()
    {
        if (hasParseError)
        {
            errorListener.handleError(errorDialogTitle, errorDialogBody);
        }
    }

    private void parseHeaderLines()
    {
        if (DEBUG_LOGGING)
        {
            logger.debug("parseHeaderLines()");
        }

        for (NumberedLine numberedLine : splitLog.getHeaderLines())
        {
            String lineContent = numberedLine.getLine();

            if (!skipLine(lineContent, SKIP_HEADER_TAGS))
            {
                Tag tag = tagProcessor.processLine(lineContent);

                processLineNumber = numberedLine.getLineNumber();

                if (tag != null)
                {
                    handleTag(tag);
                }
            }
            else if (lineContent.startsWith(TAG_HOTSPOT_LOG))
            {
                long baseTimestamp = getBaseTimestamp(lineContent);

                model.setBaseTimestamp(baseTimestamp);
            }
        }
    }

    private long getBaseTimestamp(String line)
    {
        String attributePart = line.substring(TAG_HOTSPOT_LOG.length());

        Map<String, String> attrs = StringUtil.attributeStringToMap(attributePart);

        String baseTimestampAttr = attrs.get(ATTR_TIME_MS);

        return ParseUtil.parseStamp(baseTimestampAttr) / 1000;
    }

    @Override
    protected void parseLogFile()
    {
        parseHeaderLines();

        buildParsedClasspath();

        buildClassModel();

        parseLogCompilationLines();

        parseAssemblyLines();

        checkIfErrorDialogNeeded();
    }

    private void parseLogCompilationLines()
    {
        if (DEBUG_LOGGING)
        {
            logger.debug("parseLogCompilationLines()");
        }

        for (NumberedLine numberedLine : splitLog.getCompilationLines())
        {
            if (!skipLine(numberedLine.getLine(), SKIP_BODY_TAGS))
            {
                Tag tag = tagProcessor.processLine(numberedLine.getLine());

                processLineNumber = numberedLine.getLineNumber();

                if (tag != null)
                {
                    handleTag(tag);
                }
            }
        }
    }

    private void parseAssemblyLines() {
        AssemblyProcessor asmProcessor;

        if (DEBUG_LOGGING_ASSEMBLY) {
            logger.error("parseAssemblyLines()");
        }

        if (model.getJDKMajorVersion() > 11) {
            asmProcessor = new AssemblyProcessor(architecture);
        } else
        {
            asmProcessor = new AssemblyProcessor();
        }

        for (NumberedLine numberedLine : splitLog.getAssemblyLines())
        {
            processLineNumber = numberedLine.getLineNumber();

            asmProcessor.handleLine(numberedLine.getLine());
        }

        asmProcessor.complete();

        asmProcessor.attachAssemblyToMembers(model.getPackageManager());

        asmProcessor.clear();
    }

    @Override
    protected void splitLogFile(Reader hotspotLogReader)
    {
        reading = true;

        try (BufferedReader reader = new BufferedReader(hotspotLogReader, 65536))
        {
            String currentLine = reader.readLine();

            while (reading && currentLine != null)
            {
                try
                {
                    String trimmedLine = currentLine.trim();

                    if (trimmedLine.length() > 0)
                    {
                        char firstChar = trimmedLine.charAt(0);

                        if (firstChar == C_OPEN_ANGLE || firstChar == C_OPEN_SQUARE_BRACKET || firstChar == C_AT)
                        {
                            currentLine = trimmedLine;
                        }

                        handleLogLine(currentLine);
                    }
                }
                catch (Exception ex)
                {
                    logger.error("Exception handling: '{}'", currentLine, ex);
                }

                currentLine = reader.readLine();
            }
        }
        catch (IOException ioe)
        {
            logger.error("Exception while splitting log file", ioe);
        }
    }

    private boolean skipLine(final String line, final Set<String> skipSet)
    {
        boolean isSkip = false;

        for (String skip : skipSet)
        {
            if (line.startsWith(skip))
            {
                isSkip = true;
                break;
            }
        }

        return isSkip;
    }

    private void handleLogLine(final String inCurrentLine)
    {
        String currentLine = inCurrentLine;

        NumberedLine numberedLine = new NumberedLine(parseLineNumber++, currentLine);

        if (TAG_TTY.equals(currentLine))
        {
            inHeader = false;
            return;
        }
        else if (currentLine.startsWith(TAG_XML))
        {
            inHeader = true;
        }

        if (inHeader)
        {
            // HotSpot log header XML can have text nodes so consume all lines
            splitLog.addHeaderLine(numberedLine);
        }
        else
        {
            if (currentLine.startsWith(TAG_OPEN_CDATA) || currentLine.startsWith(TAG_CLOSE_CDATA)
                    || currentLine.startsWith(TAG_OPEN_CLOSE_CDATA))
            {
                // ignore, TagProcessor will recognise from <fragment> tag
            }
            else if (currentLine.startsWith(S_OPEN_ANGLE))
            {
                // After the header, XML nodes do not have text nodes
                splitLog.addCompilationLine(numberedLine);
            }
            else if (currentLine.startsWith(LOADED))
            {
                splitLog.addClassLoaderLine(numberedLine);
            }
            else if (currentLine.startsWith(S_AT))
            {
                // possible PrintCompilation was enabled as well as
                // LogCompilation?
                // jmh does this with perf annotations
                // Ignore this line
            }
            else if (currentLine.indexOf(S_OPEN_ANGLE + TAG_NMETHOD) != -1)
            {
                // need to cope with nmethod appearing on same line as last hlt
                // 0x0000 hlt <nmethod compile_id= ....

                int indexNMethod = currentLine.indexOf(S_OPEN_ANGLE + TAG_NMETHOD);

                if (DEBUG_LOGGING)
                {
                    logger.debug("detected nmethod tag mangled with assembly");
                }

                String assembly = currentLine.substring(0, indexNMethod);

                String remainder = currentLine.substring(indexNMethod);

                numberedLine.setLine(assembly);

                splitLog.addAssemblyLine(numberedLine);

                handleLogLine(remainder);

            }
            else if (currentLine.indexOf(S_OPEN_ANGLE + S_SLASH + TAG_PRINT_NMETHOD) != -1)
            {
                // need to cope with </print_nmethod> appearing on same last as
                // last assembly statement
                // ImmutableOopMap{rsi=Oop }pc offsets: 182 192 197 206 215
                // </print_nmethod>

                int indexClosePrintNmethod = currentLine.indexOf(S_OPEN_ANGLE + S_SLASH + TAG_PRINT_NMETHOD);

                if (DEBUG_LOGGING)
                {
                    logger.debug("detected </print_nmethod> tag mangled with assembly");
                }

                String assembly = currentLine.substring(0, indexClosePrintNmethod);

                String remainder = currentLine.substring(indexClosePrintNmethod);

                numberedLine.setLine(assembly);

                splitLog.addAssemblyLine(numberedLine);

                handleLogLine(remainder);

            }
            else
            {
                splitLog.addAssemblyLine(numberedLine);
            }
        }
    }

    @Override
    protected void handleTag(Tag tag)
    {
        String tagName = tag.getName();

        switch (tagName)
        {

        case TAG_WRITER:
            handleWriterThread(tag);
            break;

        case TAG_VM_VERSION:
            handleVmVersion(tag);
            break;

        case TAG_TASK_QUEUED:
            handleTagQueued(tag);
            break;

        case TAG_NMETHOD:
            handleTagNMethod(tag);
            break;

        case TAG_TASK:
            handleTagTask((Task) tag);
            break;

        case TAG_SWEEPER:
            storeCodeCacheEvent(CodeCacheEventType.SWEEPER, tag);
            break;

        case TAG_CODE_CACHE_FULL:
            storeCodeCacheEvent(CodeCacheEventType.CACHE_FULL, tag);
            break;

        case TAG_HOTSPOT_LOG_DONE:
            model.setEndOfLog(tag);
            break;

        case TAG_START_COMPILE_THREAD:
            handleStartCompileThread(tag);
            break;

        case TAG_VM_ARGUMENTS:
            handleTagVmArguments(tag);
            break;

        default:
            break;
        }
    }

    private void handleVmVersion(Tag tag)
    {
        model.setJDKMajorVersion(VmVersionDetector.getMajorVersionFromHotSpotTag(tag));
        if (model.getJDKMajorVersion() > 11) architecture = Architecture.parseFromLogLine(VmVersionDetector.getArchitectureFromHotSpotTag(tag));
    }

    private void handleTagVmArguments(Tag tag)
    {
        List<Tag> tagCommandChildren = tag.getNamedChildren(TAG_COMMAND);

        if (tagCommandChildren.size() > 0)
        {
            vmCommand = tagCommandChildren.get(0).getTextContent();

            if (DEBUG_LOGGING)
            {
                logger.debug("VM Command: {}", vmCommand);
            }
        }
    }

    private void handleWriterThread(Tag tag)
    {
        String threadId = tag.getAttributes().get(ATTR_THREAD);

        if (threadId != null)
        {
            // currentCompilerThread = model.getCompilerThread(threadId);
        }
    }

    private void handleStartCompileThread(Tag tag)
    {
        // <start_compile_thread name='C2 CompilerThread1' thread='17667'
        // process='82237' stamp='0.079'/>

        String threadId = tag.getAttributes().get(ATTR_THREAD);
        String threadName = tag.getAttributes().get(ATTR_NAME);

        if (threadId != null)
        {
            currentCompilerThread = model.createCompilerThread(threadId, threadName);
        }
    }

    private void buildParsedClasspath()
    {
        if (DEBUG_LOGGING)
        {
            logger.debug("buildParsedClasspath()");
        }

        for (NumberedLine numberedLine : splitLog.getClassLoaderLines())
        {
            buildParsedClasspath(numberedLine.getLine());
        }
    }

    private void buildClassModel()
    {
        if (DEBUG_LOGGING)
        {
            logger.debug("buildClassModel()");
        }

        for (NumberedLine numberedLine : splitLog.getClassLoaderLines())
        {
            buildClassModel(numberedLine.getLine());
        }
    }

    private void buildParsedClasspath(String inCurrentLine)
    {
        final String FROM_SPACE = "from ";

        String originalLocation = null;

        int fromSpacePos = inCurrentLine.indexOf(FROM_SPACE);

        if (fromSpacePos != -1)
        {
            originalLocation = inCurrentLine.substring(fromSpacePos + FROM_SPACE.length(), inCurrentLine.length() - 1);
        }

        if (originalLocation != null && originalLocation.startsWith(S_FILE_COLON))
        {
            originalLocation = originalLocation.substring(S_FILE_COLON.length());

            try
            {
                originalLocation = URLDecoder.decode(originalLocation, "UTF-8");
            }
            catch (UnsupportedEncodingException e)
            {
                // ignore
            }

            getParsedClasspath().addClassLocation(originalLocation);
        }
    }

    private void buildClassModel(String inCurrentLine)
    {
        String fqClassName = StringUtil.getSubstringBetween(inCurrentLine, LOADED, S_SPACE);

        if (fqClassName != null)
        {
            addToClassModel(fqClassName);
        }
    }
    
    @Override
    public boolean isHiddenClassWarningNeeded()
    {
        return !hiddenClassWarningMessage.isEmpty();
    }

    @Override
    public String getHiddenClassWarningMessage()
    {
        return hiddenClassWarningMessage;
    }
    
    @Override
    public void reset()
    {
        super.reset();
        sawUnresolvableDefineHiddenClass = false;
        sawUnresolvableLambdaClass = false;
        hiddenClassDumpPresent = false;
        lambdaProxyDumpPresent = false;
        hiddenClassWarningMessage = "";
    }

    @Override
    protected boolean tryHandleHiddenClass(String fqClassName)
    {
    	if (ParseUtil.isHiddenClassFQN(fqClassName))
        {
            loadHiddenClassToModel(fqClassName);
            return true;
        }
        return false;
    }

    @Override
    protected boolean tryRecoverMember(String logSignature)
    {
    	int spaceIdx = logSignature.indexOf(' ');
        if (spaceIdx < 0)
        {
            return false;
        }

        String fqClassName = logSignature.substring(0, spaceIdx);

        if (!ParseUtil.isHiddenClassFQN(fqClassName))
        {
            return false;
        }

        if (model.getPackageManager().getMetaClass(fqClassName) != null)
        {
            return true;
        }

        addToClassModel(fqClassName);

        return model.getPackageManager().getMetaClass(fqClassName) != null;
    }

    @Override
    protected void onMemberUnresolvable(String logSignature, LogParseException ex)
    {
        int spaceIdx = logSignature.indexOf(' ');
        String candidateFQN = spaceIdx >= 0 ? logSignature.substring(0, spaceIdx) : logSignature;

        if (candidateFQN.contains("$$Lambda"))
        {
            String lookupFQN = ParseUtil.isLegacyLambdaFQN(candidateFQN)
                ? ParseUtil.stripLegacyLambdaHash(candidateFQN)
                : candidateFQN;
            if (model.getPackageManager().getMetaClass(lookupFQN) == null)
            {
                sawUnresolvableLambdaClass = true;
            }
        }
        else
        {
            super.onMemberUnresolvable(logSignature, ex);
        }
    }

    @Override
    protected void afterParseLogFile()
    {
        // Anonymous/hidden classes that bypass classloader and compilation XML sections
        // (no [Loaded ...] line, no <task_queued> entry) still appear in assembly as
        // method-header comments of the form:  # {method} ... in 'FQN/0xADDR'
        // Scan those lines as a fallback so the warning fires even in this case.
        if (!sawUnresolvableDefineHiddenClass || !sawUnresolvableLambdaClass)
        {
            for (NumberedLine nl : splitLog.getAssemblyLines())
            {
                String line = nl.getLine();
                int inIdx = line.indexOf("in '");
                if (inIdx < 0)
                {
                    continue;
                }
                int start = inIdx + 4;
                int end   = line.indexOf('\'', start);
                if (end <= start)
                {
                    continue;
                }
                String fqn = line.substring(start, end);
                if (!ParseUtil.isHiddenClassFQN(fqn))
                {
                    continue;
                }
                if (fqn.contains("$$Lambda"))
                {
                    if (!lambdaProxyDumpPresent)
                    {
                        sawUnresolvableLambdaClass = true;
                    }
                }
                else
                {
                    if (!hiddenClassDumpPresent)
                    {
                        sawUnresolvableDefineHiddenClass = true;
                    }
                }
            }
        }

        boolean warnLambda = sawUnresolvableLambdaClass && !lambdaProxyDumpPresent;
        boolean warnHidden = sawUnresolvableDefineHiddenClass && !hiddenClassDumpPresent;

        if (warnLambda || warnHidden)
        {
            int jdkMajor = model.getJDKMajorVersion();

            StringBuilder msg = new StringBuilder();
            msg.append(WARN_SEP).append('\n');
            msg.append(WARN_HEAD).append('\n');
            msg.append(WARN_SEP).append('\n');

            boolean anyFlagAvailable = warnLambda || (warnHidden && jdkMajor >= 21);

            if (anyFlagAvailable)
            {
                msg.append("  Re-run with the appropriate JVM flag(s), then add the dump\n");
                msg.append("  directory to Class locations in JITWatch configuration.\n");
            }

            if (warnLambda)
            {
                msg.append('\n');
                if (jdkMajor >= 21)
                {
                    msg.append("  Lambda proxy classes (JDK ").append(jdkMajor).append(", hidden-class backed):\n");
                    msg.append("    -Djdk.invoke.LambdaMetafactory.dumpProxyClassFiles\n");
                    msg.append("    Add DUMP_LAMBDA_PROXY_CLASS_FILES/ to Class locations.\n");
                }
                else if (jdkMajor >= 15)
                {
                    msg.append("  Lambda proxy classes (JDK ").append(jdkMajor).append(", hidden-class backed):\n");
                    msg.append("    -Djdk.internal.lambda.dumpProxyClasses=/your/dump/dir\n");
                    msg.append("    Add that directory to Class locations.\n");
                }
                else
                {
                    msg.append("  Lambda proxy classes (JDK ").append(jdkMajor).append("):\n");
                    msg.append("    -Djdk.internal.lambda.dumpProxyClasses=/your/dump/dir\n");
                    msg.append("    Add that directory to Class locations.\n");
                }
            }

            if (warnHidden)
            {
                if (warnLambda) msg.append('\n');
                if (jdkMajor >= 21)
                {
                    msg.append("  Lookup.defineHiddenClass classes (JDK ").append(jdkMajor).append("):\n");
                    msg.append("    -Djdk.invoke.MethodHandle.dumpClassFiles\n");
                    msg.append("    Add DUMP_CLASS_FILES/ to Class locations.\n");
                }
                else if (jdkMajor >= 15)
                {
                    msg.append("  Lookup.defineHiddenClass classes (JDK ").append(jdkMajor).append("):\n");
                    msg.append("    No standard JVM flag is available on this JDK version.\n");
                    msg.append("    Capture requires a native JVMTI agent or bytecode\n");
                    msg.append("    instrumentation on Lookup::defineHiddenClass.\n");
                }
                else
                {
                    // JDK 11-14: Unsafe.defineAnonymousClass writes /0x<addr> FQNs into
                    // compilation log XML, so detection works.
                    // JDK 8-10: bare class names in XML — detection is not supported.
                    msg.append("  Unsafe.defineAnonymousClass classes (JDK ").append(jdkMajor).append("):\n");
                    msg.append("    No standard JVM flag is available on this JDK version.\n");
                    msg.append("    Capture requires a native JVMTI agent or bytecode\n");
                    msg.append("    instrumentation on Unsafe::defineAnonymousClass.\n");
                }
            }

            msg.append(WARN_SEP);

            hiddenClassWarningMessage = msg.toString();
            logError("Hidden/dynamic class files could not be located; see parse log for details.");
        }
    }

    private void loadHiddenClassToModel(String fqClassName)
    {
        // fqClassName is e.g. "com.foo.Bar/0x61044ae8"
        int slashIdx = fqClassName.indexOf('/');
        String baseFQN = fqClassName.substring(0, slashIdx);           // com.foo.Bar
        String address = fqClassName.substring(slashIdx + 1);          // 0x61044ae8
        String baseRelativePath = baseFQN.replace('.', '/');

        List<String> locations = new ArrayList<>();
        locations.addAll(config.getConfiguredClassLocations());
        locations.addAll(getParsedClasspath().getClassLocations());

        String[] candidates = {
            baseRelativePath + "." + address + ".class",  // new flags (JDK 21+)
            baseRelativePath + ".class"                   // old flags (any JDK)
        };

        for (String root : locations)
        {
            for (String candidate : candidates)
            {
                File classFile = new File(root, candidate);

                if (classFile.exists())
                {
                    if (fqClassName.contains("$$Lambda")) 
                    {
                    	lambdaProxyDumpPresent = true;
                    }
                    else if (!baseFQN.startsWith("java.lang.invoke.")) 
                    {
                    	hiddenClassDumpPresent = true;
                    }

                    if (tryLoadHiddenClassFile(classFile, fqClassName)) 
                    {
                    	return;
                    }
                }
            }
        }

        if (DEBUG_LOGGING)
        {
            logger.debug("Could not find class file for hidden class {}", fqClassName);
        }

        if (fqClassName.contains("$$Lambda"))
        {
            sawUnresolvableLambdaClass = true;
        }
        else
        {
            sawUnresolvableDefineHiddenClass = true;
        }
    }

    private boolean tryLoadHiddenClassFile(File classFile, String fqClassName)
    {
        try (FileInputStream fis = new FileInputStream(classFile))
        {
            byte[] bytes = new byte[(int) classFile.length()];
            int read = 0;
            while (read < bytes.length)
            {
                int n = fis.read(bytes, read, bytes.length - read);
                if (n < 0) 
                {
                	break;
                }
                read += n;
            }
            Class<?> clazz = new ByteArrayClassLoader(ClassUtil.getDisposableClassLoader()).define(bytes);
            model.buildAndGetMetaClass(clazz, fqClassName);
            return true;
        }
        catch (SecurityException se) // NB: ClassAct can likely fully recover from this
        {
            if (DEBUG_LOGGING)
            {
                logger.debug("Skipping hidden class in protected package: {}", fqClassName);
            }
            return true;
        }
        catch (IOException e)
        {
            logger.error("Could not read hidden class file {}", classFile, e);
            return false;
        }
    }
}
