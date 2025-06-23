/*******************************************************************************
 *
 *	Copyright (c) 2016 Fujitsu Services Ltd.
 *
 *	Author: Nick Battle
 *
 *	This file is part of VDMJ.
 *
 *	VDMJ is free software: you can redistribute it and/or modify
 *	it under the terms of the GNU General Public License as published by
 *	the Free Software Foundation, either version 3 of the License, or
 *	(at your option) any later version.
 *
 *	VDMJ is distributed in the hope that it will be useful,
 *	but WITHOUT ANY WARRANTY; without even the implied warranty of
 *	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *	GNU General Public License for more details.
 *
 *	You should have received a copy of the GNU General Public License
 *	along with VDMJ.  If not, see &lt;http://www.gnu.org/licenses/&gt;.
 *	SPDX-License-Identifier: GPL-3.0-or-later
 *
 ******************************************************************************/

package com.fujitsu.vdmj.tc.patterns;

import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.tc.TCNode;
import com.fujitsu.vdmj.tc.patterns.visitors.TCBindVisitor;
import com.fujitsu.vdmj.typechecker.TypeChecker;

/**
 * The parent class of {@link TCSetBind} and {@link TCTypeBind}.
 */
public abstract class TCBind extends TCNode
{
	private static final long serialVersionUID = 1L;

	/** The textual location of the bind. */
	public final LexLocation location;
	/** The pattern of the bind. */
	public final TCPattern pattern;

	/**
	 * Create a bind at the given location with the given pattern.
	 */
	public TCBind(LexLocation location, TCPattern pattern)
	{
		this.location = location;
		this.pattern = pattern;
	}

	/** Return this one bind as a list of {@link TCMultipleBind}. */
	abstract public TCMultipleBindList getMultipleBindList();

	public void report(int number, String msg)
	{
		TypeChecker.report(number, msg, location);
	}

	public void detail(String tag, Object obj)
	{
		TypeChecker.detail(tag, obj);
	}

	public void detail2(String tag1, Object obj1, String tag2, Object obj2)
	{
		TypeChecker.detail2(tag1, obj1, tag2, obj2);
	}

	/**
	 * Implemented by all binds to allow visitor processing.
	 */
	abstract public <R, S> R apply(TCBindVisitor<R, S> visitor, S arg);
}
