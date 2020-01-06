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

package com.fujitsu.vdmj.tc.expressions;

import com.fujitsu.vdmj.ast.expressions.ASTExpression;
import com.fujitsu.vdmj.ast.expressions.ASTExpressionList;
import com.fujitsu.vdmj.tc.TCMappedList;
import com.fujitsu.vdmj.tc.lex.TCNameSet;
import com.fujitsu.vdmj.tc.types.TCTypeSet;
import com.fujitsu.vdmj.typechecker.Environment;
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

	@Override
	public String toString()
	{
		return Utils.listToString(this);
	}

	public TCNameSet getFreeVariables(Environment globals, Environment env)
	{
		TCNameSet names = new TCNameSet();
		
		for (TCExpression exp: this)
		{
			names.addAll(exp.getFreeVariables(globals, env));
		}
		
		return names;
	}
	
	public TCTypeSet exitCheck(Environment base)
	{
		TCTypeSet result = new TCTypeSet();
		
		for (TCExpression exp: this)
		{
			result.addAll(exp.exitCheck(base));
		}
		
		return result;
	}
}
