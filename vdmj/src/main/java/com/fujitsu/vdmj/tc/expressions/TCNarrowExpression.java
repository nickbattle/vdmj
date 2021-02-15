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

package com.fujitsu.vdmj.tc.expressions;

import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.tc.definitions.TCDefinition;
import com.fujitsu.vdmj.tc.expressions.visitors.TCExpressionVisitor;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.TCTypeList;
import com.fujitsu.vdmj.tc.types.TCUnknownType;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.NameScope;
import com.fujitsu.vdmj.typechecker.TypeComparator;

public class TCNarrowExpression extends TCExpression
{
	private static final long serialVersionUID = 1L;
	public TCType basictype;
	public final TCNameToken typename;
	public final TCExpression test;

	private TCDefinition typedef = null;
	private TCType exptype = null;
	public TCTypeList unresolved = null;

	public TCNarrowExpression(LexLocation location, TCNameToken typename, TCType type, TCExpression test)
	{
		super(location);
		this.basictype = type;
		this.typename = typename;
		this.test = test;
		
		if (basictype != null)
		{
			unresolved = basictype.unresolvedTypes();
		}
	}

	@Override
	public String toString()
	{
		return "narrow_(" + test + ", " + (typename == null ? basictype : typename) + ")";
	}

	@Override
	public TCType typeCheck(Environment env, TCTypeList qualifiers, NameScope scope, TCType constraint)
	{
		exptype = test.typeCheck(env, null, scope, null);
		TCType result = null;

		if (basictype != null)
		{
			basictype = basictype.typeResolve(env, null);
			result = basictype;
			TypeComparator.checkImports(env, unresolved, location.module);
			TypeComparator.checkComposeTypes(basictype, env, false);
		}
		else
		{
			typedef = env.findType(typename, location.module);

			if (typedef == null)
			{
				report(3113, "Unknown type name '" + typename + "'");
				result = new TCUnknownType(location);
			}
			else
			{
				result = typedef.getType();
			}
		}
		
		if (!TypeComparator.compatible(result, exptype))
		{
			report(3317, "Expression can never match narrow type");
		}
		
		return possibleConstraint(constraint, result);
	}

	@Override
	public <R, S> R apply(TCExpressionVisitor<R, S> visitor, S arg)
	{
		return visitor.caseNarrowExpression(this, arg);
	}
}
