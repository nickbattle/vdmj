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

package com.fujitsu.vdmj.tc.lex;

import java.util.Vector;

import com.fujitsu.vdmj.ast.lex.LexNameList;
import com.fujitsu.vdmj.ast.lex.LexNameToken;
import com.fujitsu.vdmj.mapper.Mappable;
import com.fujitsu.vdmj.util.Utils;

public class TCNameList extends Vector<TCNameToken> implements Mappable
{
	public TCNameList()
	{
		super();
	}

	public TCNameList(TCNameToken name)
	{
		super();
		add(name);
	}

	public TCNameList(LexNameList list)
	{
		for (LexNameToken name: list)
		{
			add(new TCNameToken(name));
		}
	}

	@Override
	public String toString()
	{
		return Utils.listToString(this);
	}
	
	public LexNameList getLex()
	{
		LexNameList list = new LexNameList();
		
		for (TCNameToken name: this)
		{
			list.add(name.getLex());
		}
		
		return list;
	}

	public boolean hasDuplicates()
	{
		int len = size();

		for (int i=0; i<len; i++)
		{
			TCNameToken name = get(i);

			for (int j=i+1; j<len; j++)
			{
				if (get(j).equals(name))
				{
					return true;
				}
			}
		}

		return false;
	}

	public TCNameList matching(String sought)
	{
		TCNameList result = new TCNameList();

		for (TCNameToken name: this)
		{
			if (name.getModule().equals(sought))
			{
				result.add(name);
			}
		}

		return result;
	}
}
