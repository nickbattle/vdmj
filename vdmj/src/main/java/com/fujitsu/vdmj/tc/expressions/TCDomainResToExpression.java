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

import com.fujitsu.vdmj.ast.lex.LexToken;
import com.fujitsu.vdmj.tc.expressions.visitors.TCExpressionVisitor;
import com.fujitsu.vdmj.tc.types.TCMapType;
import com.fujitsu.vdmj.tc.types.TCSetType;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.TCTypeList;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.NameScope;
import com.fujitsu.vdmj.typechecker.TypeComparator;

public class TCDomainResToExpression extends TCBinaryExpression
{
	private static final long serialVersionUID = 1L;

	public TCDomainResToExpression(TCExpression left, LexToken op, TCExpression right)
	{
		super(left, op, right);
	}

	@Override
	public TCType typeCheck(Environment env, TCTypeList qualifiers, NameScope scope, TCType constraint)
	{
		TCType domConstraint = null;
		
		if (constraint != null && constraint.isMap(location))
		{
			domConstraint = new TCSetType(location, constraint.getMap().from);
		}

		ltype = left.typeCheck(env, null, scope, domConstraint);
		rtype = right.typeCheck(env, null, scope, constraint);

		if (!ltype.isSet(location))
		{
			report(3082, "Left of '<:' is not a set");
			detail("Actual", ltype);
		}
		else if (!rtype.isMap(location))
		{
			report(3083, "Right of '<:' is not a map");
			detail("Actual", rtype);
		}
		else
		{
			TCSetType set = ltype.getSet();
			TCMapType map = rtype.getMap();

			if (!TypeComparator.compatible(set.setof, map.from))
			{
				report(3084, "Restriction of map should be set of " + map.from);
			}
		}

		return setType(rtype);
	}

	@Override
	public <R, S> R apply(TCExpressionVisitor<R, S> visitor, S arg)
	{
		return visitor.caseDomainResToExpression(this, arg);
	}
}
