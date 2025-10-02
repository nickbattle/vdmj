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

package com.fujitsu.vdmj.in.statements;

import com.fujitsu.vdmj.config.Properties;
import com.fujitsu.vdmj.in.INNode;
import com.fujitsu.vdmj.in.annotations.INAnnotation;
import com.fujitsu.vdmj.in.annotations.INAnnotationList;
import com.fujitsu.vdmj.in.statements.visitors.INStatementVisitor;
import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.runtime.Breakpoint;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.runtime.ContextException;
import com.fujitsu.vdmj.scheduler.InitThread;
import com.fujitsu.vdmj.scheduler.SchedulableThread;
import com.fujitsu.vdmj.values.Value;

/**
 * The parent class of all statements.
 */
public abstract class INStatement extends INNode
{
	private static final long serialVersionUID = 1L;

	/** The statement's breakpoint, if any. */
	public Breakpoint breakpoint;
	
	/** A list of annotations, if any. See POAnnotatedStatement */
	protected INAnnotationList annotations = new INAnnotationList();

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

	/** Evaluate the statement in the context given. */
	abstract public Value eval(Context ctxt);

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

		if (!(Thread.currentThread() instanceof SchedulableThread))		// eg. QuickCheck
		{
			throw new ContextException(4177, "Not permitted on this thread", location, ctxt);
		}
	}

	/**
	 * Add annotations from POAnnotatedAnnotation
	 */
	public INStatement addAnnotation(INAnnotation annotation)
	{
		annotations.add(annotation);
		return this;
	}

	/**
	 * Implemented by all statements to allow visitor processing.
	 */
	abstract public <R, S> R apply(INStatementVisitor<R, S> visitor, S arg);
}
