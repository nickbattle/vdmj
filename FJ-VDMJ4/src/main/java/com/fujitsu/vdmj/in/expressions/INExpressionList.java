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

package com.fujitsu.vdmj.in.expressions;

import com.fujitsu.vdmj.in.INMappedList;
import com.fujitsu.vdmj.tc.expressions.TCExpression;
import com.fujitsu.vdmj.tc.expressions.TCExpressionList;
import com.fujitsu.vdmj.tc.lex.TCNameList;
import com.fujitsu.vdmj.util.Utils;

@SuppressWarnings("serial")
public class INExpressionList extends INMappedList<TCExpression, INExpression>
{
	public INExpressionList(TCExpressionList from) throws Exception
	{
		super(from);
	}
	
	public INExpressionList()
	{
		super();
	}

	@Override
	public String toString()
	{
		return Utils.listToString(this);
	}

	public TCNameList getOldNames()
	{
		TCNameList list = new TCNameList();

		for (INExpression exp: this)
		{
			list.addAll(exp.getOldNames());
		}

		return list;
	}
}
