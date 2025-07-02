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

package com.fujitsu.vdmj.po.statements;

import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.po.PONode;
import com.fujitsu.vdmj.po.annotations.POAnnotation;
import com.fujitsu.vdmj.po.annotations.POAnnotationList;
import com.fujitsu.vdmj.po.statements.visitors.POStatementStateUpdates;
import com.fujitsu.vdmj.po.statements.visitors.POStatementVisitor;
import com.fujitsu.vdmj.pog.POContextStack;
import com.fujitsu.vdmj.pog.POGState;
import com.fujitsu.vdmj.pog.ProofObligationList;
import com.fujitsu.vdmj.tc.lex.TCNameSet;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.typechecker.Environment;

/**
 * The parent class of all statements.
 */
public abstract class POStatement extends PONode
{
	private static final long serialVersionUID = 1L;

	/** The type of this sub-expression */
	private TCType stmttype;
	
	/** A list of annotations, if any. See POAnnotatedStatement */
	protected POAnnotationList annotations = new POAnnotationList();

	/**
	 * Create a statement at the given location.
	 * @param location
	 */
	public POStatement(LexLocation location)
	{
		super(location);
	}

	@Override
	abstract public String toString();

	/**
	 * Get a list of proof obligations from the statement.
	 *
	 * @param ctxt The call context.
	 * @param pogState The global context created by this statement, if any.
	 * @param env The Environment to lookup symbols.
	 * @return The list of proof obligations.
	 */
	public ProofObligationList getProofObligations(POContextStack ctxt, POGState pogState, Environment env)
	{
		return new ProofObligationList();
	}

	/**
	 * Get and set the statement type field.
	 * The setter returns the type too, so return T can change to return setType(T). 
	 */
	public TCType getStmttype()
	{
		return stmttype;
	}
	
	public TCType setStmttype(TCType stmttype)
	{
		this.stmttype = stmttype;
		return stmttype;
	}

	/**
	 * State variables updated or read by this statement.
	 */
	public TCNameSet updatesState()
	{
		POStatementStateUpdates visitor = new POStatementStateUpdates();
		return this.apply(visitor, null);
	}

	/**
	 * Add annotations from POAnnotatedAnnotation
	 */
	public void addAnnotation(POAnnotation annotation)
	{
		annotations.add(annotation);
	}
	
	/**
	 * Implemented by all definitions to allow visitor processing.
	 */
	abstract public <R, S> R apply(POStatementVisitor<R, S> visitor, S arg);
}
