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

package com.fujitsu.vdmj.tc.expressions;

import com.fujitsu.vdmj.Release;
import com.fujitsu.vdmj.Settings;
import com.fujitsu.vdmj.ast.lex.LexNameToken;
import com.fujitsu.vdmj.tc.TCRecursiveCycles;
import com.fujitsu.vdmj.tc.definitions.TCDefinition;
import com.fujitsu.vdmj.tc.definitions.TCDefinitionListList;
import com.fujitsu.vdmj.tc.definitions.TCExplicitFunctionDefinition;
import com.fujitsu.vdmj.tc.expressions.visitors.TCExpressionVisitor;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.tc.statements.TCStatement;
import com.fujitsu.vdmj.tc.types.TCFunctionType;
import com.fujitsu.vdmj.tc.types.TCMapType;
import com.fujitsu.vdmj.tc.types.TCOperationType;
import com.fujitsu.vdmj.tc.types.TCSeqType;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.TCTypeList;
import com.fujitsu.vdmj.tc.types.TCTypeSet;
import com.fujitsu.vdmj.tc.types.TCUnknownType;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.NameScope;
import com.fujitsu.vdmj.typechecker.TypeComparator;
import com.fujitsu.vdmj.util.Utils;

public class TCApplyExpression extends TCExpression
{
	private static final long serialVersionUID = 1L;

	public final TCExpression root;
	public final TCExpressionList args;

	public TCType type;
	public TCTypeList argtypes;
	
	// Used by PO
	public TCDefinitionListList recursiveCycles = null;
	public TCDefinition opdef = null;
	public Boolean inFunction = null;

	public TCApplyExpression(TCExpression root, TCExpressionList args)
	{
		super(root);
		this.root = root;
		this.args = args;
	}

	@Override
	public String toString()
	{
		return root + "("+ Utils.listToString(args) + ")";
	}

	@Override
	public TCType typeCheck(Environment env, TCTypeList qualifiers, NameScope scope, TCType constraint)
	{
		argtypes = new TCTypeList();

		for (TCExpression a: args)
		{
			argtypes.add(a.typeCheck(env, null, scope, null));
		}

		type = root.typeCheck(env, argtypes, scope, null);

		if (type.isUnknown(location))
		{
			return setType(type);
		}

		TCDefinition encldef = env.getEnclosingDefinition();

		this.inFunction = env.isFunctional();
		boolean inOperation = !inFunction;
		boolean inReserved = (encldef == null || encldef.name == null) ? false : encldef.name.isReserved();
			
		{
			TCDefinition calling = getRootDefinition(env, scope);

			if (calling instanceof TCExplicitFunctionDefinition)
			{
				TCExplicitFunctionDefinition def = (TCExplicitFunctionDefinition)calling;
				
				if (def.isCurried)
				{
					// Only recursive if this apply is the last - so our type is not a function.
					
					if (type instanceof TCFunctionType && ((TCFunctionType)type).result instanceof TCFunctionType)
					{
						calling = null;
					}
				}
			}
			
			if (encldef != null && calling != null && calling.isFunctionOrOperation())
			{
				recursiveCycles = new TCDefinitionListList();
				TCRecursiveCycles.getInstance().addCaller(encldef, recursiveCycles, calling);
			}
		}

		boolean isSimple = !type.isUnion(location);
		TCTypeSet results = new TCTypeSet();

		if (type.isFunction(location))
		{
			TCFunctionType ft = type.getFunction();
			
			if (ft.instantiated != null && !ft.instantiated)
			{
				// Something like f(x) rather than f[nat](x)
				root.report(3350, "Polymorphic function has not been instantiated");
			}
			
			ft.typeResolve(env);
			results.add(functionApply(isSimple, ft));
		}

		if (type.isOperation(location))
		{
			if (root instanceof TCVariableExpression)
			{
				TCVariableExpression exp = (TCVariableExpression)root;
				opdef = env.findName(exp.name, scope);
				
				if (opdef != null && TCStatement.isConstructor(opdef) && !TCStatement.inConstructor(env))
				{
					report(3337, "Cannot call a constructor from here");
					results.add(new TCUnknownType(location));
				}
			}
			else if (root instanceof TCFieldExpression)
			{
				TCFieldExpression exp = (TCFieldExpression)root;
				opdef = env.findName(exp.memberName, scope);
			}
			
			TCOperationType ot = type.getOperation();
			ot.typeResolve(env);

			if (inFunction && Settings.release == Release.VDM_10 && !ot.pure)
			{
				if (env.isFunctionalError())
				{
					report(3300, "Impure operation '" + root + "' cannot be called from here");
				}
				else
				{
					warning(5033, "Impure operation '" + root + "' cannot be called from here");
				}
				
				results.add(new TCUnknownType(location));
			}
			else if (inOperation && Settings.release == Release.VDM_10 && encldef != null && encldef.isPure() && !ot.pure)
			{
				report(3339, "Cannot call impure operation '" + root + "' from a pure operation");
				results.add(new TCUnknownType(location));
			}
			else
			{
    			results.add(operationApply(isSimple, ot));
			}
			
			if (inFunction && Settings.release == Release.VDM_10 && ot.pure && !inReserved)
			{
				warning(5017, "Pure operation call may not be referentially transparent");
			}
		}

		if (type.isSeq(location))
		{
			TCSeqType seq = type.getSeq();
			results.add(sequenceApply(isSimple, seq));
		}

		if (type.isMap(location))
		{
			TCMapType map = type.getMap();
			results.add(mapApply(isSimple, map));
		}

		if (results.isEmpty())
		{
			report(3054, "Type " + type + " cannot be applied");
			return setType(new TCUnknownType(location));
		}

		// If a constraint is passed in, we can raise an error if it is
		// not possible for the type to match the constraint (rather than
		// certain, as checkConstraint would).
		
		return possibleConstraint(constraint, results.getType(location));
	}

	private TCType sequenceApply(boolean isSimple, TCSeqType seq)
	{
		if (args.size() != 1)
		{
			concern(isSimple, 3055, "Sequence selector must have one argument");
		}
		else if (!argtypes.get(0).isNumeric(location))
		{
			concern(isSimple, 3056, "Sequence application argument must be numeric");
		}
		else if (seq.empty)
		{
			concern(isSimple, 3268, "Empty sequence cannot be applied");
		}
		
		if (args.size() == 1 && args.get(0) instanceof TCIntegerLiteralExpression)
		{
			TCIntegerLiteralExpression index = (TCIntegerLiteralExpression)args.get(0);
			
			if (index.value.value <= 0)		// Common enough to explicitly check!
			{
				concern(isSimple, 3056, "Sequence application argument must > 0");	
			}
		}

		return seq.seqof;
	}

	private TCType mapApply(boolean isSimple, TCMapType map)
	{
		if (args.size() != 1)
		{
			concern(isSimple, 3057, "Map application must have one argument");
		}
		else if (map.empty)
		{
			concern(isSimple, 3267, "Empty map cannot be applied");
		}

		TCType argtype = argtypes.get(0);

		if (!TypeComparator.compatible(map.from, argtype))
		{
			concern(isSimple, 3058, "Map application argument is incompatible type");
			detail2(isSimple, "Map domain", map.from, "Argument", argtype);
		}

		return map.to;
	}

	private TCType functionApply(boolean isSimple, TCFunctionType ftype)
	{
		TCTypeList ptypes = ftype.parameters;

		if (args.size() > ptypes.size())
		{
			concern(isSimple, 3059, "Too many arguments");
			detail2(isSimple, "Args", args, "Params", ptypes);
			return ftype.result;
		}
		else if (args.size() < ptypes.size())
		{
			concern(isSimple, 3060, "Too few arguments");
			detail2(isSimple, "Args", args, "Params", ptypes);
			return ftype.result;
		}

		int i=0;

		for (TCType at: argtypes)
		{
			TCType pt = ptypes.get(i++);

			if (!TypeComparator.compatible(pt, at))
			{
				args.get(i-1).concern(isSimple, 3061, "Inappropriate type for argument " + i);
				detail2(isSimple, "Expect", pt, "Actual", at);
			}
			else if (at instanceof TCFunctionType)
			{
				TCFunctionType fat = (TCFunctionType)at;
				
				if (fat.instantiated != null && !fat.instantiated)
				{
					// Cannot pass uninstantiated polymorphic function arguments
					args.get(i-1).concern(isSimple, 3354, "Function argument must be instantiated");
				}
			}
		}

		return ftype.result;
	}

	private TCType operationApply(boolean isSimple, TCOperationType optype)
	{
		TCTypeList ptypes = optype.parameters;

		if (args.size() > ptypes.size())
		{
			concern(isSimple, 3062, "Too many arguments");
			detail2(isSimple, "Args", args, "Params", ptypes);
			return optype.result;
		}
		else if (args.size() < ptypes.size())
		{
			concern(isSimple, 3063, "Too few arguments");
			detail2(isSimple, "Args", args, "Params", ptypes);
			return optype.result;
		}

		int i=0;

		for (TCType at: argtypes)
		{
			TCType pt = ptypes.get(i++);

			if (!TypeComparator.compatible(pt, at))
			{
				concern(isSimple, 3064, "Inappropriate type for argument " + i);
				detail2(isSimple, "Expect", pt, "Actual", at);
			}
		}

		return optype.result;
	}

	private TCDefinition getRootDefinition(Environment env, NameScope scope)
	{
		TCNameToken fname = null;
		
		if (root instanceof TCApplyExpression)
		{
			TCApplyExpression aexp = (TCApplyExpression)root;
			return aexp.getRootDefinition(env, scope);
		}
		else if (root instanceof TCVariableExpression)
		{
			TCVariableExpression var = (TCVariableExpression)root;
			fname = var.name;
		}
		else if (root instanceof TCFuncInstantiationExpression)
		{
			TCFuncInstantiationExpression fie = (TCFuncInstantiationExpression)root;

			if (fie.expdef != null)
			{
				fname = fie.expdef.name;
			}
			else if (fie.impdef != null)
			{
				fname = fie.impdef.name;
			}
		}
			
		if (fname != null)
		{
			return env.findName(fname, scope);
		}
		else
		{
			return null;
		}
	}
	
	public String getMeasureApply(LexNameToken measure)
	{
		return getMeasureApply(measure, true);
	}
	
	/**
	 * Create a measure application string from this apply, turning the root function
	 * name into the measure name passed, and collapsing curried argument sets into one. 
	 */
	private String getMeasureApply(LexNameToken measure, boolean close)
	{
		String start = null;
		
		if (root instanceof TCApplyExpression)
		{
			TCApplyExpression aexp = (TCApplyExpression)root;
			start = aexp.getMeasureApply(measure, false);
		}
		else if (root instanceof TCVariableExpression)
		{
			start = measure.getName() + "(";
		}
		else if (root instanceof TCFuncInstantiationExpression)
		{
			TCFuncInstantiationExpression fie = (TCFuncInstantiationExpression)root;
			start = measure.getName() + "[" + Utils.listToString(fie.actualTypes) + "](";
		}
		else
		{
			start = root.toString() + "(";
		}
		
		return start  + Utils.listToString(args) + (close ? ")" : ", ");
	}

	@Override
	public <R, S> R apply(TCExpressionVisitor<R, S> visitor, S arg)
	{
		return visitor.caseApplyExpression(this, arg);
	}
}
