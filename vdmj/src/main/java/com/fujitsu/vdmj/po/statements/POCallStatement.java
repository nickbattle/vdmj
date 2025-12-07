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

import com.fujitsu.vdmj.Settings;
import com.fujitsu.vdmj.lex.Dialect;
import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.po.definitions.PODefinition;
import com.fujitsu.vdmj.po.definitions.PODefinitionList;
import com.fujitsu.vdmj.po.definitions.PODefinitionListList;
import com.fujitsu.vdmj.po.definitions.POExplicitFunctionDefinition;
import com.fujitsu.vdmj.po.definitions.POExplicitOperationDefinition;
import com.fujitsu.vdmj.po.definitions.POImplicitFunctionDefinition;
import com.fujitsu.vdmj.po.definitions.POImplicitOperationDefinition;
import com.fujitsu.vdmj.po.definitions.POStateDefinition;
import com.fujitsu.vdmj.po.expressions.POExpression;
import com.fujitsu.vdmj.po.expressions.POExpressionList;
import com.fujitsu.vdmj.po.expressions.POVariableExpression;
import com.fujitsu.vdmj.po.statements.visitors.POStatementVisitor;
import com.fujitsu.vdmj.pog.FunctionApplyObligation;
import com.fujitsu.vdmj.pog.OperationPreConditionObligation;
import com.fujitsu.vdmj.pog.POContextStack;
import com.fujitsu.vdmj.pog.POGState;
import com.fujitsu.vdmj.pog.POSaveStateContext;
import com.fujitsu.vdmj.pog.ProofObligation;
import com.fujitsu.vdmj.pog.ProofObligationList;
import com.fujitsu.vdmj.pog.RecursiveObligation;
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
	public final PODefinitionListList recursiveCycles;

	public POCallStatement(TCNameToken name, POExpressionList args, PODefinition opdef, PODefinitionListList recursiveCycles)
	{
		super(name.getLocation());
		this.name = name;
		this.args = args;
		this.opdef = opdef;
		this.recursiveCycles = recursiveCycles;
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
			POExpression extracted = extractOpCalls(exp, obligations, pogState, ctxt, env);
			obligations.addAll(extracted.getProofObligations(ctxt, pogState, env));

			TCType pt = paramTypes.get(i);
			TCType at = extracted.getExptype();
			
			if (!TypeComparator.isSubType(at, pt))
			{
				obligations.addAll(SubTypeObligation.getAllPOs(extracted, pt, at, ctxt));
			}

			i++;
		}

		if (Settings.dialect != Dialect.VDM_SL)
		{
			// Precondition calling is not defined for PP dialects...
			ProofObligationList checks = new ProofObligationList();
			POExpression root = new POVariableExpression(opdef.name, opdef);

			if (opdef instanceof POExplicitOperationDefinition)
			{
				POExplicitOperationDefinition exop = (POExplicitOperationDefinition)opdef;

				if (exop.precondition != null)
				{
					checks.addAll(OperationPreConditionObligation.getAllPOs(
						location, root, args, FunctionApplyObligation.UNKNOWN, ctxt));
				}
			}
			else if (opdef instanceof POImplicitOperationDefinition)
			{
				POImplicitOperationDefinition imop = (POImplicitOperationDefinition)opdef;

				if (imop.precondition != null)
				{
					checks.addAll(OperationPreConditionObligation.getAllPOs(
						location, root, args, FunctionApplyObligation.UNKNOWN, ctxt));
				}
			}

			checks.markUnchecked(ProofObligation.UNCHECKED_VDMPP);
			obligations.addAll(checks);
		}
		else
		{
			obligations.addAll(checkPrecondition(location, opdef, args, ctxt));
		}

		if (recursiveCycles != null)	// name is a func/op in a recursive loop
		{
			for (PODefinitionList loop: recursiveCycles)
			{
				obligations.addAll(RecursiveObligation.getAllPOs(location, loop, this, ctxt));
			}
		}

		ctxt.makeOperationCall(location, opdef, args, null, getStmttype().isReturn(), pogState, env);
		
		TCType rtype = pogState.getResultType();
		
		if (rtype != null && getStmttype().isReturn() && !TypeComparator.isSubType(getStmttype(), rtype))
		{
			PODefinition calledFrom = ctxt.getDefinition();

			if (!calledFrom.isConstructor())	// Strange subtype rules don't apply to ctors
			{
				obligations.addAll(SubTypeObligation.getAllPOs(location, ctxt.getDefinition(), getStmttype(), ctxt));
			}
		}

		return obligations;
	}

	public static ProofObligationList checkPrecondition(LexLocation location,
		PODefinition opdef, POExpressionList args, POContextStack ctxt)
	{
		ProofObligationList obligations = new ProofObligationList();
		
		if (opdef != null && Settings.dialect == Dialect.VDM_SL)
		{
			String prename = null;
			PODefinition predef = null;
			POStateDefinition targetState = null;

			if (opdef instanceof POExplicitOperationDefinition)
			{
				POExplicitOperationDefinition exop = (POExplicitOperationDefinition)opdef;

				if (exop.predef != null)
				{
					predef = exop.predef;
					prename = predef.name.toExplicitString(location);
					targetState = exop.stateDefinition;
				}
			}
			else if (opdef instanceof POImplicitOperationDefinition)
			{
				POImplicitOperationDefinition imop = (POImplicitOperationDefinition)opdef;

				if (imop.predef != null)
				{
					predef = imop.predef;
					prename = predef.name.toExplicitString(location);
					targetState = imop.stateDefinition;
				}
			}

			if (prename != null)
			{
				POExpression root = new POVariableExpression(opdef.name, opdef);

				if (opdef.location.sameModule(location))		// Local?
				{
					POExpressionList preargs = new POExpressionList();
					preargs.addAll(args);

					if (targetState != null)
					{
						preargs.add(targetState.getMkExpression(location));
					}

					obligations.addAll(OperationPreConditionObligation.getAllPOs(location, root, preargs, prename, ctxt));
				}
				else	// target is another module
				{
					POExpressionList preargs = new POExpressionList();
					preargs.addAll(args);
					
					if (targetState != null)
					{
						POSaveStateContext.advance();
						ctxt.push(new POSaveStateContext(predef, location, false));
						preargs.add(new POVariableExpression(targetState.getPatternName(location), null));
					}
					
					ProofObligationList checks = new ProofObligationList();
					checks.addAll(OperationPreConditionObligation.getAllPOs(location, root, preargs, prename, ctxt));
					obligations.addAll(checks);

					if (targetState != null)
					{
						ctxt.pop();	// Remove forall, ready for op call context
					}
				}
			}
		}

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
	
	/**
	 * Create a measure application string from this apply, turning the root function
	 * name into the measure name passed. 
	 */
	public String getMeasureApply(String measure)
	{
		StringBuilder sb = new StringBuilder(name.getMeasureName(location).getName());
		sb.append("(");
		String separator = "";
		
		for (POExpression arg: args)
		{
			sb.append(separator);
			sb.append(Utils.deBracketed(arg));
			separator = ", ";
		}

		if (opdef instanceof POExplicitOperationDefinition)
		{
			POExplicitOperationDefinition exop = (POExplicitOperationDefinition)opdef;

			if (exop.stateDefinition != null)
			{
				sb.append(separator);
				sb.append(exop.stateDefinition.toPattern(false, location));
			}
			else if (exop.classDefinition != null)
			{
				sb.append(separator);
				sb.append(exop.classDefinition.toPattern(false, location));
			}
		}
		else if (opdef instanceof POImplicitOperationDefinition)
		{
			POImplicitOperationDefinition imop = (POImplicitOperationDefinition)opdef;

			if (imop.stateDefinition != null)
			{
				sb.append(separator);
				sb.append(imop.stateDefinition.toPattern(false, location));
			}
			else if (imop.classDefinition != null)
			{
				sb.append(separator);
				sb.append(imop.classDefinition.toPattern(false, location));
			}
		}

		sb.append(")");
		return sb.toString();
	}

	@Override
	public <R, S> R apply(POStatementVisitor<R, S> visitor, S arg)
	{
		return visitor.caseCallStatement(this, arg);
	}
}
