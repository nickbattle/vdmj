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

package com.fujitsu.vdmj.tc.patterns.visitors;

import com.fujitsu.vdmj.tc.TCVisitorSet;
import com.fujitsu.vdmj.tc.lex.TCNameSet;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.tc.patterns.TCMultipleBind;
import com.fujitsu.vdmj.tc.patterns.TCMultipleSeqBind;
import com.fujitsu.vdmj.tc.patterns.TCMultipleSetBind;
import com.fujitsu.vdmj.tc.patterns.TCMultipleTypeBind;
import com.fujitsu.vdmj.typechecker.Environment;

public class TCFreeVariableMultipleBindVisitor extends TCMultipleBindVisitor<TCNameSet, Environment>
{
	private final TCVisitorSet<TCNameToken, TCNameSet, Environment> visitorSet;
	
	public TCFreeVariableMultipleBindVisitor(TCVisitorSet<TCNameToken, TCNameSet, Environment> visitors)
	{
		assert visitors != null : "Visitor set cannot be null";
		visitorSet = visitors;
	}

	@Override
	public TCNameSet caseMultipleBind(TCMultipleBind node, Environment arg)
	{
		return new TCNameSet();
	}
	
	@Override
	public TCNameSet caseMultipleSeqBind(TCMultipleSeqBind node, Environment arg)
	{
		return visitorSet.applyExpressionVisitor(node.sequence, arg);
	}
	
	@Override
	public TCNameSet caseMultipleSetBind(TCMultipleSetBind node, Environment arg)
	{
		return visitorSet.applyExpressionVisitor(node.set, arg);
	}
	
	@Override
	public TCNameSet caseMultipleTypeBind(TCMultipleTypeBind node, Environment arg)
	{
		return visitorSet.applyTypeVisitor(node.type, arg);
	}
}
