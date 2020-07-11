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
 *
 ******************************************************************************/

package com.fujitsu.vdmj.in.patterns;

import java.util.List;
import java.util.Vector;

import com.fujitsu.vdmj.in.INNode;
import com.fujitsu.vdmj.in.patterns.visitors.INMultipleBindVisitor;
import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.runtime.ContextException;
import com.fujitsu.vdmj.runtime.ValueException;
import com.fujitsu.vdmj.tc.lex.TCNameList;
import com.fujitsu.vdmj.values.Value;
import com.fujitsu.vdmj.values.ValueList;

/**
 * The parent class of {@link INMultipleSetBind} and {@link INMultipleTypeBind}.
 */
public abstract class INMultipleBind extends INNode
{
	private static final long serialVersionUID = 1L;

	/** The textual location of the bind. */
	public final LexLocation location;
	/** The list of patterns for this bind. */
	public final INPatternList plist;

	/**
	 * Create a multiple bind with the given pattern list. The location is
	 * taken from the location of the first pattern in the list.
	 */
	public INMultipleBind(INPatternList plist)
	{
		this.plist = plist;
		this.location = plist.get(0).location;
	}

	/** Return this one bind as a list of {@link INMultipleBind}. */
	public List<INMultipleBind> getMultipleBindList()
	{
		List<INMultipleBind> list = new Vector<INMultipleBind>();
		list.add(this);
		return list;
	}

	/** Get a list of all the possible values to bind the variables to. */ 
	abstract public ValueList getBindValues(Context ctxt, boolean permuted) throws ValueException;

	/** Return a list of all values read by the bind evaluation. */
	abstract public ValueList getValues(Context ctxt);

	/** Return a list of old names used by the bind. */
	abstract public TCNameList getOldNames();

	/**
	 * @see org.INDefinition.vdmj.definitions.Definition#abort
	 */

	@Override
	public Value abort(ValueException ve)
	{
		throw new ContextException(ve, location);
	}

	/**
	 * Implemented by all multiple binds to allow visitor processing.
	 */
	abstract public <R, S> R apply(INMultipleBindVisitor<R, S> visitor, S arg);
}
