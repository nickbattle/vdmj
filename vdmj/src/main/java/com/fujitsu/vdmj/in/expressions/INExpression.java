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

package com.fujitsu.vdmj.in.expressions;

import com.fujitsu.vdmj.config.Properties;
import com.fujitsu.vdmj.in.INNode;
import com.fujitsu.vdmj.in.expressions.visitors.INExpressionFinder;
import com.fujitsu.vdmj.in.expressions.visitors.INExpressionUpdatableFinder;
import com.fujitsu.vdmj.in.expressions.visitors.INExpressionVisitor;
import com.fujitsu.vdmj.in.expressions.visitors.INHistoryExpressionFinder;
import com.fujitsu.vdmj.in.expressions.visitors.INOldNamesFinder;
import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.runtime.Breakpoint;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.runtime.ContextException;
import com.fujitsu.vdmj.scheduler.InitThread;
import com.fujitsu.vdmj.tc.lex.TCNameList;
import com.fujitsu.vdmj.values.Value;
import com.fujitsu.vdmj.values.ValueList;

/**
 *	The parent class of all VDM expressions.
 */
public abstract class INExpression extends INNode
{
	private static final long serialVersionUID = 1L;

	/** The expression's breakpoint, if any. */
	public Breakpoint breakpoint;

	/**
	 * Generate an expression at the given location.
	 *
	 * @param location	The location of the new expression.
	 */

	public INExpression(LexLocation location)
	{
		super(location);
		this.breakpoint = new Breakpoint(location);
		location.executable(true);
	}

	/**
	 * Generate an expression at the same location as the expression passed.
	 * This is used when a compound expression, comprising several
	 * subexpressions, is being constructed. The expression passed "up" is
	 * considered the location of the overall expression. For example, a
	 * function application involves an expression for the function to apply,
	 * plus a list of expressions for the arguments. The location of the
	 * expression for the function (often just a variable name) is considered
	 * the location of the entire function application.
	 *
	 * @param exp The expression containing the location.
	 */

	public INExpression(INExpression exp)
	{
		this(exp.location);
	}

	@Override
	public abstract String toString();

	@Override
	public boolean equals(Object other)
	{
		if (other instanceof INExpression)
		{
			return toString().equals(other.toString());	// For now...
		}
		else
		{
			return false;
		}
	}

	@Override
	public int hashCode()
	{
		return toString().hashCode();
	}

	/**
	 * Evaluate the expression in the given runtime context. The {@link Value} object
	 * returned can hold any type of value (int, bool, sequences, sets etc).
	 *
	 * @param ctxt	The context in which to evaluate the expression.
	 * @return	The value of the expression.
	 */
	abstract public Value eval(Context ctxt);

	/**
	 * Find an expression starting on the given line. Single expressions just
	 * compare their location to lineno, but expressions with sub-expressions
	 * iterate over their branches.
	 *
	 * @param lineno The line number to locate.
	 * @return An expression starting on the line, or null.
	 */
	public final INExpression findExpression(int lineno)
	{
		INExpressionList all = this.apply(new INExpressionFinder(), lineno);
		return all.isEmpty() ? null : all.get(0);
	}

	/**
	 * Return a list of all the updatable values read by the expression. This
	 * is used to add listeners to values that affect permission guards, so
	 * that the guard may be efficiently re-evaluated when the values change.
	 *
	 * @param ctxt	The context in which to search for values.
	 * @return  A list of values read by the expression.
	 */
	public final ValueList getValues(Context ctxt)
	{
		return this.apply(new INExpressionUpdatableFinder(), ctxt);
	}
	
	/**
	 * Return a list of all the variable names used by an expression that refer
	 * to the "old" names of variables (like xyz~). This is used during the
	 * evaluation of postconditions to decide which variables to evaluate.
	 * 
	 * @return A list of old variable names.
	 */
	public final TCNameList getOldNames()
	{
		return apply(new INOldNamesFinder(), null);
	}

	/**
	 * Return a list of history sub-expressions of this expression. This is used when
	 * looking for history operators in permission guards. The result is guaranteed
	 * to only contain INHistoryExpressions (if any).
	 */
	public final INExpressionList getHistoryExpressions()
	{
		return this.apply(new INHistoryExpressionFinder(), null);
	}

	/**
	 * Check whether we are running during initialzation and fail if we are. This is used by
	 * some expressions and statements that are not permitted (like "duration" in RT).
	 */
	protected void assertNotInit(Context ctxt)
	{
		if (Properties.in_init_checks && Thread.currentThread() instanceof InitThread)
		{
			throw new ContextException(4177, "Not permitted during initialization", location, ctxt);
		}
	}

	/**
	 * Implemented by all expressions to allow visitor processing.
	 */
	abstract public <R, S> R apply(INExpressionVisitor<R, S> visitor, S arg);
}
