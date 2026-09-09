/*
 * Copyright (c) 2013-2021 Chris Newland.
 * Licensed under https://github.com/chriswhocodes/jitwatch/blob/master/LICENSE-BSD
 * Instructions: https://github.com/chriswhocodes/jitwatch/wiki
 */
package com.chrisnewland.jitwatch.model;

import static com.chrisnewland.jitwatch.core.JITWatchConstants.C_SPACE;
import static com.chrisnewland.jitwatch.core.JITWatchConstants.DEBUG_LOGGING_PARSE_DICTIONARY;
import static com.chrisnewland.jitwatch.core.JITWatchConstants.S_COMMA;
import static com.chrisnewland.jitwatch.core.JITWatchConstants.S_DOT;
import static com.chrisnewland.jitwatch.core.JITWatchConstants.S_OPEN_PARENTHESES;
import static com.chrisnewland.jitwatch.core.JITWatchConstants.S_SLASH;
import static com.chrisnewland.jitwatch.core.JITWatchConstants.S_SPACE;
import static com.chrisnewland.jitwatch.core.JITWatchConstants.TAG_TASK;
import static com.chrisnewland.jitwatch.core.JITWatchConstants.ATTR_METHOD;

import java.util.Map;

import com.chrisnewland.jitwatch.core.JITWatchConstants;
import com.chrisnewland.jitwatch.model.bytecode.Opcode;
import com.chrisnewland.jitwatch.util.ParseUtil;
import com.chrisnewland.freelogj.Logger;
import com.chrisnewland.freelogj.LoggerFactory;

public class Task extends Tag
{
	private static final Logger logger = LoggerFactory.getLogger(Task.class);

	private IParseDictionary parseDictionary;
	
	public Task(String attributeString, boolean selfClosing)
	{
		super(TAG_TASK, attributeString, selfClosing);

		parseDictionary = new ParseDictionary(getAttributes().get(ATTR_METHOD));
	}

	public IParseDictionary getParseDictionary()
	{
		return parseDictionary;
	}
	
	public void addBCIOpcodeMapping(String methodID, int bci, Opcode opcode)
	{
		if (DEBUG_LOGGING_PARSE_DICTIONARY)
		{
			logger.debug("Adding bci mapping: {} {} {}", methodID, bci, opcode);
		}

		parseDictionary.putBCIOpcode(methodID, bci, opcode);
	}

	public void addDictionaryType(String type, Tag tag)
	{
		if (DEBUG_LOGGING_PARSE_DICTIONARY)
		{
			logger.debug("Adding type: {}", type);
		}

		parseDictionary.putType(type, tag);
	}

	public void addDictionaryMethod(String method, Tag tag)
	{
		if (DEBUG_LOGGING_PARSE_DICTIONARY)
		{
			logger.debug("Adding method: {}", method);
		}

		parseDictionary.putMethod(method, tag);
	}

	public void addDictionaryKlass(String klass, Tag tag)
	{
		if (DEBUG_LOGGING_PARSE_DICTIONARY)
		{
			logger.debug("Adding klass: {}", klass);
		}

		parseDictionary.putKlass(klass, tag);
	}

	public String decodeParseMethod(String method)
	{
		StringBuilder builder = new StringBuilder();

		Tag methodTag = parseDictionary.getMethod(method);
		
		Map<String, String> methodTagAttrs = methodTag.getAttributes();

		String returnTypeID = methodTagAttrs.get(JITWatchConstants.ATTR_RETURN);

		String args = methodTagAttrs.get(JITWatchConstants.ATTR_ARGUMENTS);

		String methodName = methodTagAttrs.get(JITWatchConstants.ATTR_NAME);

		String klassId = methodTagAttrs.get(JITWatchConstants.ATTR_HOLDER);

		Tag klassTag = parseDictionary.getKlass(klassId);

		Map<String, String> klassTagAttrs = klassTag.getAttributes();
		
		String klassName = klassTagAttrs.get(JITWatchConstants.ATTR_NAME);
		klassName = klassName.replace(S_SLASH, S_DOT);

		builder.append(" <!-- ");
		builder.append(ParseUtil.lookupType(returnTypeID, parseDictionary));
		builder.append(C_SPACE);
		builder.append(klassName);
		builder.append(S_DOT);
		builder.append(methodName);
		builder.append(S_OPEN_PARENTHESES);

		if (args != null && args.length() > 0)
		{
			String[] ids = args.split(S_SPACE);

			for (String id : ids)
			{
				builder.append(ParseUtil.lookupType(id, parseDictionary));
				builder.append(S_COMMA);
			}

			builder.deleteCharAt(builder.length() - 1);
		}

		builder.append(") -->");

		return builder.toString();
	}
}
