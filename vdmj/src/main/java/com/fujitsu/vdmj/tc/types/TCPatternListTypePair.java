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

package com.fujitsu.vdmj.tc.types;

import com.fujitsu.vdmj.tc.TCNode;
import com.fujitsu.vdmj.tc.definitions.TCDefinitionList;
import com.fujitsu.vdmj.tc.patterns.TCPattern;
import com.fujitsu.vdmj.tc.patterns.TCPatternList;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.NameScope;

public class TCPatternListTypePair extends TCNode
{
	private static final long serialVersionUID = 1L;
	public final TCPatternList patterns;
	public TCType type;

	public TCPatternListTypePair(TCPatternList patterns, TCType type)
	{
		this.patterns = patterns;
		this.type = type;
	}

	public TCTypeList getTypeList()
	{
		TCTypeList list = new TCTypeList();

		for (int i=0; i<patterns.size(); i++)
		{
			list.add(type);
		}

		return list;
	}

	public TCDefinitionList getDefinitions(NameScope scope)
	{
		TCDefinitionList list = new TCDefinitionList();

		for (TCPattern p: patterns)
		{
			list.addAll(p.getDefinitions(type, scope));
		}

		return list;
	}

	public void typeResolve(Environment base)
	{
		patterns.typeResolve(base);
		type = type.typeResolve(base, null);
	}

	@Override
	public String toString()
	{
		return "(" + patterns + ":" + type + ")";
	}
}
