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

package com.fujitsu.vdmj.tc.patterns;

import com.fujitsu.vdmj.tc.expressions.TCExpression;
import com.fujitsu.vdmj.tc.patterns.visitors.TCMultipleBindVisitor;
import com.fujitsu.vdmj.tc.types.TCSetType;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.TCUnknownType;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.NameScope;
import com.fujitsu.vdmj.typechecker.TypeComparator;

public class TCMultipleSetBind extends TCMultipleBind
{
	private static final long serialVersionUID = 1L;
	public final TCExpression set;

	public TCMultipleSetBind(TCPatternList plist, TCExpression set)
	{
		super(plist);
		this.set = set;
	}

	@Override
	public String toString()
	{
		return plist + " in set " + set;
	}

	@Override
	public TCType typeCheck(Environment base, NameScope scope)
	{
		plist.typeResolve(base);
		TCType type = set.typeCheck(base, null, scope, null);
		TCType result = new TCUnknownType(location);

		if (!type.isSet(location))
		{
			set.report(3197, "Expression matching set bind is not a set");
			set.detail("Actual type", type);
		}
		else
		{
			TCSetType st = type.getSet();

			if (!st.empty)
			{
				result = st.setof;
				TCType ptype = getPossibleType();

				if (!TypeComparator.compatible(ptype, result))
				{
					set.report(3264, "At least one bind cannot match set");
					set.detail2("Binds", ptype, "Set of", st);
				}
			}
			else
			{
				set.warning(5009, "Empty set used in bind");
			}
		}

		return result;
	}

	@Override
	public <R, S> R apply(TCMultipleBindVisitor<R, S> visitor, S arg)
	{
		return visitor.caseMultipleSetBind(this, arg);
	}
}
