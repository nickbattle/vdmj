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

import com.fujitsu.vdmj.tc.TCVisitorSet;
import com.fujitsu.vdmj.tc.expressions.EnvTriple;
import com.fujitsu.vdmj.tc.expressions.TCExpressionVisitor;
import com.fujitsu.vdmj.tc.lex.TCNameSet;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.tc.types.TCTypeVisitor;

public class TCGetFreeVariablesMultipleBindVisitor extends TCMultipleBindVisitor<TCNameSet, EnvTriple>
{
	private static class VisitorSet extends TCVisitorSet<TCNameToken, TCNameSet, EnvTriple>
	{
		private final com.fujitsu.vdmj.tc.expressions.TCGetFreeVariablesVisitor expVisitor;
		private final com.fujitsu.vdmj.tc.types.TCGetFreeVariablesVisitor typeVisitor;
		
		public VisitorSet()
		{
			expVisitor = new com.fujitsu.vdmj.tc.expressions.TCGetFreeVariablesVisitor(this);
			typeVisitor = new com.fujitsu.vdmj.tc.types.TCGetFreeVariablesVisitor(this);
		}
		
		@Override
		public TCExpressionVisitor<TCNameSet, EnvTriple> getExpressionVisitor()
		{
			return expVisitor;
		}
		
		@Override
		public TCTypeVisitor<TCNameSet, EnvTriple> getTypeVisitor()
		{
			return typeVisitor;
		}
	}
	
	private final VisitorSet visitorSet;
	
	public TCGetFreeVariablesMultipleBindVisitor()
	{
		visitorSet = new VisitorSet();
	}

	@Override
	public TCNameSet caseMultipleBind(TCMultipleBind node, EnvTriple arg)
	{
		return new TCNameSet();
	}
	
	@Override
	public TCNameSet caseMultipleSeqBind(TCMultipleSeqBind node, EnvTriple arg)
	{
		return node.sequence.apply(visitorSet.getExpressionVisitor(), arg);
	}
	
	@Override
	public TCNameSet caseMultipleSetBind(TCMultipleSetBind node, EnvTriple arg)
	{
		return node.set.apply(visitorSet.getExpressionVisitor(), arg);
	}
	
	@Override
	public TCNameSet caseMultipleTypeBind(TCMultipleTypeBind node, EnvTriple arg)
	{
		return node.type.apply(visitorSet.getTypeVisitor(), arg);
	}
}
