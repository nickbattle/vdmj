/*******************************************************************************
 *
 *	Copyright (c) 2018 Nick Battle.
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

package com.fujitsu.vdmj.po.annotations;

import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.po.statements.POStatement;
import com.fujitsu.vdmj.po.statements.visitors.POStatementVisitor;
import com.fujitsu.vdmj.pog.POContextStack;
import com.fujitsu.vdmj.pog.POGState;
import com.fujitsu.vdmj.pog.ProofObligationList;
import com.fujitsu.vdmj.typechecker.Environment;

public class POAnnotatedStatement extends POStatement
{
	private static final long serialVersionUID = 1L;

	public final POAnnotation annotation;
	public final POStatement statement;
	
	public POAnnotatedStatement(LexLocation location, POAnnotation annotation, POStatement statement)
	{
		super(location);
		this.annotation = (annotation != null) ? annotation : new PONoAnnotation();
		this.statement = statement;
		setStmttype(statement.getStmttype());

		POStatement stmt = this.statement;

		while (stmt instanceof POAnnotatedStatement)
		{
			POAnnotatedStatement astmt = (POAnnotatedStatement)stmt;
			stmt = astmt.statement;
		}

		stmt.addAnnotation(annotation);
	}

	@Override
	public String toString()
	{
		return statement.toString();	// Don't include annotation in PO source
	}
	
	@Override
	public ProofObligationList getProofObligations(POContextStack ctxt, POGState pogState, Environment env)
	{
		annotation.poBefore(this, ctxt);
		ProofObligationList obligations = statement.getProofObligations(ctxt, pogState, env);
		annotation.poAfter(this, obligations, ctxt);
		return obligations;
	}

	@Override
	public <R, S> R apply(POStatementVisitor<R, S> visitor, S arg)
	{
		return visitor.caseAnnotatedStatement(this, arg);
	}
}
