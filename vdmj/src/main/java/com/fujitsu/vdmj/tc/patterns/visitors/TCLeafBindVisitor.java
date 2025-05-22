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

package com.fujitsu.vdmj.tc.patterns.visitors;

import java.util.Collection;

import com.fujitsu.vdmj.tc.TCVisitorSet;
import com.fujitsu.vdmj.tc.patterns.TCBind;
import com.fujitsu.vdmj.tc.patterns.TCSeqBind;
import com.fujitsu.vdmj.tc.patterns.TCSetBind;
import com.fujitsu.vdmj.tc.patterns.TCTypeBind;

/**
 * This TCBind visitor visits all of the leaves of a bind tree and calls
 * the basic processing methods for the simple cases.
 */
public abstract class TCLeafBindVisitor<E, C extends Collection<E>, S> extends TCBindVisitor<C, S>
{
	protected TCVisitorSet<E, C, S> visitorSet = new TCVisitorSet<E, C, S>()
	{
		@Override
		protected void setVisitors()
		{
			bindVisitor = TCLeafBindVisitor.this;
		}

		@Override
		protected C newCollection()
		{
			return TCLeafBindVisitor.this.newCollection();
		}
	};

 	@Override
	abstract public C caseBind(TCBind node, S arg);

 	@Override
	public C caseSeqBind(TCSeqBind node, S arg)
	{
 		C all = visitorSet.applyExpressionVisitor(node.sequence, arg);
		all.addAll(visitorSet.applyPatternVisitor(node.pattern, arg));
 		return all;
	}

 	@Override
	public C caseSetBind(TCSetBind node, S arg)
	{
 		C all = visitorSet.applyExpressionVisitor(node.set, arg);
		all.addAll(visitorSet.applyPatternVisitor(node.pattern, arg));
 		return all;
	}

 	@Override
	public C caseTypeBind(TCTypeBind node, S arg)
	{
		C all = visitorSet.applyTypeVisitor(node.type, arg);
		all.addAll(visitorSet.applyPatternVisitor(node.pattern, arg));
 		return all;
	}

 	abstract protected C newCollection();
}
