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

package com.fujitsu.vdmj.po.patterns;

import com.fujitsu.vdmj.po.POMappedList;
import com.fujitsu.vdmj.tc.lex.TCNameSet;
import com.fujitsu.vdmj.tc.patterns.TCTypeBind;
import com.fujitsu.vdmj.tc.patterns.TCTypeBindList;
import com.fujitsu.vdmj.util.Utils;

public class POTypeBindList extends POMappedList<TCTypeBind, POTypeBind>
{
	private static final long serialVersionUID = 1L;

	public POTypeBindList(TCTypeBindList from) throws Exception
	{
		super(from);
	}
	
	public POTypeBindList()
	{
		super();
	}

	public TCNameSet getVariableNames()
	{
		TCNameSet all = new TCNameSet();
		
		for (POTypeBind mb: this)
		{
			all.addAll(mb.getVariableNames());
		}
		
		return all;
	}

	@Override
	public String toString()
	{
		return Utils.listToString(this);
	}
}
