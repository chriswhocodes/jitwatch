/*
 * Copyright (c) 2015-2021 Jean Phillipe BEMPEL
 * Licensed under https://github.com/chriswhocodes/jitwatch/blob/master/LICENSE-BSD
 * Instructions: https://github.com/chriswhocodes/jitwatch/wiki
 */
package com.chrisnewland.jitwatch.inline;

import static com.chrisnewland.jitwatch.core.JITWatchConstants.ATTR_BYTES;
import static com.chrisnewland.jitwatch.core.JITWatchConstants.ATTR_ID;
import static com.chrisnewland.jitwatch.core.JITWatchConstants.ATTR_METHOD;
import static com.chrisnewland.jitwatch.core.JITWatchConstants.ATTR_NAME;
import static com.chrisnewland.jitwatch.core.JITWatchConstants.ATTR_REASON;
import static com.chrisnewland.jitwatch.core.JITWatchConstants.S_PARSE_HIR;
import static com.chrisnewland.jitwatch.core.JITWatchConstants.TAG_CALL;
import static com.chrisnewland.jitwatch.core.JITWatchConstants.TAG_INLINE_FAIL;
import static com.chrisnewland.jitwatch.core.JITWatchConstants.TAG_INLINE_SUCCESS;
import static com.chrisnewland.jitwatch.core.JITWatchConstants.TAG_METHOD;
import static com.chrisnewland.jitwatch.core.JITWatchConstants.TAG_PARSE;
import static com.chrisnewland.jitwatch.core.JITWatchConstants.TAG_PHASE;

import java.io.PrintStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.chrisnewland.jitwatch.compilation.AbstractCompilationVisitable;
import com.chrisnewland.jitwatch.compilation.ICompilationVisitable;
import com.chrisnewland.jitwatch.compilation.CompilationUtil;
import com.chrisnewland.jitwatch.model.Compilation;
import com.chrisnewland.jitwatch.model.IMetaMember;
import com.chrisnewland.jitwatch.model.IParseDictionary;
import com.chrisnewland.jitwatch.model.IReadOnlyJITDataModel;
import com.chrisnewland.jitwatch.model.LogParseException;
import com.chrisnewland.jitwatch.model.Tag;
import com.chrisnewland.jitwatch.treevisitor.ITreeVisitable;
import com.chrisnewland.jitwatch.util.ParseUtil;
import com.chrisnewland.freelogj.Logger;
import com.chrisnewland.freelogj.LoggerFactory;

public class HeadlessInlineVisitor implements ITreeVisitable
{
    private static final Logger logger = LoggerFactory.getLogger(HeadlessInlineVisitor.class);

    private final Map<String, Map<String, InlineFailureInfo>> failures = new HashMap<>();
    private final IReadOnlyJITDataModel model;

    public HeadlessInlineVisitor(IReadOnlyJITDataModel model)
    {
        this.model = model;
    }

    @Override
    public void visit(IMetaMember metaMember)
    {
        if (metaMember == null)
        {
            return;
        }
        
        if (!metaMember.isCompiled())
        {
            return;
        }
        
        try
        {
            String callerName = metaMember.toString();
            InlineJournalVisitor inlineJournalVisitor = new InlineJournalVisitor(failures,  model, callerName);
            
			for (Compilation compilation : metaMember.getCompilations())
			{
				CompilationUtil.visitParseTagsOfCompilation(compilation, inlineJournalVisitor);
			}            
        }
        catch (LogParseException e)
        {
            logger.error("Error building inlining stats", e);
        }
    }

    @Override
    public void reset()
    {

    }

    public void printFailedList(PrintStream out)
    {
        for (Map.Entry<String, Map<String, InlineFailureInfo>> entry : failures.entrySet())
        {
            out.println("=== " + entry.getKey() + " ===");
            Map<String, InlineFailureInfo> members = entry.getValue();
            for (InlineFailureInfo inlineFailureInfo : members.values())
            {
                out.println(inlineFailureInfo);
            }
        }
    }

    private static class InlineJournalVisitor extends AbstractCompilationVisitable  implements ICompilationVisitable
    {
        private final Map<String, Map<String, InlineFailureInfo>> failures;
        private final IReadOnlyJITDataModel model;
        private final String callerName;

        public InlineJournalVisitor(Map<String, Map<String, InlineFailureInfo>> failures, IReadOnlyJITDataModel model, String callerName)
        {
            this.failures = failures;
            this.model = model;
            this.callerName = callerName;
        }

        @Override
        public void visitTag(Tag parseTag, IParseDictionary parseDictionary) throws LogParseException
        {
            processParseTag(parseTag, parseDictionary);
        }

        private void processParseTag(Tag parseTag, IParseDictionary parseDictionary)
        {
            String methodID = null;
            
            for (Tag child : parseTag.getChildren())
            {
                String tagName = child.getName();
                
                Map<String, String> tagAttrs = child.getAttributes();
                
                switch (tagName)
                {
                    case TAG_METHOD:
                    {
                        methodID = tagAttrs.get(ATTR_ID);
                        break;
                    }
                    
                    case TAG_CALL:
                    {
                        methodID = tagAttrs.get(ATTR_METHOD);
                        break;
                    }
                    
                    case TAG_INLINE_FAIL:
                    {
                        String reason = tagAttrs.get(ATTR_REASON);
                        Map<String, InlineFailureInfo> inlineFailureInfos = failures.get(reason);
                   
                        if (inlineFailureInfos == null)
                        {
                            inlineFailureInfos = new HashMap<>();
                            failures.put(reason, inlineFailureInfos);
                        }
                     
                        IMetaMember metaMember = ParseUtil.lookupMember(methodID, parseDictionary, model);
                     
                        if (metaMember == null)
                        {
                            logger.warn("Cannot find name of methodId: ", methodID);
                        }
                        else
                        {
                            String memberName = metaMember.toString();
                            InlineFailureInfo inlineFailureInfo = inlineFailureInfos.get(memberName);
                            if (inlineFailureInfo == null)
                            {
                                Tag methodTag = parseDictionary.getMethod(methodID);
                                int byteCodeSize = Integer.parseInt(methodTag.getAttributes().get(ATTR_BYTES));
                                inlineFailureInfo = new InlineFailureInfo(memberName, byteCodeSize);
                                inlineFailureInfos.put(memberName, inlineFailureInfo);
                            }
                            inlineFailureInfo.addCaller(callerName);
                            inlineFailureInfo.incFailureCount();
                        }
                        methodID = null;
                        
                        break;
                    }

                    case TAG_INLINE_SUCCESS:
                    {
                        break;
                    }
                    
                    case TAG_PARSE:
                    {
                        processParseTag(child, parseDictionary);
                        break;
                    }
        			
        			case TAG_PHASE:
        			{
        				String phaseName = tagAttrs.get(ATTR_NAME);
        				
        				if (S_PARSE_HIR.equals(phaseName))
        				{
        					processParseTag(child, parseDictionary);
        				}
        				else
        				{
        					logger.warn("Don't know how to handle phase {}", phaseName);
        				}
        				
        				break;
        			}
                    
                    default:
                    	handleOther(child);
                        break;
                }
            }
        }
    }

    private static class InlineFailureInfo
    {
        private String memberName;
        private int byteCodeSize;
        private int failureCount;
        private Set<String> callers = new HashSet<>();

        public InlineFailureInfo(String memberName, int byteCodeSize)
        {
            this.memberName = memberName;
            this.byteCodeSize = byteCodeSize;
        }

        public void incFailureCount()
        {
            failureCount++;
        }

        public void addCaller(String name)
        {
            callers.add(name);
        }

        @Override
        public boolean equals(Object o)
        {
            if (this == o)
            {
                return true;
            }
            if (o == null || getClass() != o.getClass())
            {
                return false;
            }
            InlineFailureInfo that = (InlineFailureInfo) o;
            return memberName.equals(that.memberName);
        }

        @Override
        public int hashCode()
        {
            return memberName.hashCode();
        }

        @Override
        public String toString()
        {
            return "InlineFailureInfo{" +
                    "memberName='" + memberName + '\'' +
                    ", byteCodeSize=" + byteCodeSize +
                    ", failureCount=" + failureCount +
                    ", callers=" + callers +
                    '}';
        }
    }
}
