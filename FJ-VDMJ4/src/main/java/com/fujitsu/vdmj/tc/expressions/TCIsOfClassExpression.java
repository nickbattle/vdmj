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
import com.fujitsu.vdmj.tc.definitions.TCClassDefinition;
import com.fujitsu.vdmj.tc.definitions.TCDefinition;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.tc.types.TCBooleanType;
import com.fujitsu.vdmj.tc.types.TCClassType;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.TCTypeList;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.NameScope;

public class TCIsOfClassExpression extends TCExpression
{
	private static final long serialVersionUID = 1L;
	public final TCNameToken classname;
	public final TCExpression exp;
	public TCClassType classType;

	public TCIsOfClassExpression(LexLocation start, TCNameToken classname, TCExpression exp)
	{
		super(start);
		this.classname = classname.getExplicit(false);
		this.exp = exp;
	}

	@Override
	public String toString()
	{
		return "isofclass(" + classname + "," + exp + ")";
	}

	@Override
	public TCType typeCheck(Environment env, TCTypeList qualifiers, NameScope scope, TCType constraint)
	{
		TCDefinition cls = env.findType(classname, null);

		if (cls == null || !(cls instanceof TCClassDefinition))
		{
			report(3115, "Undefined class type: " + classname.getName());
		}
		else
		{
			classType = (TCClassType)cls.getType();
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
		return visitor.caseIsOfClassExpression(this, arg);
	}
}
