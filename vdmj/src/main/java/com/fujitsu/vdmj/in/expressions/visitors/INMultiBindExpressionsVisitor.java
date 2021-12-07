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

package com.fujitsu.vdmj.in.expressions.visitors;

import java.util.Collection;

import com.fujitsu.vdmj.in.INVisitorSet;
import com.fujitsu.vdmj.in.patterns.INMultipleBind;
import com.fujitsu.vdmj.in.patterns.INMultipleSeqBind;
import com.fujitsu.vdmj.in.patterns.INMultipleSetBind;
import com.fujitsu.vdmj.in.patterns.INMultipleTypeBind;
import com.fujitsu.vdmj.in.patterns.visitors.INLeafMultipleBindVisitor;

/**
 * This is a helper-visitor that just applies the expression visitor of the
 * visitorSet to any components of a TCMultipleBind that involve expressions.
 */
public class INMultiBindExpressionsVisitor<E, C extends Collection<E>, S> extends INLeafMultipleBindVisitor<E, C, S>
{
	public INMultiBindExpressionsVisitor(INVisitorSet<E, C, S> visitorSet)
	{
		this.visitorSet = visitorSet;
	}
	
	@Override
	public C caseMultipleBind(INMultipleBind node, S arg)
	{
		return newCollection();
	}
	
	@Override
	public C caseMultipleTypeBind(INMultipleTypeBind node, S arg)
	{
		return visitorSet.applyTypeVisitor(node.type, arg);
	}
	
	@Override
	public C caseMultipleSeqBind(INMultipleSeqBind node, S arg)
	{
		return visitorSet.applyExpressionVisitor(node.sequence, arg);
	}
	
	@Override
	public C caseMultipleSetBind(INMultipleSetBind node, S arg)
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
		throw new RuntimeException("Unexpected INMultiBindExpressionsVisitor newCollection");
	}
}
