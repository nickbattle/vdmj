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
 *	along with VDMJ.  If not, see <http://www.gnu.org/licenses/>.
 *
 ******************************************************************************/

package annotations.in;

import com.fujitsu.vdmj.in.annotations.INAnnotation;
import com.fujitsu.vdmj.in.expressions.INExpression;
import com.fujitsu.vdmj.in.expressions.INExpressionList;
import com.fujitsu.vdmj.in.expressions.INStringLiteralExpression;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.runtime.ValueException;
import com.fujitsu.vdmj.tc.lex.TCIdentifierToken;
import com.fujitsu.vdmj.values.Value;

public class INOnFailAnnotation extends INAnnotation
{
	public INOnFailAnnotation(TCIdentifierToken name, INExpressionList args)
	{
		super(name, args);
	}
	
	@Override
	public void inAfter(INExpression exp, Value rv, Context ctxt)
	{
		try
		{
			if (!rv.boolValue(ctxt))	// ONLY if we fail!
			{
				Object[] values = new Value[args.size() - 1];
				
				for (int p=1; p < args.size(); p++)
				{
					values[p-1] = args.get(p).eval(ctxt);
				}
				
				INStringLiteralExpression fmt = (INStringLiteralExpression)args.get(0);
				String fmts = fmt.value.value;
				String location = "";
				
				if (fmts.endsWith("$"))
				{
					 location = name.getLocation().toString();
					 fmts = fmts.substring(0, fmts.length() - 1);
				}
							
				System.out.printf(fmts + location + "\n", values);
			}
		}
		catch (ValueException e)
		{
			// Doesn't happen
		}
	}
}
