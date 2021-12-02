/*******************************************************************************
 *
 *	Copyright (c) 2021 Nick Battle.
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

package com.fujitsu.vdmj.tc.expressions.visitors;

import java.util.Collection;

import com.fujitsu.vdmj.tc.TCVisitorSet;
import com.fujitsu.vdmj.tc.patterns.TCMultipleBind;
import com.fujitsu.vdmj.tc.patterns.TCMultipleSeqBind;
import com.fujitsu.vdmj.tc.patterns.TCMultipleSetBind;
import com.fujitsu.vdmj.tc.patterns.visitors.TCLeafMultipleBindVisitor;

abstract public class TCMultiBindExpressionsVisitor<E, C extends Collection<E>, S> extends TCLeafMultipleBindVisitor<E, C, S>
{
	public TCMultiBindExpressionsVisitor(TCVisitorSet<E, C, S> visitorSet)
	{
		this.visitorSet = visitorSet;
	}
	
	@Override
	public C caseMultipleBind(TCMultipleBind node, S arg)
	{
		return newCollection();
	}
	
	@Override
	public C caseMultipleSeqBind(TCMultipleSeqBind node, S arg)
	{
		return visitorSet.applyExpressionVisitor(node.sequence, arg);
	}
	
	@Override
	public C caseMultipleSetBind(TCMultipleSetBind node, S arg)
	{
		return visitorSet.applyExpressionVisitor(node.set, arg);
	}
}
