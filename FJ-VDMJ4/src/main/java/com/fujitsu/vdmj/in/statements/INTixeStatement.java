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

import com.fujitsu.vdmj.in.expressions.INExpression;
import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.runtime.ExitException;
import com.fujitsu.vdmj.values.Value;

public class INTixeStatement extends INStatement
{
	private static final long serialVersionUID = 1L;
	public final INTixeStmtAlternativeList traps;
	public final INStatement body;

	public INTixeStatement(LexLocation location, INTixeStmtAlternativeList traps, INStatement body)
	{
		super(location);
		this.traps = traps;
		this.body = body;
	}

	@Override
	public String toString()
	{
		return "tixe {" + traps + "} in " + body;
	}

	@Override
	public INStatement findStatement(int lineno)
	{
		INStatement found = super.findStatement(lineno);
		if (found != null) return found;
		found = body.findStatement(lineno);
		if (found != null) return found;

		for (INTixeStmtAlternative tsa: traps)
		{
			found = tsa.statement.findStatement(lineno);
			if (found != null) break;
		}

		return found;
	}

	@Override
	public INExpression findExpression(int lineno)
	{
		INExpression found = body.findExpression(lineno);
		if (found != null) return found;

		for (INTixeStmtAlternative tsa: traps)
		{
			found = tsa.statement.findExpression(lineno);
			if (found != null) break;
		}

		return found;
	}

	@Override
	public Value eval(Context ctxt)
	{
		breakpoint.check(location, ctxt);
		Value rv = null;

		try
		{
			rv = body.eval(ctxt);
		}
		catch (ExitException original)
		{
			ExitException last = original;

			while (true)
			{
				Value exval = last.value;

				try
    			{
    				for (INTixeStmtAlternative tsa: traps)
    				{
    					rv = tsa.eval(location, exval, ctxt);

    					if (rv != null)  // TCStatement was executed
    					{
    						return rv;
    					}
    				}
    			}
    			catch (ExitException ex)
    			{
    				last = ex;
    				continue;
    			}

				throw last;
			}
		}

		return rv;
	}

	@Override
	public <R, S> R apply(INStatementVisitor<R, S> visitor, S arg)
	{
		return visitor.caseTixeStatement(this, arg);
	}
}
