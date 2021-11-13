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
import com.fujitsu.vdmj.tc.types.TCUnknownType;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.NameScope;

public class TCPowerSetExpression extends TCUnaryExpression
{
	private static final long serialVersionUID = 1L;

	public TCPowerSetExpression(LexLocation location, TCExpression exp)
	{
		super(location, exp);
	}

	@Override
	public String toString()
	{
		return "(power " + exp + ")";
	}

	@Override
	public TCType typeCheck(Environment env, TCTypeList qualifiers, NameScope scope, TCType constraint)
	{
		TCType elemConstraint = null;
		
		if (constraint != null && constraint.isSet(location))
		{
			elemConstraint = constraint.getSet().setof;
		}

		TCType etype = exp.typeCheck(env, null, scope, elemConstraint);

		if (!etype.isSet(location))
		{
			report(3145, "Argument to 'power' is not a set");
			return setType(new TCUnknownType(location));
		}
		
		TCSetType eset = etype.getSet();

		return setType(new TCSetType(location, new TCSetType(location, eset.setof)));
	}

	@Override
	public <R, S> R apply(TCExpressionVisitor<R, S> visitor, S arg)
	{
		return visitor.casePowerSetExpression(this, arg);
	}
}
