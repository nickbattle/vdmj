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

package com.fujitsu.vdmj.po.patterns.visitors;

import java.util.Collection;

import com.fujitsu.vdmj.po.POVisitorSet;
import com.fujitsu.vdmj.po.patterns.POMultipleSeqBind;
import com.fujitsu.vdmj.po.patterns.POMultipleSetBind;
import com.fujitsu.vdmj.po.patterns.POMultipleTypeBind;
import com.fujitsu.vdmj.po.patterns.POPattern;

/**
 * This POMultipleBind visitor visits all of the leaves of a bind tree and calls
 * the basic processing methods for the simple patterns.
 */
public abstract class POLeafMultipleBindVisitor<E, C extends Collection<E>, S> extends POMultipleBindVisitor<C, S>
{
	protected POVisitorSet<E, C, S> visitorSet = new POVisitorSet<E, C, S>()
	{
		@Override
		protected void setVisitors()
		{
			multiBindVisitor = POLeafMultipleBindVisitor.this;
		}

		@Override
		protected C newCollection()
		{
			return POLeafMultipleBindVisitor.this.newCollection();
		}
	};

 	@Override
	public C caseMultipleSeqBind(POMultipleSeqBind node, S arg)
	{
 		C all = visitorSet.applyExpressionVisitor(node.sequence, arg);
 		
		for (POPattern p: node.plist)
		{
			all.addAll(visitorSet.applyPatternVisitor(p, arg));
		}
 		
 		return all;
	}

 	@Override
	public C caseMultipleSetBind(POMultipleSetBind node, S arg)
	{
 		C all = visitorSet.applyExpressionVisitor(node.set, arg);
 		
		for (POPattern p: node.plist)
		{
			all.addAll(visitorSet.applyPatternVisitor(p, arg));
		}
 		
 		return all;
	}

 	@Override
	public C caseMultipleTypeBind(POMultipleTypeBind node, S arg)
	{
 		C all = visitorSet.applyTypeVisitor(node.type, arg);

		for (POPattern p: node.plist)
		{
			all.addAll(visitorSet.applyPatternVisitor(p, arg));
		}

 		return all;
	}

 	abstract protected C newCollection();
}
