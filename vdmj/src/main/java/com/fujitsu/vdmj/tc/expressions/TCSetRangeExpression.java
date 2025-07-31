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
import com.fujitsu.vdmj.tc.types.TCIntegerType;
import com.fujitsu.vdmj.tc.types.TCSetType;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.TCTypeList;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.NameScope;

public class TCSetRangeExpression extends TCSetExpression
{
	private static final long serialVersionUID = 1L;
	public final TCExpression first;
	public final TCExpression last;

	public TCType ftype = null;
	public TCType ltype = null;

	public TCSetRangeExpression(LexLocation start, TCExpression first, TCExpression last)
	{
		super(start);
		this.first = first;
		this.last = last;
	}

	@Override
	public String toString()
	{
		return "{" + first + ", ..., " + last + "}";
	}

	@Override
	public TCType typeCheck(Environment env, TCTypeList qualifiers, NameScope scope, TCType constraint)
	{
		ftype = first.typeCheck(env, null, scope, null);
		ltype = last.typeCheck(env, null, scope, null);

		if (!ftype.isNumeric(location))
		{
			first.report(3166, "Set range type must be a number");
			ftype = new TCIntegerType(location);	// Avoid later errors
		}
		
		if (ftype.getNumeric().getWeight() > 1)
		{
			ftype = new TCIntegerType(location);	// Caused by ceiling/floor
		}

		if (!ltype.isNumeric(location))
		{
			last.report(3167, "Set range type must be a number");
		}

		return possibleConstraint(constraint, new TCSetType(first.location, ftype));
	}

	@Override
	public <R, S> R apply(TCExpressionVisitor<R, S> visitor, S arg)
	{
		return visitor.caseSetRangeExpression(this, arg);
	}
}
