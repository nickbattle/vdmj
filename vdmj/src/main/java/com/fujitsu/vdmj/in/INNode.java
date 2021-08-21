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

package com.fujitsu.vdmj.in;

import java.io.Serializable;

import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.mapper.MappedObject;
import com.fujitsu.vdmj.messages.LocatedException;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.runtime.ContextException;
import com.fujitsu.vdmj.runtime.ExceptionHandler;
import com.fujitsu.vdmj.runtime.ValueException;
import com.fujitsu.vdmj.values.Value;

/**
 * The abstract root of all interpreter nodes.
 */
abstract public class INNode extends MappedObject implements Serializable
{
	private static final long serialVersionUID = 1L;

	public final static String MAPPINGS = "tc-in.mappings";

	/** The textual location of the node. */
	public final LexLocation location;
	
	protected INNode()
	{
		this.location = LexLocation.ANY;	// Some INNodes don't care
	}

	protected INNode(LexLocation location)
	{
		this.location = location;
	}

	/**
	 * Get the location of the expression. This is occasionally overridden by
	 * pre and postcondition expressions that need to simulate their location
	 * as being that of an errs clause.
	 */
	public LexLocation getLocation()
	{
		return location;
	}
	
	/**
	 * Abort methods are useful throughout the IN hierarchy, but they need a location.
	 * Hence the location in the constructor above. 
	 */
	protected Value abort(int number, String msg, Context ctxt)
	{
		ExceptionHandler.handle(new ContextException(number, msg, location, ctxt));
		return null;
	}

	public Value abort(ValueException ve)
	{
		ExceptionHandler.handle(new ContextException(ve, location));
		return null;
	}
	
	public Value abort(int number, String msg, Context ctxt, LexLocation... loc)
	{
		if (loc.length >= 1)
		{
			ExceptionHandler.handle(new ContextException(number, msg, loc[0], ctxt));
		}
		else
		{
			ExceptionHandler.handle(new ContextException(number, msg, location, ctxt));
		}
		
		return null;
	}

	public Value abort(LocatedException e, Context ctxt)
	{
		ExceptionHandler.handle(new ContextException(e.number, e.getMessage(), e.location, ctxt));
		return null;
	}
}
