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

import java.util.Map;

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
	private static POSaveStateContext current = null;

	private static final String OLDNAME = "$oldState";
	private static final String NEWNAME = "$newState";

	private final LexLocation from;
	private final POStateDefinition state;
	private final POClassDefinition clazz;
	private final int number;
	private final boolean oldAndNew;

	public String moduleName = null;
	public String moduleVar = null;
	private String previousState = null;

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

		// If this is a remote module call, note the name of the module
		// and variable name that will hold the module state after the
		// postcondition call.

		if (state != null && !state.location.sameModule(from))
		{
			moduleName = state.location.module;
			moduleVar = newName();
		}

		current = this;
	}

	public static void reset()
	{
		count = 0;
		current = null;
	}

	public static String getOldName()
	{
		return current == null ? null : current.oldName();
	}

	public static String getNewName()
	{
		return current == null ? null : current.newName();
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
	public void updateStateMap(Map<String, String> stateMap)
	{
		if (moduleName != null)
		{
			previousState = stateMap.get(moduleName);	// Previous state for this module
			stateMap.put(moduleName, moduleVar);		// New state after post_op
		}
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
				sb.append(" in ");
			}
			else
			{
				boolean forall = false;

				if (previousState != null)	// re-use an existing state value
				{
					sb.append("let ");
					sb.append(oldName());
					sb.append(" = ");
					sb.append(previousState);
					sb.append(" in ");
					
					if (oldAndNew)
					{
						sb.append("forall ");
						forall = true;
					}
				}
				else
				{
					sb.append("forall ");
					sb.append(oldName());
					sb.append(":");
					sb.append(state.name.toExplicitString(from));
					forall = true;
				}

				if (oldAndNew)
				{
					if (previousState == null) sb.append(", ");
					sb.append(newName());
					sb.append(":");
					sb.append(state.name.toExplicitString(from));
				}

				if (forall) sb.append(" &");	// Could just be a "let"
			}
		}
		else if (clazz != null)
		{
			if (clazz.location.sameModule(from))
			{
				sb.append("let ");
				sb.append(oldName());
				sb.append(" = ");
				sb.append("undefined");		// Can't create a saved object state?
				sb.append(" in");
			}
			else
			{
				sb.append("forall ");
				sb.append(oldName());
				sb.append(":");
				sb.append(clazz.name);

				if (oldAndNew)
				{
					sb.append(", ");
					sb.append(newName());
					sb.append(":");
					sb.append(clazz.name);
				}

				sb.append(" &");
			}
		}

		return sb.toString();
	}
}
