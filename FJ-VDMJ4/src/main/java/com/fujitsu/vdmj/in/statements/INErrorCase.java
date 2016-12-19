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

package com.fujitsu.vdmj.in.statements;

import com.fujitsu.vdmj.in.expressions.INExpression;
import com.fujitsu.vdmj.tc.lex.TCIdentifierToken;

public class INErrorCase
{
	public final TCIdentifierToken name;
	public final INExpression left;
	public final INExpression right;
	
	public INErrorCase(TCIdentifierToken name, INExpression left, INExpression right)
	{
		this.name = name;
		this.left = left;
		this.right = right;
	}
	
	@Override
	public String toString()
	{
		return "(" + name + ": " + left + "->" + right + ")";
	}

	public INExpression findExpression(int lineno)
	{
		INExpression found = left.findExpression(lineno);
		if (found != null) return found;
		return right.findExpression(lineno);
	}
}
