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
 *	SPDX-License-Identifier: GPL-3.0-or-later
 *
 ******************************************************************************/

package com.fujitsu.vdmj.in.patterns.visitors;

import com.fujitsu.vdmj.in.INVisitorSet;
import com.fujitsu.vdmj.in.patterns.INBind;
import com.fujitsu.vdmj.in.patterns.visitors.INLeafBindVisitor;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.values.Value;
import com.fujitsu.vdmj.values.ValueList;

public class INBindUpdatableFinder extends INLeafBindVisitor<Value, ValueList, Context>
{
	public INBindUpdatableFinder(INVisitorSet<Value, ValueList, Context> visitors)
	{
		visitorSet = visitors;
	}

	@Override
	public ValueList caseBind(INBind node, Context arg)
	{
		return newCollection();
	}

	@Override
	protected ValueList newCollection()
	{
		return new ValueList();
	}
}
