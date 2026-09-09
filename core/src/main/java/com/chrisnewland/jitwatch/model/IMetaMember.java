/*
 * Copyright (c) 2013-2017 Chris Newland.
 * Licensed under https://github.com/chriswhocodes/jitwatch/blob/master/LICENSE-BSD
 * Instructions: https://github.com/chriswhocodes/jitwatch/wiki
 */
package com.chrisnewland.jitwatch.model;

import java.util.List;
import java.util.Map;

import com.chrisnewland.jitwatch.model.assembly.AssemblyMethod;
import com.chrisnewland.jitwatch.model.bytecode.BytecodeInstruction;
import com.chrisnewland.jitwatch.model.bytecode.MemberBytecode;

public interface IMetaMember
{
	MetaClass getMetaClass();
	
	MemberBytecode getMemberBytecode();
	
	List<BytecodeInstruction> getInstructions();

	String toStringUnqualifiedMethodName(boolean visibilityAndReturnType, boolean fqParamTypes);

	String getMemberName();
	String getFullyQualifiedMemberName();
	String getAbbreviatedFullyQualifiedMemberName();

	int getModifier();
	String getModifierString();
	String getReturnTypeName();
	String[] getParamTypeNames();

	boolean matchesSignature(MemberSignatureParts msp, boolean matchTypesExactly);

	boolean isConstructor();
	
	String getQueuedAttribute(String key);
	Map<String, String> getQueuedAttributes();

	void storeCompilation(Compilation compilation);
	
	String getCompiledAttribute(String key);
	Map<String, String> getCompiledAttributes();

	Compilation getCompilationByCompileID(String compileID);
	Compilation getCompilationByAddress(AssemblyMethod asmMethod);
	
	void setCompiled(boolean compiled);
	boolean isCompiled();
	
	void addAssembly(AssemblyMethod asmMethod);
	
	void setSelectedCompilation(int index);
	Compilation getSelectedCompilation();
	List<Compilation> getCompilations();
	Compilation getCompilation(int index);
	Compilation getLastCompilation();
	
	String getSourceMethodSignatureRegEx();
}