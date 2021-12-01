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
import com.fujitsu.vdmj.po.expressions.visitors.POExpressionVisitor;
import com.fujitsu.vdmj.po.patterns.POMultipleSeqBind;
import com.fujitsu.vdmj.po.patterns.POMultipleSetBind;
import com.fujitsu.vdmj.po.patterns.POMultipleTypeBind;
import com.fujitsu.vdmj.po.patterns.POPattern;
import com.fujitsu.vdmj.tc.types.visitors.TCTypeVisitor;

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
			// None
		}

		@Override
		protected C newCollection()
		{
			return null;
		}
	};

 	@Override
	public C caseMultipleSeqBind(POMultipleSeqBind node, S arg)
	{
 		POExpressionVisitor<C, S> expVisitor = visitorSet.getExpressionVisitor();
 		POPatternVisitor<C, S> patVisitor = visitorSet.getPatternVisitor();
 		C all = newCollection();
 		
 		if (expVisitor != null)
 		{
 			all.addAll(node.sequence.apply(expVisitor, arg));
 		}
 		
 		if (patVisitor != null)
 		{
 			for (POPattern p: node.plist)
 			{
 				all.addAll(p.apply(patVisitor, arg));
 			}
 		}
 		
 		return all;
	}

 	@Override
	public C caseMultipleSetBind(POMultipleSetBind node, S arg)
	{
 		POExpressionVisitor<C, S> expVisitor = visitorSet.getExpressionVisitor();
 		POPatternVisitor<C, S> patVisitor = visitorSet.getPatternVisitor();
 		C all = newCollection();
 		
 		if (expVisitor != null)
 		{
 			all.addAll(node.set.apply(expVisitor, arg));
 		}
 		
 		if (patVisitor != null)
 		{
 			for (POPattern p: node.plist)
 			{
 				all.addAll(p.apply(patVisitor, arg));
 			}
 		}
 		
 		return all;
	}

 	@Override
	public C caseMultipleTypeBind(POMultipleTypeBind node, S arg)
	{
 		TCTypeVisitor<C, S> typeVisitor = visitorSet.getTypeVisitor();
 		POPatternVisitor<C, S> patVisitor = visitorSet.getPatternVisitor();
 		C all = newCollection();
 		
 		if (typeVisitor != null)
 		{
 			all.addAll(node.type.apply(typeVisitor, arg));
 		}

 		if (patVisitor != null)
 		{
 			for (POPattern p: node.plist)
 			{
 				all.addAll(p.apply(patVisitor, arg));
 			}
 		}

 		return all;
	}

 	abstract protected C newCollection();
}
