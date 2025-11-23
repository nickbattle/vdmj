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

import com.fujitsu.vdmj.ast.lex.LexNameToken;
import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.po.definitions.PODefinition;
import com.fujitsu.vdmj.po.definitions.POExplicitFunctionDefinition;
import com.fujitsu.vdmj.po.definitions.POExplicitOperationDefinition;
import com.fujitsu.vdmj.po.definitions.POImplicitFunctionDefinition;
import com.fujitsu.vdmj.po.definitions.POImplicitOperationDefinition;
import com.fujitsu.vdmj.po.expressions.POExpression;
import com.fujitsu.vdmj.po.expressions.POExpressionList;
import com.fujitsu.vdmj.po.expressions.POFieldExpression;
import com.fujitsu.vdmj.po.statements.visitors.POStatementVisitor;
import com.fujitsu.vdmj.pog.FunctionApplyObligation;
import com.fujitsu.vdmj.pog.OperationPreConditionObligation;
import com.fujitsu.vdmj.pog.POContextStack;
import com.fujitsu.vdmj.pog.POGState;
import com.fujitsu.vdmj.pog.ProofObligation;
import com.fujitsu.vdmj.pog.ProofObligationList;
import com.fujitsu.vdmj.pog.SubTypeObligation;
import com.fujitsu.vdmj.tc.lex.TCIdentifierToken;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.TCTypeList;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.TypeComparator;
import com.fujitsu.vdmj.util.Utils;

public class POCallObjectStatement extends POStatement
{
	private static final long serialVersionUID = 1L;
	public final POObjectDesignator designator;
	public final TCNameToken classname;
	public final TCIdentifierToken fieldname;
	public final POExpressionList args;
	public final PODefinition fdef;

	public LexNameToken field = null;

	public POCallObjectStatement(LexLocation location, POObjectDesignator designator,
		TCNameToken classname, TCIdentifierToken fieldname, POExpressionList args, PODefinition fdef)
	{
		super(location);

		this.designator = designator;
		this.classname = classname;
		this.fieldname = fieldname;
		this.args = args;
		this.fdef = fdef;
	}

	@Override
	public String toString()
	{
		return designator + "." +
			(classname != null ? classname : fieldname) +
			"(" + Utils.listToString(args) + ")";
	}

	@Override
	public ProofObligationList getProofObligations(POContextStack ctxt, POGState pogState, Environment env)
	{
		ProofObligationList obligations = new ProofObligationList();
		TCTypeList paramTypes = getParamTypes();
		int i = 0;

		for (POExpression exp: args)
		{
			POExpression extracted = extractOpCalls(exp, obligations, pogState, ctxt, env);
			obligations.addAll(extracted.getProofObligations(ctxt, pogState, env));

			TCType pt = paramTypes.get(i);
			TCType at = exp.getExptype();
			
			if (!TypeComparator.isSubType(at, pt))
			{
				obligations.addAll(SubTypeObligation.getAllPOs(args.get(i), pt, at, ctxt));
			}

			i++;
		}

		// Precondition calling is not defined for PP dialects...
		ProofObligationList checks = new ProofObligationList();
		POFieldExpression field = new POFieldExpression(designator.getExpression(), fieldname, classname);
		checks.addAll(OperationPreConditionObligation.getAllPOs(location, field, args, FunctionApplyObligation.UNKNOWN, ctxt));
		checks.markUnchecked(ProofObligation.UNCHECKED_VDMPP);
		obligations.addAll(checks);

		ctxt.makeOperationCall(location, fdef, args, null, getStmttype().isReturn(), pogState, env);
		
		TCType rtype = pogState.getResultType();
		
		if (rtype != null && getStmttype().isReturn() && !TypeComparator.isSubType(getStmttype(), rtype))
		{
			if (!isConstructor(fdef))	// Can return their superclass' type (see TC)
			{
				obligations.addAll(SubTypeObligation.getAllPOs(location, ctxt.getDefinition(), getStmttype(), ctxt));
			}
		}
		
		return obligations;
	}

	private TCTypeList getParamTypes()
	{
		if (fdef instanceof POExplicitOperationDefinition)
		{
			POExplicitOperationDefinition exop = (POExplicitOperationDefinition)fdef;
			return exop.type.parameters;
		}
		else if (fdef instanceof POImplicitOperationDefinition)
		{
			POImplicitOperationDefinition imop = (POImplicitOperationDefinition)fdef;
			return imop.type.parameters;
		}
		else if (fdef instanceof POExplicitFunctionDefinition)
		{
			POExplicitFunctionDefinition exfn = (POExplicitFunctionDefinition)fdef;
			return exfn.type.parameters;
		}
		else if (fdef instanceof POImplicitFunctionDefinition)
		{
			POImplicitFunctionDefinition imfn = (POImplicitFunctionDefinition)fdef;
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
		return visitor.caseCallObjectStatement(this, arg);
	}
}
