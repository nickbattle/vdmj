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

package com.fujitsu.vdmj.tc.expressions;

import java.util.List;

import com.fujitsu.vdmj.ast.expressions.ASTExpression;
import com.fujitsu.vdmj.ast.expressions.ASTExpressionList;
import com.fujitsu.vdmj.tc.TCMappedList;
import com.fujitsu.vdmj.util.Utils;

@SuppressWarnings("serial")
public class TCExpressionList extends TCMappedList<ASTExpression, TCExpression>
{
	public TCExpressionList(ASTExpressionList from) throws Exception
	{
		super(from);
	}
	
	public TCExpressionList()
	{
		super();
	}
	
	public TCExpressionList(List<TCExpression> to)  
	{
		super();
		addAll(to);
	}

	@Override
	public String toString()
	{
		return Utils.listToString(this);
	}
}
