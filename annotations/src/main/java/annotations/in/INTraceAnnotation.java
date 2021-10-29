/*******************************************************************************
 *
 *	Copyright (c) 2018 Nick Battle.
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

package annotations.in;

import com.fujitsu.vdmj.in.annotations.INAnnotation;
import com.fujitsu.vdmj.in.expressions.INExpression;
import com.fujitsu.vdmj.in.expressions.INExpressionList;
import com.fujitsu.vdmj.in.statements.INStatement;
import com.fujitsu.vdmj.messages.Console;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.tc.lex.TCIdentifierToken;
import com.fujitsu.vdmj.values.Value;

public class INTraceAnnotation extends INAnnotation
{
	private static final long serialVersionUID = 1L;

	public INTraceAnnotation(TCIdentifierToken name, INExpressionList args)
	{
		super(name, args);
	}
	
	@Override
	public void inBefore(INStatement stmt, Context ctxt)
	{
		doTrace(ctxt);
	}
	
	@Override
	public void inBefore(INExpression exp, Context ctxt)
	{
		doTrace(ctxt);
	}
	
	private void doTrace(Context ctxt)
	{
		if (args.isEmpty())
		{
			System.err.println("Trace: " + name.getLocation());
		}
		else
		{
			for (INExpression arg: args)
			{
				Value v = arg.eval(ctxt);
				Console.err.println("Trace: " + name.getLocation() + ", " + arg + " = " + v);
			}
		}
	}
}
