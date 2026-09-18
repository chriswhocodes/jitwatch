/*
 * Copyright (c) 2013-2022 Chris Newland.
 * Licensed under https://github.com/chriswhocodes/jitwatch/blob/master/LICENSE-BSD
 * Instructions: https://github.com/chriswhocodes/jitwatch/wiki
 */
package com.chrisnewland.jitwatch.loader;

public class ByteArrayClassLoader extends ClassLoader
{
    public ByteArrayClassLoader(ClassLoader parent)
    {
        super(parent);
    }

    public Class<?> define(byte[] bytes)
    {
        return defineClass(null, bytes, 0, bytes.length);
    }
}
