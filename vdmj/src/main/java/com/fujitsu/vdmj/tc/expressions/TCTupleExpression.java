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
import com.fujitsu.vdmj.tc.types.TCProductType;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.TCTypeList;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.NameScope;
import com.fujitsu.vdmj.util.Utils;

public class TCTupleExpression extends TCExpression
{
	private static final long serialVersionUID = 1L;
	public final TCExpressionList args;
	private TCTypeList types = null;

	public TCTupleExpression(LexLocation location, TCExpressionList args)
	{
		super(location);
		this.args = args;
	}

	@Override
	public String toString()
	{
		return "mk_(" + Utils.listToString(args) + ")";
	}

	@Override
	public TCType typeCheck(Environment env, TCTypeList qualifiers, NameScope scope, TCType constraint)
	{
		TCTypeList elemConstraints = null;
		
		if (constraint != null && constraint.isProduct(location))
		{
			elemConstraints = constraint.getProduct().types;
			
			if (elemConstraints.size() != args.size())
			{
				elemConstraints = null;
			}
		}
		
		int i = 0;
		types = new TCTypeList();
		
		for (TCExpression arg: args)
		{
			if (elemConstraints == null)
			{
				types.add(arg.typeCheck(env, null, scope, null));
			}
			else
			{
				types.add(arg.typeCheck(env, null, scope, elemConstraints.get(i++)));
			}
		}

		return possibleConstraint(constraint, new TCProductType(location, types));	// NB mk_() is a product
	}

	@Override
	public <R, S> R apply(TCExpressionVisitor<R, S> visitor, S arg)
	{
		return visitor.caseTupleExpression(this, arg);
	}
}
