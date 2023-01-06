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

package com.fujitsu.vdmj.in.patterns.visitors;

import java.util.Collection;

import com.fujitsu.vdmj.in.INVisitorSet;
import com.fujitsu.vdmj.in.patterns.INMultipleSeqBind;
import com.fujitsu.vdmj.in.patterns.INMultipleSetBind;
import com.fujitsu.vdmj.in.patterns.INMultipleTypeBind;
import com.fujitsu.vdmj.in.patterns.INPattern;

/**
 * This TCMultipleBind visitor visits all of the leaves of a bind tree and calls
 * the basic processing methods for the simple patterns.
 */
public abstract class INLeafMultipleBindVisitor<E, C extends Collection<E>, S> extends INMultipleBindVisitor<C, S>
{
	protected INVisitorSet<E, C, S> visitorSet = new INVisitorSet<E, C, S>()
	{
		@Override
		protected void setVisitors()
		{
			multiBindVisitor = INLeafMultipleBindVisitor.this;
		}

		@Override
		protected C newCollection()
		{
			return INLeafMultipleBindVisitor.this.newCollection();
		}
	};

	@Override
	public C caseMultipleSeqBind(INMultipleSeqBind node, S arg)
	{
 		C all = visitorSet.applyExpressionVisitor(node.sequence, arg);
 		
		for (INPattern p: node.plist)
		{
			all.addAll(visitorSet.applyPatternVisitor(p, arg));
		}
 		
 		return all;
	}

 	@Override
	public C caseMultipleSetBind(INMultipleSetBind node, S arg)
	{
 		C all = visitorSet.applyExpressionVisitor(node.set, arg);
 		
		for (INPattern p: node.plist)
		{
			all.addAll(visitorSet.applyPatternVisitor(p, arg));
		}
 		
 		return all;
	}

 	@Override
	public C caseMultipleTypeBind(INMultipleTypeBind node, S arg)
	{
 		C all = visitorSet.applyTypeVisitor(node.type, arg);
 		
		for (INPattern p: node.plist)
		{
			all.addAll(visitorSet.applyPatternVisitor(p, arg));
		}

 		return all;
	}

 	abstract protected C newCollection();
}
