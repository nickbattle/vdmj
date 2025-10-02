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

package com.fujitsu.vdmj.pog;

import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.po.definitions.POClassDefinition;
import com.fujitsu.vdmj.po.definitions.PODefinition;
import com.fujitsu.vdmj.po.definitions.POExplicitFunctionDefinition;
import com.fujitsu.vdmj.po.definitions.POImplicitFunctionDefinition;
import com.fujitsu.vdmj.po.definitions.POStateDefinition;

public class POSaveStateContext extends POContext
{
	private static int count = 0;

	private static final String OLDNAME = "$oldState";
	private static final String NEWNAME = "$newState";

	private final LexLocation from;
	private final POStateDefinition state;
	private final POClassDefinition clazz;
	private final int number;
	private final boolean oldAndNew;

	public POSaveStateContext(PODefinition fdef, LexLocation from, boolean oldAndNew)
	{
		this.number = count;
		this.from = from;
		this.oldAndNew = oldAndNew;

		if (fdef instanceof POExplicitFunctionDefinition)
		{
			POExplicitFunctionDefinition exfn = (POExplicitFunctionDefinition)fdef;
			this.state = exfn.stateDefinition;
			this.clazz = exfn.classDefinition;
		}
		else if (fdef instanceof POImplicitFunctionDefinition)
		{
			POImplicitFunctionDefinition imfn = (POImplicitFunctionDefinition)fdef;
			this.state = imfn.stateDefinition;
			this.clazz = imfn.classDefinition;
		}
		else
		{
			this.state = null;	// Produces nothing in getSource
			this.clazz = null;
		}
	}

	public static void advance()
	{
		count = count + 1;
	}

	public static String getOldName()
	{
		return OLDNAME + count;
	}

	public static String getNewName()
	{
		return NEWNAME + count;
	}

	@Override
	public String getSource()
	{
		StringBuilder sb = new StringBuilder();

		if (state != null)
		{
			if (state.location.sameModule(from))
			{
				sb.append("let ");
				sb.append(OLDNAME + number);
				sb.append(" = ");
				sb.append(state.toPattern(false, from));
				sb.append(" in");
			}
			else
			{
				sb.append("forall ");
				sb.append(OLDNAME + number);
				sb.append(":");
				sb.append(state.name.toExplicitString(from));

				if (oldAndNew)
				{
					sb.append(", ");
					sb.append(NEWNAME + number);
					sb.append(":");
					sb.append(state.name.toExplicitString(from));
				}

				sb.append(" &");
			}
		}
		else if (clazz != null)
		{
			// Not defined!?
		}

		return sb.toString();
	}
}
