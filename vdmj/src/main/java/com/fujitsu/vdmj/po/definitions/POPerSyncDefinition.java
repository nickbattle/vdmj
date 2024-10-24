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

package com.fujitsu.vdmj.po.definitions;

import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.po.annotations.POAnnotationList;
import com.fujitsu.vdmj.po.definitions.visitors.PODefinitionVisitor;
import com.fujitsu.vdmj.po.expressions.POExpression;
import com.fujitsu.vdmj.pog.POContextStack;
import com.fujitsu.vdmj.pog.POGState;
import com.fujitsu.vdmj.pog.PONameContext;
import com.fujitsu.vdmj.pog.ProofObligationList;
import com.fujitsu.vdmj.tc.lex.TCNameList;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.tc.types.TCBooleanType;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.typechecker.Environment;

public class POPerSyncDefinition extends PODefinition
{
	private static final long serialVersionUID = 1L;
	public final TCNameToken opname;
	public final POExpression guard;

	public POPerSyncDefinition(POAnnotationList annotations, LexLocation location, TCNameToken opname, POExpression guard)
	{
		super(location, opname.getPerName(location));
		this.annotations = annotations;
		this.opname = opname;
		this.guard = guard;
	}

	@Override
	public TCType getType()
	{
		return new TCBooleanType(location);
	}

	@Override
	public String toString()
	{
		return "per " + opname + " => " + guard;
	}

	@Override
	public ProofObligationList getProofObligations(POContextStack ctxt, POGState pogState, Environment env)
	{
		ProofObligationList obligations =
				(annotations != null) ? annotations.poBefore(this, ctxt) : new ProofObligationList();

		ctxt.push(new PONameContext(new TCNameList(opname)));
		obligations.addAll(guard.getProofObligations(ctxt, pogState, env));
		ctxt.pop();
		
		if (annotations != null) annotations.poAfter(this, obligations, ctxt);
		return obligations;
	}

	@Override
	public <R, S> R apply(PODefinitionVisitor<R, S> visitor, S arg)
	{
		return visitor.casePerSyncDefinition(this, arg);
	}
}
