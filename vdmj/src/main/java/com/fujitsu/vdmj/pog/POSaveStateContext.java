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

import com.fujitsu.vdmj.po.definitions.POClassDefinition;
import com.fujitsu.vdmj.po.definitions.PODefinition;
import com.fujitsu.vdmj.po.definitions.POStateDefinition;

public class POSaveStateContext extends POContext
{
	public static final String OLDNAME = "$oldState";

	POStateDefinition state;
	POClassDefinition clazz;

	public POSaveStateContext(PODefinition state)
	{
		if (state instanceof POStateDefinition)
		{
			this.state = (POStateDefinition)state;
			this.clazz = null;
		}
		else if (state instanceof POClassDefinition)
		{
			this.clazz = (POClassDefinition)state;
			this.state = null;
		}
		else
		{
			this.state = null;	// Produces nothing in getSource
			this.clazz = null;
		}
	}

	@Override
	public String getSource()
	{
		StringBuilder sb = new StringBuilder();

		if (state != null)
		{
			sb.append("let ");
			sb.append(OLDNAME);
			sb.append(" = ");
			sb.append(state.toPattern(false));
			sb.append(" in");
		}
		else if (clazz != null)
		{
			// Not defined!?
		}

		return sb.toString();
	}
}
