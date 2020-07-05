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

import com.fujitsu.vdmj.tc.expressions.EnvTriple;
import com.fujitsu.vdmj.tc.lex.TCNameSet;

public class TCGetFreeVariablesBindVisitor extends TCBindVisitor<TCNameSet, EnvTriple>
{
	private com.fujitsu.vdmj.tc.expressions.TCGetFreeVariablesVisitor expVisitor =
		new com.fujitsu.vdmj.tc.expressions.TCGetFreeVariablesVisitor();

	private com.fujitsu.vdmj.tc.types.TCGetFreeVariablesVisitor typeVisitor =
			new com.fujitsu.vdmj.tc.types.TCGetFreeVariablesVisitor();

	@Override
	public TCNameSet caseBind(TCBind node, EnvTriple arg)
	{
		return new TCNameSet();
	}
	
	@Override
	public TCNameSet caseSeqBind(TCSeqBind node, EnvTriple arg)
	{
		return node.sequence.apply(expVisitor, arg);
	}
	
	@Override
	public TCNameSet caseSetBind(TCSetBind node, EnvTriple arg)
	{
		return node.set.apply(expVisitor, arg);
	}
	
	@Override
	public TCNameSet caseTypeBind(TCTypeBind node, EnvTriple arg)
	{
		return node.type.apply(typeVisitor, arg);
	}
}
