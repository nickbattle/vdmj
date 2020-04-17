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

package com.fujitsu.vdmj.in.statements;

import com.fujitsu.vdmj.in.definitions.INMultiBindListDefinition;
import com.fujitsu.vdmj.in.expressions.INExpression;
import com.fujitsu.vdmj.in.patterns.INMultipleBind;
import com.fujitsu.vdmj.in.patterns.INPattern;
import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.runtime.ValueException;
import com.fujitsu.vdmj.values.NameValuePair;
import com.fujitsu.vdmj.values.NameValuePairList;
import com.fujitsu.vdmj.values.Quantifier;
import com.fujitsu.vdmj.values.QuantifierList;
import com.fujitsu.vdmj.values.Value;
import com.fujitsu.vdmj.values.ValueList;

public class INLetBeStStatement extends INStatement
{
	private static final long serialVersionUID = 1L;
	public final INMultipleBind bind;
	public final INExpression suchThat;
	public final INStatement statement;
	public final INMultiBindListDefinition def;

	public INLetBeStStatement(LexLocation location,
		INMultipleBind bind, INExpression suchThat, INStatement statement, INMultiBindListDefinition def)
	{
		super(location);
		this.bind = bind;
		this.suchThat = suchThat;
		this.statement = statement;
		this.def = def;
	}

	@Override
	public String toString()
	{
		return "let " + bind +
			(suchThat == null ? "" : " be st " + suchThat) + " in " + statement;
	}

	@Override
	public Value eval(Context ctxt)
	{
		breakpoint.check(location, ctxt);
		try
		{
			QuantifierList quantifiers = new QuantifierList();

			for (INMultipleBind mb: def.bindings)
			{
				ValueList bvals = mb.getBindValues(ctxt, false);

				for (INPattern p: mb.plist)
				{
					Quantifier q = new Quantifier(p, bvals);
					quantifiers.add(q);
				}
			}

			quantifiers.init(ctxt, true);

			while (quantifiers.hasNext())
			{
				Context evalContext = new Context(location, "let be st statement", ctxt);
				NameValuePairList nvpl = quantifiers.next();
				boolean matches = true;

				for (NameValuePair nvp: nvpl)
				{
					Value v = evalContext.get(nvp.name);

					if (v == null)
					{
						evalContext.put(nvp.name, nvp.value);
					}
					else
					{
						if (!v.equals(nvp.value))
						{
							matches = false;
							break;	// This quantifier set does not match
						}
					}
				}

				if (matches &&
					(suchThat == null || suchThat.eval(evalContext).boolValue(ctxt)))
				{
					return statement.eval(evalContext);
				}
			}
		}
        catch (ValueException e)
        {
        	abort(e);
        }

		return abort(4040, "Let be st found no applicable bindings", ctxt);
	}

	@Override
	public <R, S> R apply(INStatementVisitor<R, S> visitor, S arg)
	{
		return visitor.caseLetBeStStatement(this, arg);
	}
}
