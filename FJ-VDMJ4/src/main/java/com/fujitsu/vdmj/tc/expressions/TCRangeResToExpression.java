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
import com.fujitsu.vdmj.tc.types.TCMapType;
import com.fujitsu.vdmj.tc.types.TCSetType;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.TCTypeList;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.NameScope;
import com.fujitsu.vdmj.typechecker.TypeComparator;

public class TCRangeResToExpression extends TCBinaryExpression
{
	private static final long serialVersionUID = 1L;

	public TCRangeResToExpression(TCExpression left, LexToken op, TCExpression right)
	{
		super(left, op, right);
	}

	@Override
	public TCType typeCheck(Environment env, TCTypeList qualifiers, NameScope scope, TCType constraint)
	{
		TCType rngConstraint = null;
		
		if (constraint != null && constraint.isMap(location))
		{
			rngConstraint = new TCSetType(location, constraint.getMap().to);
		}

		ltype = left.typeCheck(env, null, scope, constraint);
		rtype = right.typeCheck(env, null, scope, rngConstraint);

		if (!ltype.isMap(location))
		{
			report(3151, "Left of ':>' is not a map");
		}
		else if (!rtype.isSet(location))
		{
			report(3152, "Right of ':>' is not a set");
		}
		else
		{
			TCMapType map = ltype.getMap();
			TCSetType set = rtype.getSet();

			if (!TypeComparator.compatible(set.setof, map.to))
			{
				report(3153, "Restriction of map should be set of " + map.to);
			}
		}

		return ltype;
	}
}
