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

package com.fujitsu.vdmj.tc.patterns.visitors;

import com.fujitsu.vdmj.tc.TCVisitorSet;
import com.fujitsu.vdmj.tc.expressions.EnvTriple;
import com.fujitsu.vdmj.tc.lex.TCNameSet;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.tc.patterns.TCMultipleBind;
import com.fujitsu.vdmj.tc.patterns.TCMultipleSeqBind;
import com.fujitsu.vdmj.tc.patterns.TCMultipleSetBind;
import com.fujitsu.vdmj.tc.patterns.TCMultipleTypeBind;

public class TCGetFreeVariablesMultipleBindVisitor extends TCMultipleBindVisitor<TCNameSet, EnvTriple>
{
	private final TCVisitorSet<TCNameToken, TCNameSet, EnvTriple> visitorSet;
	
	public TCGetFreeVariablesMultipleBindVisitor(TCVisitorSet<TCNameToken, TCNameSet, EnvTriple> visitors)
	{
		visitorSet = visitors;
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
