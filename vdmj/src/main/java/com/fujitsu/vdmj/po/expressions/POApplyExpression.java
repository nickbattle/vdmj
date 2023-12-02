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

import com.fujitsu.vdmj.po.definitions.PODefinitionList;
import com.fujitsu.vdmj.po.definitions.PODefinitionListList;
import com.fujitsu.vdmj.po.expressions.visitors.POExpressionVisitor;
import com.fujitsu.vdmj.pog.FunctionApplyObligation;
import com.fujitsu.vdmj.pog.MapApplyObligation;
import com.fujitsu.vdmj.pog.POContextStack;
import com.fujitsu.vdmj.pog.ProofObligationList;
import com.fujitsu.vdmj.pog.RecursiveObligation;
import com.fujitsu.vdmj.pog.SeqApplyObligation;
import com.fujitsu.vdmj.pog.SubTypeObligation;
import com.fujitsu.vdmj.tc.definitions.TCDefinition;
import com.fujitsu.vdmj.tc.definitions.TCExplicitFunctionDefinition;
import com.fujitsu.vdmj.tc.types.TCMapType;
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
	public final PODefinitionListList recursive;

	public POApplyExpression(POExpression root, POExpressionList args,
		TCType type, TCTypeList argtypes, PODefinitionListList recursive)
	{
		super(root);
		this.root = root;
		this.args = args;
		this.type = type;
		this.argtypes = argtypes;
		this.recursive = recursive;
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
	public ProofObligationList getProofObligations(POContextStack ctxt, Environment env)
	{
		ProofObligationList obligations = new ProofObligationList();

		if (type.isMap(location))
		{
			TCMapType m = type.getMap();
			obligations.add(new MapApplyObligation(root, args.get(0), ctxt));
			TCType atype = ctxt.checkType(args.get(0), argtypes.get(0));

			if (!TypeComparator.isSubType(atype, m.from))
			{
				obligations.add(new SubTypeObligation(args.get(0), m.from, atype, ctxt));
			}
		}
		
		if (!type.isUnknown(location) &&
			(type.isFunction(location) || type.isOperation(location)))
		{
			String prename = root.getPreName();

			if (type.isFunction(location) && prename != null && !prename.isEmpty())
			{
				obligations.add(new FunctionApplyObligation(root, args, prename, ctxt));
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
					obligations.add(new SubTypeObligation(args.get(i), pt, at, ctxt));
				}

				i++;
			}
		}

		if (!type.isUnknown(location) && type.isFunction(location))
		{
			if (recursive != null)	// name is a function in a recursive loop
			{
				/**
				 * All of the functions in the loop will generate similar obligations,
				 * so the "add" method eliminates any duplicates.
				 */
				for (PODefinitionList loop: recursive)
				{
					obligations.add(new RecursiveObligation(location, loop, this, ctxt));
				}
			}
		}

		if (type.isSeq(location))
		{
			obligations.add(new SeqApplyObligation(root, args.get(0), ctxt));
		}

		obligations.addAll(root.getProofObligations(ctxt, env));

		for (POExpression arg: args)
		{
			obligations.addAll(arg.getProofObligations(ctxt, env));
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
		if (root instanceof POFuncInstantiationExpression)
		{
			String pn = root.getPreName();
			
			if (pn != null && !pn.isEmpty())
			{
				return pn + "(" + Utils.listToString(args) + ")";
			}
		}
		else if (type.definitions != null && !type.definitions.isEmpty())
		{
			TCDefinition def = type.definitions.firstElement();
			
			if (def instanceof TCExplicitFunctionDefinition)
			{
				TCExplicitFunctionDefinition exdef = (TCExplicitFunctionDefinition)def;
				
				if (exdef.isCurried)
				{
					String pn = root.getPreName();
					
					if (pn != null && !pn.isEmpty())
					{
						return pn + "(" + Utils.listToString(args) + ")";
					}
				}
			}
		}
		
		return null;	// Already generated elsewhere
	}

	@Override
	public <R, S> R apply(POExpressionVisitor<R, S> visitor, S arg)
	{
		return visitor.caseApplyExpression(this, arg);
	}
}
