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
import com.fujitsu.vdmj.tc.expressions.visitors.TCExpressionVisitor;
import com.fujitsu.vdmj.tc.types.TCSet1Type;
import com.fujitsu.vdmj.tc.types.TCSetType;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.TCTypeList;
import com.fujitsu.vdmj.tc.types.TCTypeSet;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.NameScope;
import com.fujitsu.vdmj.util.Utils;

public class TCSetEnumExpression extends TCSetExpression
{
	private static final long serialVersionUID = 1L;
	public final TCExpressionList members;
	public TCTypeList types = null;

	public TCSetEnumExpression(LexLocation location, TCExpressionList members)
	{
		super(location);
		this.members = members;
	}

	@Override
	public String toString()
	{
		return Utils.listToString("{", members, ", ", "}");
	}

	@Override
	public TCType typeCheck(Environment env, TCTypeList qualifiers, NameScope scope, TCType constraint)
	{
		TCTypeSet ts = new TCTypeSet();
		types = new TCTypeList();
		
		TCType elemConstraint = null;
		
		if (constraint != null && constraint.isSet(location))
		{
			elemConstraint = constraint.getSet().setof;
		}

		for (TCExpression ex: members)
		{
			TCType mt = ex.typeCheck(env, null, scope, elemConstraint);
			ts.add(mt);
			types.add(mt);

			if (members.size() > 1 && TCType.isFunctionType(mt, location))
			{
				ex.warning(5037, "Function equality cannot be reliably computed");
			}
		}

		TCType rt = ts.isEmpty() ? new TCSetType(location) :
					new TCSet1Type(location, ts.getType(location));
		
		return possibleConstraint(constraint, rt);
	}

	@Override
	public <R, S> R apply(TCExpressionVisitor<R, S> visitor, S arg)
	{
		return visitor.caseSetEnumExpression(this, arg);
	}
}
