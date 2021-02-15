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

import com.fujitsu.vdmj.ast.lex.LexIntegerToken;
import com.fujitsu.vdmj.tc.expressions.visitors.TCExpressionVisitor;
import com.fujitsu.vdmj.tc.types.TCProductType;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.TCTypeList;
import com.fujitsu.vdmj.tc.types.TCUnknownType;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.NameScope;

public class TCFieldNumberExpression extends TCExpression
{
	private static final long serialVersionUID = 1L;
	public final TCExpression tuple;
	public final LexIntegerToken field;
	private TCType type = null;

	public TCFieldNumberExpression(TCExpression tuple, LexIntegerToken field)
	{
		super(tuple);
		this.tuple = tuple;
		this.field = field;
	}

	@Override
	public String toString()
	{
		return "(" + tuple + ".#" + field + ")";
	}

	@Override
	public TCType typeCheck(Environment env, TCTypeList qualifiers, NameScope scope, TCType constraint)
	{
		type = tuple.typeCheck(env, null, scope, null);

		if (!type.isProduct(location))
		{
			tuple.report(3094, "Field '#" + field + "' applied to non-tuple type");
			return new TCUnknownType(location);
		}

		TCProductType product = type.getProduct();
		long fn = field.value.longValue();

		if (fn > product.types.size() || fn < 1)
		{
			tuple.report(3095, "Field number does not match tuple size");
			return new TCUnknownType(location);
		}

		return product.types.get((int)fn - 1);
	}

	@Override
	public <R, S> R apply(TCExpressionVisitor<R, S> visitor, S arg)
	{
		return visitor.caseFieldNumberExpression(this, arg);
	}
}
