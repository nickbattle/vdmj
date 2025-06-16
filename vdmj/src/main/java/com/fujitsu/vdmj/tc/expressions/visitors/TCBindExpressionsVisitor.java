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
 *	along with VDMJ.  If not, see &lt;http://www.gnu.org/licenses/&gt;.
 *	SPDX-License-Identifier: GPL-3.0-or-later
 *
 ******************************************************************************/

package com.fujitsu.vdmj.tc.expressions.visitors;

import java.util.Collection;

import com.fujitsu.vdmj.tc.TCVisitorSet;
import com.fujitsu.vdmj.tc.patterns.TCBind;
import com.fujitsu.vdmj.tc.patterns.TCSeqBind;
import com.fujitsu.vdmj.tc.patterns.TCSetBind;
import com.fujitsu.vdmj.tc.patterns.TCTypeBind;
import com.fujitsu.vdmj.tc.patterns.visitors.TCLeafBindVisitor;

/**
 * This is a helper-visitor that just applies the expression visitor of the
 * visitorSet to any components of a TCBind that involve expressions.
 */
public class TCBindExpressionsVisitor<E, C extends Collection<E>, S> extends TCLeafBindVisitor<E, C, S>
{
	public TCBindExpressionsVisitor(TCVisitorSet<E, C, S> visitorSet)
	{
		this.visitorSet = visitorSet;
	}
	
	@Override
	public C caseBind(TCBind node, S arg)
	{
		return newCollection();
	}
	
	@Override
	public C caseTypeBind(TCTypeBind node, S arg)
	{
		return visitorSet.applyTypeVisitor(node.type, arg);
	}
	
	@Override
	public C caseSeqBind(TCSeqBind node, S arg)
	{
		return visitorSet.applyExpressionVisitor(node.sequence, arg);
	}
	
	@Override
	public C caseSetBind(TCSetBind node, S arg)
	{
		return visitorSet.applyExpressionVisitor(node.set, arg);
	}

	@Override
	protected C newCollection()
	{
		/**
		 * This should never happen, because all of the bind cases are covered above.
		 * But we can't implement this without a subclass that knows C, so to avoid
		 * too many visitor classes, we just throw an exception here.
		 */
		throw new RuntimeException("Unexpected TCBindExpressionsVisitor newCollection");
	}
}
