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
 *
 ******************************************************************************/

package com.fujitsu.vdmj.tc.patterns;

import com.fujitsu.vdmj.tc.expressions.TCLeafExpressionVisitor;
import com.fujitsu.vdmj.tc.lex.TCNameList;
import com.fujitsu.vdmj.tc.lex.TCNameToken;

public class TCGetVariablesVisitor extends TCLeafPatternVisitor<TCNameToken, TCNameList, Object>
{
	@Override
	protected TCNameList newCollection()
	{
		return new TCNameList();
	}

	@Override
	protected TCLeafExpressionVisitor<TCNameToken, TCNameList, Object> getExpressionVisitor()
	{
		return null;	// No variables in expression patterns :)
	}

	@Override
	public TCNameList casePattern(TCPattern node, Object arg)
	{
		return newCollection();
	}
	
	@Override
	public TCNameList caseIdentifierPattern(TCIdentifierPattern node, Object arg)
	{
		return new TCNameList(node.name);
	}
}
