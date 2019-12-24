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
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.tc.types.TCBooleanType;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.TCTypeList;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.NameScope;

public class TCIsOfBaseClassExpression extends TCExpression
{
	private static final long serialVersionUID = 1L;
	public final TCNameToken baseclass;
	public final TCExpression exp;

	public TCIsOfBaseClassExpression(LexLocation start, TCNameToken classname, TCExpression exp)
	{
		super(start);
		this.baseclass = classname.getExplicit(false);
		this.exp = exp;
	}

	@Override
	public String toString()
	{
		return "isofbaseclass(" + baseclass + "," + exp + ")";
	}

	@Override
	public TCType typeCheck(Environment env, TCTypeList qualifiers, NameScope scope, TCType constraint)
	{
		if (env.findType(baseclass, null) == null)
		{
			report(3114, "Undefined base class type: " + baseclass.getName());
		}

		TCType rt = exp.typeCheck(env, null, scope, null);

		if (!rt.isClass(env))
		{
			exp.report(3266, "Argument is not an object");
		}

		return checkConstraint(constraint, new TCBooleanType(location));
	}

	@Override
	public <R, S> R apply(TCExpressionVisitor<R, S> visitor, S arg)
	{
		return visitor.caseIsOfBaseClassExpression(this, arg);
	}
}
