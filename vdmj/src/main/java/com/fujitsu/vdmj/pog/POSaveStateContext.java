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
import com.fujitsu.vdmj.po.definitions.POExplicitOperationDefinition;
import com.fujitsu.vdmj.po.definitions.POImplicitFunctionDefinition;
import com.fujitsu.vdmj.po.definitions.POImplicitOperationDefinition;
import com.fujitsu.vdmj.po.definitions.POStateDefinition;

public class POSaveStateContext extends POContext
{
	private static int count = 0;
	private static POSaveStateContext last = null;

	private static final String OLDNAME = "$oldState";
	private static final String NEWNAME = "$newState";

	private final LexLocation from;
	private final POStateDefinition state;
	private final POClassDefinition clazz;
	private final int number;
	private final boolean oldAndNew;

	public POSaveStateContext(PODefinition def, LexLocation from, boolean oldAndNew)
	{
		this.number = count++;
		this.from = from;
		this.oldAndNew = oldAndNew;

		if (def instanceof POExplicitFunctionDefinition)
		{
			POExplicitFunctionDefinition exfn = (POExplicitFunctionDefinition)def;
			this.state = exfn.stateDefinition;
			this.clazz = exfn.classDefinition;
		}
		else if (def instanceof POImplicitFunctionDefinition)
		{
			POImplicitFunctionDefinition imfn = (POImplicitFunctionDefinition)def;
			this.state = imfn.stateDefinition;
			this.clazz = imfn.classDefinition;
		}
		else if (def instanceof POExplicitOperationDefinition)
		{
			POExplicitOperationDefinition exop = (POExplicitOperationDefinition)def;
			this.state = exop.stateDefinition;
			this.clazz = exop.classDefinition;
		}
		else if (def instanceof POImplicitOperationDefinition)
		{
			POImplicitOperationDefinition imop = (POImplicitOperationDefinition)def;
			this.state = imop.stateDefinition;
			this.clazz = imop.classDefinition;
		}
		else
		{
			this.state = null;	// Produces nothing in getSource
			this.clazz = null;
		}

		last = this;
	}

	public static void reset()
	{
		count = 0;
	}

	public static String getOldName()
	{
		return last == null ? null : last.oldName();
	}

	public static String getNewName()
	{
		return last == null ? null : last.newName();
	}

	private String oldName()
	{
		return OLDNAME + (number == 0 ? "" : number);
	}

	private String newName()
	{
		return NEWNAME + (number == 0 ? "" : number);
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
				sb.append(oldName());
				sb.append(" = ");
				sb.append(state.toPattern(false, from));
				sb.append(" in");
			}
			else
			{
				sb.append("forall ");
				sb.append(oldName());
				sb.append(":");
				sb.append(state.name.toExplicitString(from));

				if (oldAndNew)
				{
					sb.append(", ");
					sb.append(newName());
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
