/*******************************************************************************
 *
 *	Copyright (c) 2023 Nick Battle.
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
package com.fujitsu.vdmj.po.patterns.visitors;

import com.fujitsu.vdmj.po.patterns.POIdentifierPattern;
import com.fujitsu.vdmj.po.patterns.POPattern;
import com.fujitsu.vdmj.tc.lex.TCNameList;
import com.fujitsu.vdmj.tc.lex.TCNameToken;

/**
 * Search a pattern for hidden variables, and return a list.
 */
public class POHiddenVariablesVisitor extends POLeafPatternVisitor<TCNameToken, TCNameList, Object>
{
	@Override
	protected TCNameList newCollection()
	{
		return new TCNameList();
	}

	@Override
	public TCNameList casePattern(POPattern node, Object arg)
	{
		return newCollection();
	}
	
	@Override
	public TCNameList caseIdentifierPattern(POIdentifierPattern node, Object arg)
	{
		TCNameList result = newCollection();
		
		if (node.name.getHides() != null)
		{
			result.add(node.name);
		}
		
		return result;
	}
}
