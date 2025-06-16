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

import java.util.Collection;

import com.fujitsu.vdmj.tc.TCVisitorSet;
import com.fujitsu.vdmj.tc.patterns.TCMultipleBind;
import com.fujitsu.vdmj.tc.patterns.TCMultipleSeqBind;
import com.fujitsu.vdmj.tc.patterns.TCMultipleSetBind;
import com.fujitsu.vdmj.tc.patterns.TCMultipleTypeBind;
import com.fujitsu.vdmj.tc.patterns.TCPattern;

/**
 * This TCMultipleBind visitor visits all of the leaves of a bind tree and calls
 * the basic processing methods for the simple cases.
 */
public abstract class TCLeafMultipleBindVisitor<E, C extends Collection<E>, S> extends TCMultipleBindVisitor<C, S>
{
	protected TCVisitorSet<E, C, S> visitorSet = new TCVisitorSet<E, C, S>()
	{
		@Override
		protected void setVisitors()
		{
			multiBindVisitor = TCLeafMultipleBindVisitor.this;
		}

		@Override
		protected C newCollection()
		{
			return TCLeafMultipleBindVisitor.this.newCollection();
		}
	};

 	@Override
	abstract public C caseMultipleBind(TCMultipleBind node, S arg);

 	@Override
	public C caseMultipleSeqBind(TCMultipleSeqBind node, S arg)
	{
 		C all = visitorSet.applyExpressionVisitor(node.sequence, arg);
 		
		for (TCPattern p: node.plist)
		{
			all.addAll(visitorSet.applyPatternVisitor(p, arg));
		}
 		
 		return all;
	}

 	@Override
	public C caseMultipleSetBind(TCMultipleSetBind node, S arg)
	{
 		C all = visitorSet.applyExpressionVisitor(node.set, arg);
 		
		for (TCPattern p: node.plist)
		{
			all.addAll(visitorSet.applyPatternVisitor(p, arg));
		}
 		
 		return all;
	}

 	@Override
	public C caseMultipleTypeBind(TCMultipleTypeBind node, S arg)
	{
 		C all = visitorSet.applyTypeVisitor(node.type, arg);

		for (TCPattern p: node.plist)
		{
			all.addAll(visitorSet.applyPatternVisitor(p, arg));
		}

 		return all;
	}

 	abstract protected C newCollection();
}
