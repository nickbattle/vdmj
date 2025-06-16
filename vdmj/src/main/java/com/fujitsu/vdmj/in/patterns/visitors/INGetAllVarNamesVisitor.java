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
 *	along with VDMJ.  If not, see &lt;http://www.gnu.org/licenses/&gt;.
 *	SPDX-License-Identifier: GPL-3.0-or-later
 *
 ******************************************************************************/

package com.fujitsu.vdmj.in.patterns.visitors;

import com.fujitsu.vdmj.in.patterns.INIdentifierPattern;
import com.fujitsu.vdmj.in.patterns.INPattern;
import com.fujitsu.vdmj.tc.lex.TCNameList;
import com.fujitsu.vdmj.tc.lex.TCNameToken;

public class INGetAllVarNamesVisitor extends INLeafPatternVisitor<TCNameToken, TCNameList, Object>
{
	public INGetAllVarNamesVisitor()
	{
		// default visitorSet
	}
	
	@Override
	protected TCNameList newCollection()
	{
		return new TCNameList();
	}

	@Override
	public TCNameList casePattern(INPattern node, Object arg)
	{
		return newCollection();
	}
	
	@Override
	public TCNameList caseIdentifierPattern(INIdentifierPattern node, Object arg)
	{
		TCNameList list = newCollection();
		list.add(node.name);
		return list;
	}
}
