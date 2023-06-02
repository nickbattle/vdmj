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
import com.fujitsu.vdmj.tc.expressions.visitors.TCExpressionVisitor;
import com.fujitsu.vdmj.tc.types.TCSetType;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.TCTypeList;
import com.fujitsu.vdmj.tc.types.TCTypeSet;
import com.fujitsu.vdmj.tc.types.TCUnionType;
import com.fujitsu.vdmj.tc.types.TCUnknownType;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.NameScope;

public class TCDistUnionExpression extends TCUnaryExpression
{
	private static final long serialVersionUID = 1L;

	public TCDistUnionExpression(LexLocation location, TCExpression exp)
	{
		super(location, exp);
	}

	@Override
	public String toString()
	{
		return "(dunion " + exp + ")";
	}

	@Override
	public TCType typeCheck(Environment env, TCTypeList qualifiers, NameScope scope, TCType constraint)
	{
		TCSetType setType = null;
		
		if (constraint != null)
		{
			setType = new TCSetType(location, constraint);
		}

		TCType type = exp.typeCheck(env, null, scope, setType);

		if (type.isSet(location))	// Should be set of union of member set types
		{
			TCSetType set = type.getSet();
			
			if (set.setof instanceof TCUnionType)
			{
				TCUnionType union = (TCUnionType)set.setof;
				TCTypeSet ts = new TCTypeSet();
				
				for (TCType member: union.types)
				{
					if (member instanceof TCSetType)
					{
						TCSetType sm = (TCSetType)member;
						ts.add(sm.setof);
					}
				}
				
				return setType(new TCSetType(location, ts.getType(location)));
			}
			else if (set.setof.isSet(location))
			{
				return setType(set.setof);
			}
		}

		report(3078, "dunion argument is not a set of sets");
		return setType(new TCSetType(location, new TCUnknownType(location)));
	}

	@Override
	public <R, S> R apply(TCExpressionVisitor<R, S> visitor, S arg)
	{
		return visitor.caseDistUnionExpression(this, arg);
	}
}
