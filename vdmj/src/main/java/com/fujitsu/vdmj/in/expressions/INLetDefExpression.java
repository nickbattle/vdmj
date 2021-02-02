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

package com.fujitsu.vdmj.in.expressions;

import com.fujitsu.vdmj.in.definitions.INDefinition;
import com.fujitsu.vdmj.in.definitions.INDefinitionList;
import com.fujitsu.vdmj.in.definitions.INExplicitFunctionDefinition;
import com.fujitsu.vdmj.in.expressions.visitors.INExpressionVisitor;
import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.util.Utils;
import com.fujitsu.vdmj.values.FunctionValue;
import com.fujitsu.vdmj.values.NameValuePair;
import com.fujitsu.vdmj.values.NameValuePairList;
import com.fujitsu.vdmj.values.ObjectValue;
import com.fujitsu.vdmj.values.Value;

public class INLetDefExpression extends INExpression
{
	private static final long serialVersionUID = 1L;
	public final INDefinitionList localDefs;
	public final INExpression expression;

	public INLetDefExpression(LexLocation location, INDefinitionList localDefs, INExpression expression)
	{
		super(location);
		this.localDefs = localDefs;
		this.expression = expression;
	}

	@Override
	public String toString()
	{
		return "let " + Utils.listToString(localDefs) + " in " + expression;
	}

	@Override
	public Value eval(Context ctxt)
	{
		breakpoint.check(location, ctxt);

		Context evalContext = new Context(location, "let expression", ctxt);

		TCNameToken sname = new TCNameToken(location, location.module, "self");
		Value var = ctxt.check(sname);
		ObjectValue self = (var instanceof ObjectValue) ? (ObjectValue)var : null;

		for (INDefinition d: localDefs)
		{
			NameValuePairList values = d.getNamedValues(evalContext);

			if (d instanceof INExplicitFunctionDefinition)
			{
				for (NameValuePair nvp: values)
				{
					if (nvp.value instanceof FunctionValue)
					{
						FunctionValue fv = (FunctionValue)nvp.value;
						if (self != null) fv.setSelf(self);
						
						if (fv.name.equals(d.name.getName()))
						{
							fv.freeVariables = ctxt.getVisibleVariables();
							fv.freeVariables.put(d.name, fv);
						}
					}
				}
			}

			evalContext.putList(values);
		}

		return expression.eval(evalContext);
	}

	@Override
	public <R, S> R apply(INExpressionVisitor<R, S> visitor, S arg)
	{
		return visitor.caseLetDefExpression(this, arg);
	}
}
