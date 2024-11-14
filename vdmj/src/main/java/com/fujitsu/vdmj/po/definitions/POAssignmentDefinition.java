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

import com.fujitsu.vdmj.po.definitions.visitors.PODefinitionVisitor;
import com.fujitsu.vdmj.po.expressions.POExpression;
import com.fujitsu.vdmj.pog.POContextStack;
import com.fujitsu.vdmj.pog.POGState;
import com.fujitsu.vdmj.pog.ProofObligationList;
import com.fujitsu.vdmj.pog.SubTypeObligation;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.TypeComparator;

/**
 * A class to represent assignable variable definitions.
 */
public class POAssignmentDefinition extends PODefinition
{
	private static final long serialVersionUID = 1L;

	public final TCType type;
	public final POExpression expression;
	public final TCType expType;

	public POAssignmentDefinition(TCNameToken name, TCType type, POExpression expression, TCType expType)
	{
		super(name.getLocation(), name);
		this.type = type;
		this.expression = expression;
		this.expType = expType;
	}
	
	@Override
	public String toString()
	{
		return name + ":" + type + " := " + expression;
	}

	@Override
	public TCType getType()
	{
		return type;
	}

	@Override
	public ProofObligationList getProofObligations(POContextStack ctxt, POGState pogState, Environment env)
	{
		ProofObligationList obligations = new ProofObligationList();
		obligations.addAll(expression.getProofObligations(ctxt, pogState, env));
		obligations.markIfUpdated(pogState, expression);

		if (!TypeComparator.isSubType(ctxt.checkType(expression, expType), type))
		{
			obligations.add(new SubTypeObligation(expression, type, expType, ctxt));
		}

		return obligations;
	}

	@Override
	public <R, S> R apply(PODefinitionVisitor<R, S> visitor, S arg)
	{
		return visitor.caseAssignmentDefinition(this, arg);
	}
}
