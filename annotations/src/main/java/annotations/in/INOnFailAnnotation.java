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
 *	SPDX-License-Identifier: GPL-3.0-or-later
 *
 ******************************************************************************/

package annotations.in;

import com.fujitsu.vdmj.in.annotations.INAnnotation;
import com.fujitsu.vdmj.in.annotations.INAnnotationList;
import com.fujitsu.vdmj.in.expressions.INExpression;
import com.fujitsu.vdmj.in.expressions.INExpressionList;
import com.fujitsu.vdmj.in.expressions.INIntegerLiteralExpression;
import com.fujitsu.vdmj.messages.Console;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.runtime.ValueException;
import com.fujitsu.vdmj.tc.lex.TCIdentifierToken;
import com.fujitsu.vdmj.values.Value;

public class INOnFailAnnotation extends INAnnotation
{
	private static final long serialVersionUID = 1L;
	protected final String format;
	protected final INAnnotationList doclinks;	// INDocLinkAnnotations

	public INOnFailAnnotation(TCIdentifierToken name, INExpressionList args, String format, INAnnotationList doclinks)
	{
		super(name, args);
		this.format = format;
		this.doclinks = doclinks;
	}
	
	@Override
	public void inAfter(INExpression exp, Value rv, Context ctxt)
	{
		try
		{
			if (!rv.boolValue(ctxt))	// ONLY if we fail!
			{
				String errno = "";
				int offset = 1;			// By default, args(1) is the 1st
				
				if (args.get(0) instanceof INIntegerLiteralExpression)
				{
					INIntegerLiteralExpression num = (INIntegerLiteralExpression)args.get(0);
					errno = String.format("%04d: ", num.value.value);
					offset = 2;
				}
				
				Object[] values = new Value[args.size() - offset];
				
				for (int p = offset; p < args.size(); p++)
				{
					values[p - offset] = args.get(p).eval(ctxt);
				}
				
				String location = "";
				String useformat = format;
				
				if (format.endsWith("$"))	// Add @OnFail location to output
				{
					 location = name.getLocation().toString();
					 useformat = format.substring(0, format.length() - 1);
				}
							
				Console.out.printf(errno + useformat + location + "\n", values);
				
				if (doclinks != null)
				{
					for (INAnnotation link: doclinks)
					{
						Console.out.printf(link.toString());
					}
				}
			}
		}
		catch (ValueException e)
		{
			// Doesn't happen
		}
	}
}
