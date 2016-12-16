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

package com.fujitsu.vdmj.in.statements;

import java.io.Serializable;

import com.fujitsu.vdmj.in.definitions.INDefinition;
import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.runtime.ContextException;
import com.fujitsu.vdmj.runtime.ExceptionHandler;
import com.fujitsu.vdmj.runtime.ValueException;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.values.Value;

/**
 * The root of the state designator hierarchy.
 */
public abstract class INStateDesignator implements Serializable
{
	private static final long serialVersionUID = 1L;

	public final LexLocation location;

	public INStateDesignator(LexLocation location)
	{
		this.location = location;
	}

	@Override
	abstract public String toString();

	public INDefinition targetDefinition(
		@SuppressWarnings("unused") Environment env)
	{
		return null;
	}

	abstract public Value eval(Context ctxt);

	public void abort(int number, String msg, Context ctxt)
	{
		ExceptionHandler.handle(new ContextException(number, msg, location, ctxt));
	}

	public Value abort(ValueException ve)
	{
		ExceptionHandler.handle(new ContextException(ve, location));
		return null;
	}
}
