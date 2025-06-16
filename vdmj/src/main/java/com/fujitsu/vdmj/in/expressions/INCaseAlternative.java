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
 *	along with VDMJ.  If not, see &lt;http://www.gnu.org/licenses/&gt;.
 *	SPDX-License-Identifier: GPL-3.0-or-later
 *
 ******************************************************************************/

package com.fujitsu.vdmj.in.expressions;

import com.fujitsu.vdmj.in.INNode;
import com.fujitsu.vdmj.in.patterns.INPattern;
import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.runtime.PatternMatchException;
import com.fujitsu.vdmj.values.Value;

public class INCaseAlternative extends INNode
{
	private static final long serialVersionUID = 1L;

	public final LexLocation location;
	public final INExpression cexp;
	public final INPattern pattern;
	public final INExpression result;

	public INCaseAlternative(INExpression cexp, INPattern pattern, INExpression result)
	{
		this.location = pattern.location;
		this.cexp = cexp;
		this.pattern = pattern;
		this.result = result;
	}

	@Override
	public String toString()
	{
		return pattern + " -> " + result;
	}

	public Value eval(Value val, Context ctxt)
	{
		Context evalContext = new Context(location, "case alternative", ctxt);

		try
		{
			evalContext.putList(pattern.getNamedValues(val, ctxt));
			return result.eval(evalContext);
		}
		catch (PatternMatchException e)
		{
			// Silently fail (TCCasesExpression will try the others)
		}

		return null;
	}
}
