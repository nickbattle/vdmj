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
 *	SPDX-License-Identifier: GPL-3.0-or-later
 *
 ******************************************************************************/

package com.fujitsu.vdmj.pog;

import com.fujitsu.vdmj.po.expressions.POExpression;
import com.fujitsu.vdmj.po.patterns.POIdentifierPattern;
import com.fujitsu.vdmj.po.patterns.POPattern;
import com.fujitsu.vdmj.tc.lex.TCNameSet;
import com.fujitsu.vdmj.tc.lex.TCNameToken;

public class POLetBeStContext extends POContext
{
	private final POPattern pattern;
	private final String bind;
	private final POExpression expression;
	private POExpression best;

	public POLetBeStContext(POPattern pattern, String bind, POExpression expression, POExpression best)
	{
		this.pattern = pattern;
		this.bind = bind;
		this.expression = expression;
		this.best = best;
	}

	public POLetBeStContext(TCNameToken var, String bind, POExpression expression, POExpression best)
	{
		this.pattern = new POIdentifierPattern(var);
		this.bind = bind;
		this.expression = expression;
		this.best = best;
	}

	@Override
	public boolean isScopeBoundary()
	{
		return true;
	}

	@Override
	public String getSource()
	{
		StringBuilder sb = new StringBuilder();

		sb.append("let ");
		sb.append(pattern);
		sb.append(" ");
		sb.append(bind);		// "in set", "in seq" or ": T"
		
		if (bind.startsWith(":"))
		{
			sb.append(" in ");
		}

		sb.append(" ");
		sb.append(expression);
		
		if (best != null)
		{
			sb.append(" be st ");
			sb.append(best);
		}

		sb.append(" in");
		return sb.toString();
	}
	
	@Override
	public TCNameSet reasonsAbout()
	{
		TCNameSet names = new TCNameSet();
		
		names.addAll(pattern.getVariableNames());
		
		if (expression != null)
		{
			names.addAll(expression.getVariableNames());
		}
		
		if (best != null)
		{
			names.addAll(best.getVariableNames());
		}
		
		return names;
	}
}
