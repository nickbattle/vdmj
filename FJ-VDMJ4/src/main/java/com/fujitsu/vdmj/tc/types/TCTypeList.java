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

import com.fujitsu.vdmj.ast.types.ASTType;
import com.fujitsu.vdmj.ast.types.ASTTypeList;
import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.tc.TCMappedList;
import com.fujitsu.vdmj.util.Utils;

@SuppressWarnings("serial")
public class TCTypeList extends TCMappedList<ASTType, TCType>
{
	public TCTypeList(ASTTypeList from) throws Exception
	{
		super(from);
	}

	public TCTypeList()
	{
		super();
	}

	public TCTypeList(TCType act)
	{
		add(act);
	}

	@Override
	public boolean add(TCType t)
	{
		return super.add(t);
	}

	public TCType getType(LexLocation location)
	{
		TCType result = null;

		if (this.size() == 1)
		{
			result = iterator().next();
		}
		else
		{
			result = new TCProductType(location, this);
		}

		return result;
	}

	@Override
	public String toString()
	{
		return "(" + Utils.listToString(this) + ")";
	}
	
	public TCTypeList getComposeTypes()
	{
		TCTypeList list = new TCTypeList();
		
		for (TCType type: this)
		{
			list.addAll(type.getComposeTypes());
		}
		
		return list;
	}
}
