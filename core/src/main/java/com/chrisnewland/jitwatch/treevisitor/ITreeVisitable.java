/*
 * Copyright (c) 2013-2015 Chris Newland.
 * Licensed under https://github.com/chriswhocodes/jitwatch/blob/master/LICENSE-BSD
 * Instructions: https://github.com/chriswhocodes/jitwatch/wiki
 */
package com.chrisnewland.jitwatch.treevisitor;

import com.chrisnewland.jitwatch.model.IMetaMember;

public interface ITreeVisitable
{
    void visit(IMetaMember mm);
    void reset();
}