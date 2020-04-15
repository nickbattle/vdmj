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

import com.fujitsu.vdmj.in.INNode;
import com.fujitsu.vdmj.in.expressions.INExpression;
import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.runtime.Breakpoint;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.values.Value;

/**
 * The parent class of all statements.
 */
public abstract class INStatement extends INNode implements Serializable
{
	private static final long serialVersionUID = 1L;

	/** The statement's breakpoint, if any. */
	public Breakpoint breakpoint;

	/**
	 * Create a statement at the given location.
	 * @param location
	 */

	public INStatement(LexLocation location)
	{
		super(location);
		this.breakpoint = new Breakpoint(location);
		location.executable(true);
	}

	@Override
	abstract public String toString();

	/**
	 * Find a statement starting on the given line. Single statements just
	 * compare their location to lineno, but block statements and statements
	 * with sub-statements iterate over their branches.
	 *
	 * @param lineno The line number to locate.
	 * @return A statement starting on the line, or null.
	 */

	public INStatement findStatement(int lineno)
	{
		return (location.startLine == lineno) ? this : null;	// TODO as a StatementVisitor?
	}

	/**
	 * @param lineno  
	 */
	public INExpression findExpression(int lineno)
	{
		return null;	// TODO as a StatementVisitor?
	}

	/** Evaluate the statement in the context given. */
	abstract public Value eval(Context ctxt);

	/**
	 * Implemented by all statements to allow visitor processing.
	 */
	abstract public <R, S> R apply(INStatementVisitor<R, S> visitor, S arg);
}
