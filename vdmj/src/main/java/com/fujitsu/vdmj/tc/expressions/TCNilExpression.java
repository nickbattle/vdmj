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
import com.fujitsu.vdmj.tc.types.TCOptionalType;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.TCTypeList;
import com.fujitsu.vdmj.tc.types.TCTypeSet;
import com.fujitsu.vdmj.tc.types.TCUnionType;
import com.fujitsu.vdmj.tc.types.TCUnknownType;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.NameScope;

public class TCNilExpression extends TCExpression
{
	private static final long serialVersionUID = 1L;

	public TCNilExpression(LexLocation location)
	{
		super(location);
	}

	@Override
	public String toString()
	{
		return "nil";
	}

	@Override
	public TCType typeCheck(Environment env, TCTypeList qualifiers, NameScope scope, TCType constraint)
	{
		TCType actual = null;
		
		if (constraint == null)
		{
			actual = new TCOptionalType(location, new TCUnknownType(location));
		}
		else if (constraint instanceof TCUnionType)
		{
			TCUnionType utype = (TCUnionType)constraint;
			TCTypeSet set = new TCTypeSet();

			for (TCType u: utype.types)
			{
				if (u instanceof TCOptionalType)
				{
					set.add(u);		// Pick out union of optional types
				}
			}

			if (set.isEmpty())
			{
				actual = new TCOptionalType(location, constraint);
			}
			else
			{
				actual = new TCOptionalType(location, set.getType(location));
			}
		}
		else
		{
			actual = new TCOptionalType(location, constraint);
		}

		return checkConstraint(constraint, actual);
	}

	@Override
	public <R, S> R apply(TCExpressionVisitor<R, S> visitor, S arg)
	{
		return visitor.caseNilExpression(this, arg);
	}
}
