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

import com.fujitsu.vdmj.po.definitions.POAssignmentDefinition;
import com.fujitsu.vdmj.po.definitions.PODefinition;
import com.fujitsu.vdmj.po.definitions.PODefinitionList;

public class POLetDefContext extends POContext
{
	public final PODefinitionList localDefs;

	public POLetDefContext(PODefinitionList localDefs)
	{
		this.localDefs = localDefs;
	}

	public POLetDefContext(PODefinition localDef)
	{
		this.localDefs = new PODefinitionList();
		this.localDefs.add(localDef);
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

		if (!localDefs.isEmpty())
		{
			sb.append("let ");
			String sep = "";
			
			for (PODefinition def: localDefs)
			{
				sb.append(sep);

				if (def instanceof POAssignmentDefinition)
				{
					POAssignmentDefinition ass = (POAssignmentDefinition)def;
					sb.append(ass.name);
					sb.append(":");
					sb.append(ass.expType);
					sb.append(" = ");
					sb.append(ass.expression);
				}
				else	// POValueDefinition
				{
					sb.append(def.toExplicitString(def.location));
				}
				
				sep = ", ";
			}
			
			sb.append(" in");
		}

		return sb.toString();
	}
}
