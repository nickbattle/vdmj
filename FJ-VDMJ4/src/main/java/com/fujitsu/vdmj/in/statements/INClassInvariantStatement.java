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

import com.fujitsu.vdmj.in.definitions.INClassInvariantDefinition;
import com.fujitsu.vdmj.in.definitions.INDefinition;
import com.fujitsu.vdmj.in.definitions.INDefinitionList;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.runtime.ValueException;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.values.BooleanValue;
import com.fujitsu.vdmj.values.Value;

public class INClassInvariantStatement extends INStatement
{
	private static final long serialVersionUID = 1L;
	public final TCNameToken name;
	public final INDefinitionList invdefs;

	public INClassInvariantStatement(TCNameToken name, INDefinitionList invdefs)
	{
		super(name.getLocation());
		this.name = name;
		this.invdefs = invdefs;
		name.getLocation().executable(false);
	}

	@Override
	public Value eval(Context ctxt)
	{
		for (INDefinition d: invdefs)
		{
			INClassInvariantDefinition invdef = (INClassInvariantDefinition)d;

			try
			{
				if (!invdef.expression.eval(ctxt).boolValue(ctxt))
				{
					return new BooleanValue(false);
				}
			}
			catch (ValueException e)
			{
				abort(e);
			}
		}

		return new BooleanValue(true);
	}

	@Override
	public String toString()
	{
		return "instance invariant " + name;
	}
}
