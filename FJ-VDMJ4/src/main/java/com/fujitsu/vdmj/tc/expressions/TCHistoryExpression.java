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

package com.fujitsu.vdmj.tc.expressions;

import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.lex.Token;
import com.fujitsu.vdmj.tc.definitions.TCClassDefinition;
import com.fujitsu.vdmj.tc.definitions.TCDefinition;
import com.fujitsu.vdmj.tc.lex.TCNameList;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.tc.types.TCNaturalType;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.TCTypeList;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.NameScope;

public class TCHistoryExpression extends TCExpression
{
	private static final long serialVersionUID = 1L;
	public final Token hop;
	public final TCNameList opnames;

	public TCHistoryExpression(LexLocation location, Token hop, TCNameList opnames)
	{
		super(location);
		this.hop = hop;
		this.opnames = opnames;
	}

	@Override
	public TCType typeCheck(Environment env, TCTypeList qualifiers, NameScope scope, TCType constraint)
	{
		TCClassDefinition classdef = env.findClassDefinition();
	
		for (TCNameToken opname: opnames)
		{
			int found = 0;
	
			for (TCDefinition def: classdef.getDefinitions())
			{
				if (def.name != null && def.name.matches(opname))
				{
					found++;
	
					if (!def.isCallableOperation())
					{
						opname.report(3105, opname + " is not an explicit operation");
					}
					
					if (def.isPure())
					{
						opname.report(3342, "Cannot use history counters for pure operations");
					}
					
					if (!def.isStatic() && env.isStatic())
					{
						opname.report(3349, "Cannot see non-static operation from static context");
					}
				}
			}
	
			if (found == 0)
			{
				opname.report(3106, opname + " is not in scope");
			}
			else if (found > 1)
			{
				opname.warning(5004, "History expression of overloaded operation");
			}
	
			if (opname.getName().equals(classdef.name.getName()))
			{
				opname.report(3107, "Cannot use history of a constructor");
			}
		}
	
		return new TCNaturalType(location);
	}

	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder();

		sb.append(hop.toString().toLowerCase());
		sb.append("(");
		String sep = "";

		for (TCNameToken opname: opnames)
		{
			sb.append(sep);
			sep = ", ";
			sb.append(opname.getName());
		}

		sb.append(")");
		return sb.toString();
	}
}
