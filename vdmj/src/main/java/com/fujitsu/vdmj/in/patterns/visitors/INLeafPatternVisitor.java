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
import com.fujitsu.vdmj.in.patterns.INConcatenationPattern;
import com.fujitsu.vdmj.in.patterns.INExpressionPattern;
import com.fujitsu.vdmj.in.patterns.INMapPattern;
import com.fujitsu.vdmj.in.patterns.INMapUnionPattern;
import com.fujitsu.vdmj.in.patterns.INMapletPattern;
import com.fujitsu.vdmj.in.patterns.INNamePatternPair;
import com.fujitsu.vdmj.in.patterns.INObjectPattern;
import com.fujitsu.vdmj.in.patterns.INPattern;
import com.fujitsu.vdmj.in.patterns.INRecordPattern;
import com.fujitsu.vdmj.in.patterns.INSeqPattern;
import com.fujitsu.vdmj.in.patterns.INSetPattern;
import com.fujitsu.vdmj.in.patterns.INTuplePattern;
import com.fujitsu.vdmj.in.patterns.INUnionPattern;

/**
 * This INPattern visitor visits all of the leaves of a pattern tree and calls
 * the basic processing methods for the simple patterns.
 */
public abstract class INLeafPatternVisitor<E, C extends Collection<E>, S> extends INPatternVisitor<C, S>
{
	protected INVisitorSet<E, C, S> visitorSet = new INVisitorSet<E, C, S>()
	{
		@Override
		protected void setVisitors()
		{
			patternVisitor = INLeafPatternVisitor.this;
		}

		@Override
		protected C newCollection()
		{
			return INLeafPatternVisitor.this.newCollection();
		}
	};
	
 	public INLeafPatternVisitor()
	{
		// use default visitorSet
	}
	
 	public INLeafPatternVisitor(INVisitorSet<E, C, S> inVisitorSet)
	{
		this.visitorSet = inVisitorSet;
	}

	@Override
	public C caseConcatenationPattern(INConcatenationPattern node, S arg)
	{
 		C all = node.left.apply(this, arg);
 		all.addAll(node.right.apply(this, arg));
 		return all;
	}

 	@Override
	public C caseExpressionPattern(INExpressionPattern node, S arg)
	{
		return visitorSet.applyExpressionVisitor(node.exp, arg);
	}

 	@Override
	public C caseMapPattern(INMapPattern node, S arg)
	{
 		C all = newCollection();
 		
 		for (INMapletPattern maplet: node.maplets)
 		{
 			all.addAll(maplet.from.apply(this, arg));
 			all.addAll(maplet.to.apply(this, arg));
 		}
 		
 		return all;
	}

 	@Override
	public C caseMapUnionPattern(INMapUnionPattern node, S arg)
	{
 		C all = node.left.apply(this, arg);
 		all.addAll(node.right.apply(this, arg));
 		return all;
	}

 	@Override
	public C caseObjectPattern(INObjectPattern node, S arg)
	{
 		C all = newCollection();
 		
 		for (INNamePatternPair pair: node.fieldlist)
 		{
 			all.addAll(pair.pattern.apply(this, arg));
 		}
 		
 		return all;
	}

 	@Override
	public C caseRecordPattern(INRecordPattern node, S arg)
	{
 		C all = newCollection();
 		
 		for (INPattern pattern: node.plist)
 		{
 			all.addAll(pattern.apply(this, arg));
 		}
 		
 		return all;
	}

 	@Override
	public C caseSeqPattern(INSeqPattern node, S arg)
	{
		C all = newCollection();
 		
 		for (INPattern pattern: node.plist)
 		{
 			all.addAll(pattern.apply(this, arg));
 		}
 		
 		return all;
	}

 	@Override
	public C caseSetPattern(INSetPattern node, S arg)
	{
		C all = newCollection();
 		
 		for (INPattern pattern: node.plist)
 		{
 			all.addAll(pattern.apply(this, arg));
 		}
 		
 		return all;
	}

 	@Override
	public C caseTuplePattern(INTuplePattern node, S arg)
	{
		C all = newCollection();
 		
 		for (INPattern pattern: node.plist)
 		{
 			all.addAll(pattern.apply(this, arg));
 		}
 		
 		return all;
	}

 	@Override
	public C caseUnionPattern(INUnionPattern node, S arg)
	{
 		C all = node.left.apply(this, arg);
 		all.addAll(node.right.apply(this, arg));
 		return all;
	}

 	abstract protected C newCollection();
}
