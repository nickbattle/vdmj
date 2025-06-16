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
import com.fujitsu.vdmj.tc.patterns.TCBind;
import com.fujitsu.vdmj.tc.patterns.TCSeqBind;
import com.fujitsu.vdmj.tc.patterns.TCSetBind;
import com.fujitsu.vdmj.tc.patterns.TCTypeBind;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.TCTypeSet;
import com.fujitsu.vdmj.typechecker.Environment;

public class TCBindExitChecker extends TCBindVisitor<TCTypeSet, Environment>
{
	private TCVisitorSet<TCType, TCTypeSet, Environment> visitorSet;

	public TCBindExitChecker(TCVisitorSet<TCType, TCTypeSet, Environment> visitors)
	{
		visitorSet = visitors;
	}

	@Override
	public TCTypeSet caseBind(TCBind node, Environment arg)
	{
		return new TCTypeSet();
	}
	
	@Override
	public TCTypeSet caseSeqBind(TCSeqBind node, Environment base)
	{
		return visitorSet.applyExpressionVisitor(node.sequence, base);
	}

	@Override
	public TCTypeSet caseSetBind(TCSetBind node, Environment base)
	{
		return visitorSet.applyExpressionVisitor(node.set, base);
	}
	
	@Override
	public TCTypeSet caseTypeBind(TCTypeBind node, Environment base)
	{
		return new TCTypeSet();
	}
}
