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

package com.fujitsu.vdmj.po.statements;

import com.fujitsu.vdmj.po.definitions.PODefinition;
import com.fujitsu.vdmj.po.definitions.POExplicitFunctionDefinition;
import com.fujitsu.vdmj.po.definitions.POExplicitOperationDefinition;
import com.fujitsu.vdmj.po.definitions.POImplicitFunctionDefinition;
import com.fujitsu.vdmj.po.definitions.POImplicitOperationDefinition;
import com.fujitsu.vdmj.po.expressions.POExpression;
import com.fujitsu.vdmj.po.expressions.POExpressionList;
import com.fujitsu.vdmj.po.statements.visitors.POStatementVisitor;
import com.fujitsu.vdmj.pog.POContextStack;
import com.fujitsu.vdmj.pog.POGState;
import com.fujitsu.vdmj.pog.ProofObligationList;
import com.fujitsu.vdmj.pog.SubTypeObligation;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.TCTypeList;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.TypeComparator;
import com.fujitsu.vdmj.util.Utils;

public class POCallStatement extends POStatement
{
	private static final long serialVersionUID = 1L;
	public final TCNameToken name;
	public final POExpressionList args;
	public final PODefinition opdef;

	public POCallStatement(TCNameToken name, POExpressionList args, PODefinition opdef)
	{
		super(name.getLocation());
		this.name = name;
		this.args = args;
		this.opdef = opdef;
	}

	@Override
	public String toString()
	{
		return name.getName() + "(" + Utils.listToString(args) + ")";
	}

	@Override
	public ProofObligationList getProofObligations(POContextStack ctxt, POGState pogState, Environment env)
	{
		ProofObligationList obligations = new ProofObligationList();
		TCTypeList paramTypes = getParamTypes();
		int i = 0;

		for (POExpression exp: args)
		{
			obligations.addAll(exp.getProofObligations(ctxt, pogState, env));

			TCType pt = paramTypes.get(i);
			TCType at = exp.getExptype();
			
			if (!TypeComparator.isSubType(at, pt))
			{
				obligations.add(new SubTypeObligation(args.get(i), pt, at, ctxt));
			}

			i++;
		}

		pogState.addOperationCall(location, opdef);

		return obligations;
	}

	private TCTypeList getParamTypes()
	{
		if (opdef instanceof POExplicitOperationDefinition)
		{
			POExplicitOperationDefinition exop = (POExplicitOperationDefinition)opdef;
			return exop.type.parameters;
		}
		else if (opdef instanceof POImplicitOperationDefinition)
		{
			POImplicitOperationDefinition imop = (POImplicitOperationDefinition)opdef;
			return imop.type.parameters;
		}
		else if (opdef instanceof POExplicitFunctionDefinition)
		{
			POExplicitFunctionDefinition exfn = (POExplicitFunctionDefinition)opdef;
			return exfn.type.parameters;
		}
		else if (opdef instanceof POImplicitFunctionDefinition)
		{
			POImplicitFunctionDefinition imfn = (POImplicitFunctionDefinition)opdef;
			return imfn.type.parameters;
		}
		else	// Should never happen, but don't fail
		{
			TCTypeList list = new TCTypeList();
			
			for (POExpression arg: args)
			{
				list.add(arg.getExptype());
			}
			
			return list;
		}
	}

	@Override
	public <R, S> R apply(POStatementVisitor<R, S> visitor, S arg)
	{
		return visitor.caseCallStatement(this, arg);
	}
}
