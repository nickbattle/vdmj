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

package com.fujitsu.vdmj.ast.definitions;

import java.util.Vector;

/**
 * A class to hold a list of Definitions.
 */
@SuppressWarnings("serial")
public class ASTDefinitionList extends Vector<ASTDefinition>
{
	public ASTDefinitionList()
	{
		super();
	}

	public ASTDefinitionList(ASTDefinition definition)
	{
		add(definition);
	}

	public void setAccessibility(ASTAccessSpecifier access)
	{
		for (ASTDefinition d: this)
		{
			d.setAccessSpecifier(access);
		}
	}

	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder();

		for (ASTDefinition d: this)
		{
			sb.append(d.toString());
			sb.append("\n");
		}

		return sb.toString();
	}
	
	@Override
	public boolean equals(Object other)
	{
		if (other instanceof ASTDefinitionList)
		{
			return super.equals(other);
		}
		
		return false;
	}
}
