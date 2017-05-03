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

import com.fujitsu.vdmj.ast.lex.LexToken;
import com.fujitsu.vdmj.tc.types.TCInvariantType;
import com.fujitsu.vdmj.tc.types.TCNamedType;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.TCTypeSet;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.NameScope;

abstract public class TCBinaryExpression extends TCExpression
{
	private static final long serialVersionUID = 1L;

	public final TCExpression left;
	public final TCExpression right;
	public final LexToken op;

	public TCType ltype = null;
	public TCType rtype = null;

	public TCBinaryExpression(TCExpression left, LexToken op, TCExpression right)
	{
		super(op.location);
		this.left = left;
		this.right = right;
		this.op = op;
	}

	protected final TCType binaryCheck(Environment env, NameScope scope, TCType expected)
	{
		ltype = left.typeCheck(env, null, scope, null);
		rtype = right.typeCheck(env, null, scope, null);

		if (!ltype.isType(expected.getClass(), location))
		{
			report(3065, "Left hand of " + op + " is not " + expected);
		}

		if (!rtype.isType(expected.getClass(), location))
		{
			report(3066, "Right hand of " + op + " is not " + expected);
		}

		return expected;
	}

	@Override
	public String toString()
	{
		return "(" + left + " " + op + " " + right + ")";
	}
	
	protected void checkMultipleEqualities(TCType union)
	{
		if (union instanceof TCNamedType)
		{
			TCNamedType nt = (TCNamedType)union;
			
			if (nt.eqdef != null)
			{
				return;		// eqdef will override others anyway
			}
		}
		
		TCTypeSet members = union.getUnion().types;
		TCTypeSet equality = new TCTypeSet();
		
		for (TCType m: members)
		{
			if (m instanceof TCInvariantType)
			{
				TCInvariantType it = (TCInvariantType)m;
				if (it.eqdef != null) equality.add(m);
			}
		}
		
		if (equality.size() > 1)
		{
			warning(5022, "Multiple union members define eq clauses, " + equality);
		}
	}
}
