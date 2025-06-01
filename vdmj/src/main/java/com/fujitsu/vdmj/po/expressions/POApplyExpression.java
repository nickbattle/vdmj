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

package com.fujitsu.vdmj.po.expressions;

import com.fujitsu.vdmj.po.definitions.PODefinition;
import com.fujitsu.vdmj.po.definitions.PODefinitionList;
import com.fujitsu.vdmj.po.definitions.PODefinitionListList;
import com.fujitsu.vdmj.po.expressions.visitors.POExpressionVisitor;
import com.fujitsu.vdmj.pog.FunctionApplyObligation;
import com.fujitsu.vdmj.pog.MapApplyObligation;
import com.fujitsu.vdmj.pog.POContextStack;
import com.fujitsu.vdmj.pog.POGState;
import com.fujitsu.vdmj.pog.ProofObligationList;
import com.fujitsu.vdmj.pog.RecursiveObligation;
import com.fujitsu.vdmj.pog.SeqApplyObligation;
import com.fujitsu.vdmj.pog.SubTypeObligation;
import com.fujitsu.vdmj.tc.types.TCMapType;
import com.fujitsu.vdmj.tc.types.TCSeq1Type;
import com.fujitsu.vdmj.tc.types.TCSeqType;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.TCTypeList;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.TypeComparator;
import com.fujitsu.vdmj.util.Utils;

public class POApplyExpression extends POExpression
{
	private static final long serialVersionUID = 1L;

	public final POExpression root;
	public final POExpressionList args;
	public final TCType type;
	public final TCTypeList argtypes;
	public final PODefinitionListList recursiveCycles;
	public final PODefinition opdef;

	public POApplyExpression(POExpression root, POExpressionList args,
		TCType type, TCTypeList argtypes, PODefinitionListList recursiveCycles, PODefinition opdef)
	{
		super(root);
		this.root = root;
		this.args = args;
		this.type = type;
		this.argtypes = argtypes;
		this.recursiveCycles = recursiveCycles;
		this.opdef = opdef;
	}

	@Override
	public String toString()
	{
		if (root instanceof POVariableExpression)
		{
			POVariableExpression v = (POVariableExpression)root;
			// Exclude the param types from the TCNameToken...
			
			if (!v.name.getModule().equals(location.module))
			{
				return v.name.getModule() + "`" + v.name.getName() + "("+ Utils.listToString(args) + ")";
			}
			else
			{
				return v.name.getName() + "("+ Utils.listToString(args) + ")";
			}
		}

		return root + "("+ Utils.listToString(args) + ")";
	}

	@Override
	public ProofObligationList getProofObligations(POContextStack ctxt, POGState pogState, Environment env)
	{
		ProofObligationList obligations = new ProofObligationList();

		if (!type.isUnknown(location))
		{
			if (type.isMap(location))
			{
				TCMapType m = type.getMap();
				obligations.addAll(MapApplyObligation.getAllPOs(root, args.get(0), ctxt));
				TCType atype = ctxt.checkType(args.get(0), argtypes.get(0));
	
				if (!TypeComparator.isSubType(atype, m.from))
				{
					obligations.addAll(SubTypeObligation.getAllPOs(args.get(0), m.from, atype, ctxt));
				}
			}
			
			if (type.isFunction(location) || type.isOperation(location))
			{
				String prename = root.getPreName();
	
				if (type.isFunction(location) && prename != null && !prename.isEmpty())
				{
					obligations.addAll(FunctionApplyObligation.getAllPOs(root, args, prename, ctxt));
				}
				
				TCTypeList paramTypes = type.isFunction(location) ?
						type.getFunction().parameters : type.getOperation().parameters;
					
				int i=0;
	
				for (TCType at: argtypes)
				{
					at = ctxt.checkType(args.get(i), at);
					TCType pt = paramTypes.get(i);
	
					if (!TypeComparator.isSubType(at, pt))
					{
						obligations.addAll(SubTypeObligation.getAllPOs(args.get(i), pt, at, ctxt));
					}
	
					i++;
				}
			}
	
			if (type.isFunction(location))
			{
				if (recursiveCycles != null)	// name is a function in a recursive loop
				{
					/**
					 * All of the functions in the loop will generate similar obligations,
					 * so the "add" method eliminates any duplicates.
					 */
					for (PODefinitionList loop: recursiveCycles)
					{
						obligations.addAll(RecursiveObligation.getAllPOs(location, loop, this, ctxt));
					}
				}
			}
	
			if (type.isSeq(location))
			{
				TCSeqType st = type.getSeq();
				POExpression arg = args.get(0);
				
				if (st instanceof TCSeq1Type && arg instanceof POIntegerLiteralExpression)
				{
					POIntegerLiteralExpression e = (POIntegerLiteralExpression)arg;
					
					if (e.value.value != 1)		// s(1) is always okay for seq1
					{
						obligations.addAll(SeqApplyObligation.getAllPOs(root, arg, ctxt));
					}
				}
				else
				{
					obligations.addAll(SeqApplyObligation.getAllPOs(root, arg, ctxt));
				}
			}
			
			if (type.isOperation(location))
			{
				// Mark the context stack as ambiguous, if needed. This marks subsequent POs Unchecked.
				
				ctxt.addOperationCall(location, pogState, opdef, false);
				
				// Additionally, we mark the result as generally ambiguous, so that if this expression
				// is being used to define something in a "let", we can mark that as ambiguous too.
				
				pogState.setAmbiguous(true);
			}
		}

		obligations.addAll(root.getProofObligations(ctxt, pogState, env));

		for (POExpression arg: args)
		{
			obligations.addAll(arg.getProofObligations(ctxt, pogState, env));
		}

		return obligations;
	}
	
	public String getMeasureApply(String measure)
	{
		return getMeasureApply(measure, true);
	}
	
	/**
	 * Create a measure application string from this apply, turning the root function
	 * name into the measure name passed. 
	 */
	private String getMeasureApply(String measure, boolean close)
	{
		String start = null;
		
		if (root instanceof POApplyExpression)
		{
			POApplyExpression aexp = (POApplyExpression)root;
			start = aexp.getMeasureApply(measure, false);
		}
		else if (root instanceof POVariableExpression)
		{
			start = measure;
		}
		else if (root instanceof POFuncInstantiationExpression)
		{
			POFuncInstantiationExpression fie = (POFuncInstantiationExpression)root;
			start = measure + "[" + Utils.listToString(fie.actualTypes) + "]";
		}
		else
		{
			start = root.toString();
		}

		StringBuilder sb = new StringBuilder(start);
		sb.append("(");
		String separator = "";
		
		for (POExpression arg: args)
		{
			sb.append(separator);
			sb.append(Utils.deBracketed(arg));
			separator = ", ";
		}

		sb.append(")");
		return sb.toString();
	}
	
	/**
	 * This is used in apply chains or curried calls, where the precondition is needed
	 * at the end of the chain.
	 */
	@Override
	public String getPreName()
	{
		if (root.getPreName() == null)
		{
			return null;
		}
		
		return FunctionApplyObligation.UNKNOWN;		// Use pre_(root, args) form
	}

	@Override
	public <R, S> R apply(POExpressionVisitor<R, S> visitor, S arg)
	{
		return visitor.caseApplyExpression(this, arg);
	}
}
