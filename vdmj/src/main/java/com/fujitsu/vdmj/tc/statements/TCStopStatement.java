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

package com.fujitsu.vdmj.tc.statements;

import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.tc.expressions.TCExpression;
import com.fujitsu.vdmj.tc.statements.visitors.TCStatementVisitor;
import com.fujitsu.vdmj.tc.types.TCClassType;
import com.fujitsu.vdmj.tc.types.TCSetType;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.TCVoidType;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.NameScope;

public class TCStopStatement extends TCStatement
{
	private static final long serialVersionUID = 1L;
	public final TCExpression objects;

	public TCStopStatement(LexLocation location, TCExpression obj)
	{
		super(location);
		this.objects = obj;
	}

	@Override
	public TCType typeCheck(Environment env, NameScope scope, TCType constraint, boolean mandatory)
	{
		TCType type = objects.typeCheck(env, null, scope, null);

		if (type.isSet(location))
		{
			TCSetType set = type.getSet();

			if (!set.setof.isClass(null))
			{
				objects.report(3235, "Expression is not a set of object references");
			}
			else
			{
				TCClassType ctype = set.setof.getClassType(null);

				if (ctype.classdef.findThread() == null)
				{
					objects.report(3236, "Class does not define a thread");
				}
			}
		}
		else if (type.isClass(null))
		{
			TCClassType ctype = type.getClassType(null);

			if (ctype.classdef.findThread() == null)
			{
				objects.report(3237, "Class does not define a thread");
			}
		}
		else
		{
			objects.report(3238, "Expression is not an object reference or set of object references");
		}

		return checkReturnType(constraint, new TCVoidType(location), mandatory);
	}

	@Override
	public String toString()
	{
		return "stop(" + objects + ")";
	}

	@Override
	public <R, S> R apply(TCStatementVisitor<R, S> visitor, S arg)
	{
		return visitor.caseStopStatement(this, arg);
	}
}
