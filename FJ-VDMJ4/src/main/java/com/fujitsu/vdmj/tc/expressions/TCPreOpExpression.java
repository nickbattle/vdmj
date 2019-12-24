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

import com.fujitsu.vdmj.tc.definitions.TCDefinitionList;
import com.fujitsu.vdmj.tc.definitions.TCStateDefinition;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.tc.statements.TCErrorCaseList;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.TCTypeList;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.NameScope;

public class TCPreOpExpression extends TCExpression
{
	private static final long serialVersionUID = 1L;
	public final TCNameToken opname;
	public final TCExpression expression;
	public final TCErrorCaseList errors;
	public final TCStateDefinition state;

	public TCPreOpExpression(
		TCNameToken opname, TCExpression expression, TCErrorCaseList errors, TCStateDefinition state)
	{
		super(expression);
		this.opname = opname;
		this.expression = expression;
		this.errors = errors;
		this.state = state;
	}

	@Override
	public String toString()
	{
		return expression.toString();
	}

	@Override
	public TCType typeCheck(Environment env, TCTypeList qualifiers, NameScope scope, TCType constraint)
	{
		return expression.typeCheck(env, null, scope, null);
	}

	@Override
	public TCDefinitionList getQualifiedDefs(Environment env)
	{
		return expression.getQualifiedDefs(env);
	}

	@Override
	public <R, S> R apply(TCExpressionVisitor<R, S> visitor, S arg)
	{
		return visitor.casePreOpExpression(this, arg);
	}
}
