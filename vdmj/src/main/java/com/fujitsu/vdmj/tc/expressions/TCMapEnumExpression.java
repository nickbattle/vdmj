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

import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.tc.expressions.visitors.TCExpressionVisitor;
import com.fujitsu.vdmj.tc.types.TCMapType;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.TCTypeList;
import com.fujitsu.vdmj.tc.types.TCTypeSet;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.NameScope;
import com.fujitsu.vdmj.util.Utils;

public class TCMapEnumExpression extends TCMapExpression
{
	private static final long serialVersionUID = 1L;
	public final TCMapletExpressionList members;
	public TCTypeList domtypes = null;
	public TCTypeList rngtypes = null;

	public TCMapEnumExpression(LexLocation location, TCMapletExpressionList members)
	{
		super(location);
		this.members = members;
	}

	@Override
	public String toString()
	{
		if (members.isEmpty())
		{
			return "{|->}";
		}
		else
		{
			return "{" + Utils.listToString(members) + "}";
		}
	}

	@Override
	public TCType typeCheck(Environment env, TCTypeList qualifiers, NameScope scope, TCType constraint)
	{
		domtypes = new TCTypeList();
		rngtypes = new TCTypeList();

		if (members.isEmpty())
		{
			return setType(new TCMapType(location));
		}

		TCTypeSet dom = new TCTypeSet();
		TCTypeSet rng = new TCTypeSet();

		TCType domConstraint = null;
		TCType rngConstraint = null;
		
		if (constraint != null && constraint.isMap(location))
		{
			domConstraint = constraint.getMap().from;
			rngConstraint = constraint.getMap().to;
		}

		for (TCMapletExpression ex: members)
		{
			TCType mt = ex.typeCheck(env, scope, domConstraint, rngConstraint);

			if (!mt.isMap(location))
			{
				report(3121, "Element is not of maplet type");
			}
			else
			{
				TCMapType maplet = mt.getMap();
				dom.add(maplet.from);
				domtypes.add(maplet.from);
				rng.add(maplet.to);
				rngtypes.add(maplet.to);
			}
		}

		TCType rt = new TCMapType(location, dom.getType(location), rng.getType(location));
		return possibleConstraint(constraint, rt);
	}

	@Override
	public <R, S> R apply(TCExpressionVisitor<R, S> visitor, S arg)
	{
		return visitor.caseMapEnumExpression(this, arg);
	}
}
