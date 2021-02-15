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

package com.fujitsu.vdmj.po.types;

import com.fujitsu.vdmj.po.PONode;
import com.fujitsu.vdmj.po.patterns.POPatternList;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.TCTypeList;

public class POPatternListTypePair extends PONode
{
	private static final long serialVersionUID = 1L;
	public final POPatternList patterns;
	public final TCType type;

	public POPatternListTypePair(POPatternList patterns, TCType type)
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

	@Override
	public String toString()
	{
		return "(" + patterns + ":" + type + ")";
	}
}
