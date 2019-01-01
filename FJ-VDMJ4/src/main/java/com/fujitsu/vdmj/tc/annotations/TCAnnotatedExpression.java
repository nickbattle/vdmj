/*******************************************************************************
 *
 *	Copyright (c) 2018 Nick Battle.
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

package com.fujitsu.vdmj.tc.annotations;

import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.tc.definitions.TCDefinitionList;
import com.fujitsu.vdmj.tc.expressions.TCExpression;
import com.fujitsu.vdmj.tc.lex.TCNameSet;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.TCTypeList;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.NameScope;

public class TCAnnotatedExpression extends TCExpression
{
	private static final long serialVersionUID = 1L;
	
	public final TCAnnotation annotation;

	public final TCExpression expression;
	
	public TCAnnotatedExpression(LexLocation location, TCAnnotation annotation, TCExpression expression)
	{
		super(location);
		this.annotation = annotation;
		this.expression = expression;
	}

	@Override
	public String toString()
	{
		return annotation + " " + expression;
	}

	@Override
	public TCType typeCheck(Environment env, TCTypeList qualifiers, NameScope scope, TCType constraint)
	{
		annotation.before(this, env, scope);
		TCType type = expression.typeCheck(env, qualifiers, scope, constraint);
		annotation.after(this, type, env, scope);
		return type;
	}

	@Override
	public TCDefinitionList getQualifiedDefs(Environment env)
	{
		return expression.getQualifiedDefs(env);
	}
	
	@Override
	public TCNameSet getFreeVariables(Environment globals, Environment env)
	{
		return expression.getFreeVariables(globals, env);
	}
}
