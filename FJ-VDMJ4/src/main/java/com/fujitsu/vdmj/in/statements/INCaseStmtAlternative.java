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

import java.io.Serializable;

import com.fujitsu.vdmj.in.patterns.INPattern;
import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.runtime.PatternMatchException;
import com.fujitsu.vdmj.values.Value;

public class INCaseStmtAlternative implements Serializable
{
	private static final long serialVersionUID = 1L;

	public final LexLocation location;
	public final INPattern pattern;
	public final INStatement statement;

	public INCaseStmtAlternative(INPattern pattern, INStatement stmt)
	{
		this.location = pattern.location;
		this.pattern = pattern;
		this.statement = stmt;
	}

	@Override
	public String toString()
	{
		return "case " + pattern + " -> " + statement;
	}

	public Value eval(Value val, Context ctxt)
	{
		Context evalContext = new Context(location, "case alternative", ctxt);

		try
		{
			evalContext.putList(pattern.getNamedValues(val, ctxt));
			return statement.eval(evalContext);
		}
		catch (PatternMatchException e)
		{
			// TCCasesStatement tries the others
		}

		return null;
	}
}
