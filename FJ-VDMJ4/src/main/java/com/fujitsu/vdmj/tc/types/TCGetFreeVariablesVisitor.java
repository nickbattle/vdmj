/*******************************************************************************
 *
 *	Copyright (c) 2020 Nick Battle.
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

package com.fujitsu.vdmj.tc.types;

import com.fujitsu.vdmj.tc.TCVisitorSet;
import com.fujitsu.vdmj.tc.expressions.EnvTriple;
import com.fujitsu.vdmj.tc.lex.TCNameSet;
import com.fujitsu.vdmj.tc.lex.TCNameToken;

public class TCGetFreeVariablesVisitor extends TCLeafTypeVisitor<TCNameToken, TCNameSet, EnvTriple>
{
	public TCGetFreeVariablesVisitor(TCVisitorSet<TCNameToken, TCNameSet, EnvTriple> visitors)
	{
		visitorSet = visitors;
	}

	@Override
	protected TCNameSet newCollection()
	{
		return new TCNameSet();
	}

	@Override
	public TCNameSet caseType(TCType node, EnvTriple arg)
	{
		return newCollection();		// Default has no names
	}

	@Override
	public TCNameSet caseNamedType(TCNamedType node, EnvTriple arg)
	{
		if (done.contains(node))
		{
			return newCollection();
		}
		else
		{
			done.add(node);

			if (arg.env.findType(node.typename, node.typename.getModule()) == null)
			{
				// Invariant values covered in TCTypeDefinition
				return new TCNameSet(node.typename.getExplicit(true));
			}
			else
			{
				return new TCNameSet();
			}
		}
	}
	
	@Override
	public TCNameSet caseRecordType(TCRecordType node, EnvTriple arg)
	{
		if (done.contains(node))
		{
			return newCollection();
		}
		else
		{
			done.add(node);

			if (arg.env.findType(node.name, node.name.getModule()) == null)
			{
				// Invariant values covered in TCTypeDefinition
				return new TCNameSet(node.name.getExplicit(true));
			}
			else
			{
				return new TCNameSet();
			}
		}
	}
}
