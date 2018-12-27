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
 *	along with VDMJ.  If not, see <http://www.gnu.org/licenses/>.
 *
 ******************************************************************************/

package com.fujitsu.vdmj.po.annotations;

import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.po.statements.POStatement;
import com.fujitsu.vdmj.pog.POContextStack;
import com.fujitsu.vdmj.pog.ProofObligationList;

public class POAnnotatedStatement extends POStatement
{
	private static final long serialVersionUID = 1L;

	public final POAnnotation annotation;

	public final POStatement statement;
	
	public POAnnotatedStatement(LexLocation location, POAnnotation annotation, POStatement statement)
	{
		super(location);
		this.annotation = annotation;
		this.statement = statement;
	}

	@Override
	public String toString()
	{
		return "/* " + annotation + " */ " + statement;
	}
	
	@Override
	public ProofObligationList getProofObligations(POContextStack ctxt)
	{
		annotation.before(ctxt, this);
		ProofObligationList obligations = statement.getProofObligations(ctxt);
		annotation.after(ctxt, this, obligations);
		return obligations;
	}

	@Override
	public boolean hasSideEffects()
	{
		return statement.hasSideEffects();
	}
}
