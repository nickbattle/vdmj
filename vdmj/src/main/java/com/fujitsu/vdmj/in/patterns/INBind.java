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
 *	along with VDMJ.  If not, see <http://www.gnu.org/licenses/>.
 *	SPDX-License-Identifier: GPL-3.0-or-later
 *
 ******************************************************************************/

package com.fujitsu.vdmj.in.patterns;

import java.io.Serializable;

import com.fujitsu.vdmj.in.INNode;
import com.fujitsu.vdmj.in.patterns.visitors.INBindVisitor;
import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.runtime.ValueException;
import com.fujitsu.vdmj.values.ValueList;

/**
 * The parent class of {@link INSetBind} and {@link INTypeBind}.
 */
public abstract class INBind extends INNode implements Serializable
{
	private static final long serialVersionUID = 1L;

	/** The pattern of the bind. */
	public final INPattern pattern;

	/**
	 * Create a bind at the given location with the given pattern.
	 */
	public INBind(LexLocation location, INPattern pattern)
	{
		super(location);
		this.pattern = pattern;
	}

	/** Return this one bind as a list of {@link INMultipleBind}. */
	abstract public INMultipleBindList getMultipleBindList();

	/** Return a list of all possible values for the bind. */ 
	abstract public ValueList getBindValues(Context ctxt, boolean permuted) throws ValueException;

	/**
	 * Implemented by all binds to allow visitor processing.
	 */
	abstract public <R, S> R apply(INBindVisitor<R, S> visitor, S arg);
}
