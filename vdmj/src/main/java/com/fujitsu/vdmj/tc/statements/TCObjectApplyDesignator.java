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

package com.fujitsu.vdmj.tc.statements;

import com.fujitsu.vdmj.tc.expressions.TCExpression;
import com.fujitsu.vdmj.tc.expressions.TCExpressionList;
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

public class TCObjectApplyDesignator extends TCObjectDesignator
{
	private static final long serialVersionUID = 1L;
	public final TCObjectDesignator object;
	public final TCExpressionList args;

	public TCObjectApplyDesignator(TCObjectDesignator object, TCExpressionList args)
	{
		super(object.location);
		this.object = object;
		this.args = args;
	}

	@Override
	public String toString()
	{
		return "(" + object + "(" + Utils.listToString(args) + "))";
	}

	@Override
	public TCType typeCheck(Environment env, TCTypeList qualifiers)
	{
		TCTypeList argtypes = new TCTypeList();

		for (TCExpression a: args)
		{
			argtypes.add(a.typeCheck(env, null, NameScope.NAMESANDSTATE, null));
		}

		TCType type = object.typeCheck(env, argtypes);
		boolean unique = !type.isUnion(location);
		TCTypeSet result = new TCTypeSet();

		if (type.isMap(location))
		{
			TCMapType map = type.getMap();
			result.add(mapApply(map, env, NameScope.NAMESANDSTATE, unique));
		}

		if (type.isSeq(location))
		{
			TCSeqType seq = type.getSeq();
			result.add(seqApply(seq, env, NameScope.NAMESANDSTATE, unique));
		}

		if (type.isFunction(location))
		{
			TCFunctionType ft = type.getFunction();
			ft.typeResolve(env, null);
			result.add(functionApply(ft, env, NameScope.NAMESANDSTATE, unique));
		}

		if (type.isOperation(location))
		{
			TCOperationType ot = type.getOperation();
			ot.typeResolve(env, null);
			result.add(operationApply(ot, env, NameScope.NAMESANDSTATE, unique));
		}

		if (result.isEmpty())
		{
			report(3249, "Object designator is not a map, sequence, function or operation");
			detail2("Designator", object, "Type", type);
			return new TCUnknownType(location);
		}

		return result.getType(location);
	}

	private TCType mapApply(
		TCMapType map, Environment env, NameScope scope, boolean unique)
	{
		if (args.size() != 1)
		{
			concern(unique, 3250, "Map application must have one argument");
			return new TCUnknownType(location);
		}

		TCType argtype = args.get(0).typeCheck(env, null, scope, null);

		if (!TypeComparator.compatible(map.from, argtype))
		{
			concern(unique, 3251, "Map application argument is incompatible type");
			detail2(unique, "Map domain", map.from, "Argument", argtype);
		}

		return map.to;
	}

	private TCType seqApply(
		TCSeqType seq, Environment env, NameScope scope, boolean unique)
	{
		if (args.size() != 1)
		{
			concern(unique, 3252, "Sequence application must have one argument");
			return new TCUnknownType(location);
		}

		TCType argtype = args.get(0).typeCheck(env, null, scope, null);

		if (!argtype.isNumeric(location))
		{
			concern(unique, 3253, "Sequence argument is not numeric");
			detail(unique, "Type", argtype);
		}

		return seq.seqof;
	}

	private TCType functionApply(
		TCFunctionType ftype, Environment env, NameScope scope, boolean unique)
	{
		TCTypeList ptypes = ftype.parameters;

		if (args.size() > ptypes.size())
		{
			concern(unique, 3254, "Too many arguments");
			detail2(unique, "Args", args, "Params", ptypes);
			return ftype.result;
		}
		else if (args.size() < ptypes.size())
		{
			concern(unique, 3255, "Too few arguments");
			detail2(unique, "Args", args, "Params", ptypes);
			return ftype.result;
		}

		int i=0;

		for (TCExpression a: args)
		{
			TCType at = a.typeCheck(env, null, scope, null);
			TCType pt = ptypes.get(i++);

			if (!TypeComparator.compatible(pt, at))
			{
				concern(unique, 3256, "Inappropriate type for argument " + i);
				detail2(unique, "Expect", pt, "Actual", at);
			}
		}

		return ftype.result;
	}

	private TCType operationApply(
		TCOperationType optype, Environment env, NameScope scope, boolean unique)
	{
		TCTypeList ptypes = optype.parameters;

		if (args.size() > ptypes.size())
		{
			concern(unique, 3257, "Too many arguments");
			detail2(unique, "Args", args, "Params", ptypes);
			return optype.result;
		}
		else if (args.size() < ptypes.size())
		{
			concern(unique, 3258, "Too few arguments");
			detail2(unique, "Args", args, "Params", ptypes);
			return optype.result;
		}

		int i=0;

		for (TCExpression a: args)
		{
			TCType at = a.typeCheck(env, null, scope, null);
			TCType pt = ptypes.get(i++);

			if (!TypeComparator.compatible(pt, at))
			{
				concern(unique, 3259, "Inappropriate type for argument " + i);
				detail2(unique, "Expect", pt, "Actual", at);
			}
		}

		return optype.result;
	}
}
