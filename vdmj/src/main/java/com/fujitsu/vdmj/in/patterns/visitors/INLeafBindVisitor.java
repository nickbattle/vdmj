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
import com.fujitsu.vdmj.in.expressions.visitors.INExpressionVisitor;
import com.fujitsu.vdmj.in.patterns.INSeqBind;
import com.fujitsu.vdmj.in.patterns.INSetBind;
import com.fujitsu.vdmj.in.patterns.INTypeBind;
import com.fujitsu.vdmj.tc.types.visitors.TCTypeVisitor;

/**
 * This TCBind visitor visits all of the leaves of a bind tree and calls
 * the basic processing methods for the simple patterns.
 */
public abstract class INLeafBindVisitor<E, C extends Collection<E>, S> extends INBindVisitor<C, S>
{
	protected INVisitorSet<E, C, S> visitorSet;

 	@Override
	public C caseSeqBind(INSeqBind node, S arg)
	{
 		INExpressionVisitor<C, S> expVisitor = visitorSet.getExpressionVisitor();
 		INPatternVisitor<C, S> patVisitor = visitorSet.getPatternVisitor();
 		C all = newCollection();
 		
 		if (expVisitor != null)
 		{
 			all.addAll(node.sequence.apply(expVisitor, arg));
 		}
 		
 		if (patVisitor != null)
 		{
 			all.addAll(node.pattern.apply(patVisitor, arg));
 		}
 		
 		return all;
	}

 	@Override
	public C caseSetBind(INSetBind node, S arg)
	{
 		INExpressionVisitor<C, S> expVisitor = visitorSet.getExpressionVisitor();
 		INPatternVisitor<C, S> patVisitor = visitorSet.getPatternVisitor();
 		C all = newCollection();
 		
 		if (expVisitor != null)
 		{
 			all.addAll(node.set.apply(expVisitor, arg));
 		}
 		
 		if (patVisitor != null)
 		{
 			all.addAll(node.pattern.apply(patVisitor, arg));
 		}
 		
 		return all;
	}

 	@Override
	public C caseTypeBind(INTypeBind node, S arg)
	{
 		TCTypeVisitor<C, S> typeVisitor = visitorSet.getTypeVisitor();
		INPatternVisitor<C, S> patVisitor = visitorSet.getPatternVisitor();
		C all = newCollection();
 		
 		if (typeVisitor != null)
 		{
 			all.addAll(node.type.apply(typeVisitor, arg));
 		}
 		
 		if (patVisitor != null)
 		{
 			all.addAll(node.pattern.apply(patVisitor, arg));
 		}

 		return all;
	}

 	abstract protected C newCollection();
}
