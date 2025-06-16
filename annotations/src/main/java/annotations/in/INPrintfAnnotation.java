/*******************************************************************************
 *
 *	Copyright (c) 2019 Nick Battle.
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

package annotations.in;

import com.fujitsu.vdmj.in.annotations.INAnnotation;
import com.fujitsu.vdmj.in.expressions.INExpression;
import com.fujitsu.vdmj.in.expressions.INExpressionList;
import com.fujitsu.vdmj.in.expressions.INStringLiteralExpression;
import com.fujitsu.vdmj.in.statements.INStatement;
import com.fujitsu.vdmj.messages.Console;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.tc.lex.TCIdentifierToken;
import com.fujitsu.vdmj.values.Value;

public class INPrintfAnnotation extends INAnnotation
{
	private static final long serialVersionUID = 1L;

	public INPrintfAnnotation(TCIdentifierToken name, INExpressionList args)
	{
		super(name, args);
	}
	
	@Override
	public void inBefore(INStatement stmt, Context ctxt)
	{
		doPrintf(ctxt);
	}
	
	@Override
	public void inBefore(INExpression exp, Context ctxt)
	{
		doPrintf(ctxt);
	}
	
	private void doPrintf(Context ctxt)
	{
		Object[] values = new Value[args.size() - 1];
		
		for (int p=1; p < args.size(); p++)
		{
			values[p-1] = args.get(p).eval(ctxt);
		}
		
		INStringLiteralExpression fmt = (INStringLiteralExpression)args.get(0);
		fmt.location.hits++;
		Console.out.printf(fmt.value.value, values);
	}
}
