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
import com.fujitsu.vdmj.tc.definitions.TCDefinitionList;
import com.fujitsu.vdmj.tc.types.TCBooleanType;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.TCTypeList;
import com.fujitsu.vdmj.tc.types.TCTypeSet;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.FlatEnvironment;
import com.fujitsu.vdmj.typechecker.NameScope;

public class TCIfExpression extends TCExpression
{
	private static final long serialVersionUID = 1L;
	public final TCExpression ifExp;
	public final TCExpression thenExp;
	public final TCElseIfExpressionList elseList;
	public final TCExpression elseExp;

	public TCIfExpression(LexLocation location,
		TCExpression ifExp, TCExpression thenExp, TCElseIfExpressionList elseList,
		TCExpression elseExp)
	{
		super(location);
		this.ifExp = ifExp;
		this.thenExp = thenExp;
		this.elseList = elseList;
		this.elseExp = elseExp;
	}

	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		sb.append("(if " + ifExp + "\nthen " + thenExp);

		for (TCElseIfExpression s: elseList)
		{
			sb.append("\n");
			sb.append(s.toString());
		}

		if (elseExp != null)
		{
			sb.append("\nelse ");
			sb.append(elseExp.toString());
		}

		sb.append(")");

		return sb.toString();
	}

	@Override
	public TCType typeCheck(Environment env, TCTypeList qualifiers, NameScope scope, TCType constraint)
	{
		if (!ifExp.typeCheck(env, null, scope, null).isType(TCBooleanType.class, location))
		{
			report(3108, "If expression is not a boolean");
		}
		
		TCDefinitionList qualified = ifExp.getQualifiedDefs(env);
		Environment qenv = env;
		
		if (!qualified.isEmpty())
		{
			qenv = new FlatEnvironment(qualified, env);
		}

		TCTypeSet rtypes = new TCTypeSet();
		rtypes.add(thenExp.typeCheck(qenv, null, scope, constraint));

		for (TCElseIfExpression eie: elseList)
		{
			rtypes.add(eie.typeCheck(env, null, scope, constraint));
		}

		rtypes.add(elseExp.typeCheck(env, null, scope, constraint));

		return rtypes.getType(location);
	}
}
