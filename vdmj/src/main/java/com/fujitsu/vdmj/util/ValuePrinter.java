/*******************************************************************************
 *
 *	Copyright (c) 2022 Nick Battle.
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

package com.fujitsu.vdmj.util;

import java.util.List;
import java.util.Vector;

import com.fujitsu.vdmj.values.Value;
import com.fujitsu.vdmj.values.visitors.LeafValueVisitor;

public class ValuePrinter extends LeafValueVisitor<String, List<String>, Object>
{
	public static void print(Value v)
	{
		ValuePrinter visitor = new ValuePrinter();
		
		for (String line: v.apply(visitor, null))
		{
			System.out.println(line);
		}
	}
	
	@Override
	public List<String> caseValue(Value node, Object arg)
	{
		List<String> result = newCollection();
		result.add(node.toString());
		return result;
	}

	@Override
	protected List<String> newCollection()
	{
		return new Vector<String>();
	}
}
