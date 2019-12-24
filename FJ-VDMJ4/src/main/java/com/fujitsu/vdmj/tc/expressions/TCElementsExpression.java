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
 *
 ******************************************************************************/

package com.fujitsu.vdmj.tc.expressions;

import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.tc.lex.TCNameSet;
import com.fujitsu.vdmj.tc.types.TCSeqType;
import com.fujitsu.vdmj.tc.types.TCSetType;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.TCTypeList;
import com.fujitsu.vdmj.tc.types.TCUnknownType;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.NameScope;

public class TCElementsExpression extends TCSetExpression
{
	private static final long serialVersionUID = 1L;
	public final TCExpression exp;

	public TCElementsExpression(LexLocation location, TCExpression exp)
	{
		super(location);
		this.exp = exp;
	}

	@Override
	public String toString()
	{
		return "(elems " + exp + ")";
	}

	@Override
	public TCType typeCheck(Environment env, TCTypeList qualifiers, NameScope scope, TCType constraint)
	{
		TCType elemType = null;
		
		if (constraint != null && constraint.isSet(location))
		{
			elemType = new TCSeqType(location, constraint.getSet().setof);
		}
		
		TCType arg = exp.typeCheck(env, null, scope, elemType);

		if (!arg.isSeq(location))
		{
			report(3085, "Argument of 'elems' is not a sequence");
			return new TCSetType(location, new TCUnknownType(location));
		}

		TCSeqType seq = arg.getSeq();
		return seq.empty ? new TCSetType(location) : new TCSetType(location, seq.seqof);
	}

	@Override
	public TCNameSet getFreeVariables(Environment globals, Environment env)
	{
		return exp.getFreeVariables(globals, env);
	}

	@Override
	public <R, S> R apply(TCExpressionVisitor<R, S> visitor, S arg)
	{
		return visitor.caseElementsExpression(this, arg);
	}
}
