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

package com.fujitsu.vdmj.po.patterns.visitors;

import java.util.Collection;

import com.fujitsu.vdmj.po.POVisitorSet;
import com.fujitsu.vdmj.po.patterns.POConcatenationPattern;
import com.fujitsu.vdmj.po.patterns.POExpressionPattern;
import com.fujitsu.vdmj.po.patterns.POMapPattern;
import com.fujitsu.vdmj.po.patterns.POMapUnionPattern;
import com.fujitsu.vdmj.po.patterns.POMapletPattern;
import com.fujitsu.vdmj.po.patterns.PONamePatternPair;
import com.fujitsu.vdmj.po.patterns.POObjectPattern;
import com.fujitsu.vdmj.po.patterns.POPattern;
import com.fujitsu.vdmj.po.patterns.PORecordPattern;
import com.fujitsu.vdmj.po.patterns.POSeqPattern;
import com.fujitsu.vdmj.po.patterns.POSetPattern;
import com.fujitsu.vdmj.po.patterns.POTuplePattern;
import com.fujitsu.vdmj.po.patterns.POUnionPattern;

/**
 * This POPattern visitor visits all of the leaves of a pattern tree and calls
 * the basic processing methods for the simple patterns.
 */
public abstract class POLeafPatternVisitor<E, C extends Collection<E>, S> extends POPatternVisitor<C, S>
{
	protected POVisitorSet<E, C, S> visitorSet = new POVisitorSet<E, C, S>()
	{
		@Override
		protected void setVisitors()
		{
			patternVisitor = POLeafPatternVisitor.this;
		}

		@Override
		protected C newCollection()
		{
			return POLeafPatternVisitor.this.newCollection();
		}
	};

 	@Override
	public C caseConcatenationPattern(POConcatenationPattern node, S arg)
	{
 		C all = node.left.apply(this, arg);
 		all.addAll(node.right.apply(this, arg));
 		return all;
	}

 	@Override
	public C caseExpressionPattern(POExpressionPattern node, S arg)
	{
		return visitorSet.applyExpressionVisitor(node.exp, arg);
	}

 	@Override
	public C caseMapPattern(POMapPattern node, S arg)
	{
 		C all = newCollection();
 		
 		for (POMapletPattern maplet: node.maplets)
 		{
 			all.addAll(maplet.apply(this, arg));
 		}
 		
 		return all;
	}
 	
 	@Override
 	public C caseMapletPattern(POMapletPattern node, S arg)
 	{
		C all = node.from.apply(this, arg);
		all.addAll(node.to.apply(this, arg));
		return all;
 	}

 	@Override
	public C caseMapUnionPattern(POMapUnionPattern node, S arg)
	{
 		C all = node.left.apply(this, arg);
 		all.addAll(node.right.apply(this, arg));
 		return all;
	}

 	@Override
	public C caseObjectPattern(POObjectPattern node, S arg)
	{
 		C all = newCollection();
 		
 		for (PONamePatternPair pair: node.fieldlist)
 		{
 			all.addAll(pair.pattern.apply(this, arg));
 		}
 		
 		return all;
	}

 	@Override
	public C caseRecordPattern(PORecordPattern node, S arg)
	{
 		C all = newCollection();
 		
 		for (POPattern pattern: node.plist)
 		{
 			all.addAll(pattern.apply(this, arg));
 		}
 		
 		return all;
	}

 	@Override
	public C caseSeqPattern(POSeqPattern node, S arg)
	{
		C all = newCollection();
 		
 		for (POPattern pattern: node.plist)
 		{
 			all.addAll(pattern.apply(this, arg));
 		}
 		
 		return all;
	}

 	@Override
	public C caseSetPattern(POSetPattern node, S arg)
	{
		C all = newCollection();
 		
 		for (POPattern pattern: node.plist)
 		{
 			all.addAll(pattern.apply(this, arg));
 		}
 		
 		return all;
	}

 	@Override
	public C caseTuplePattern(POTuplePattern node, S arg)
	{
		C all = newCollection();
 		
 		for (POPattern pattern: node.plist)
 		{
 			all.addAll(pattern.apply(this, arg));
 		}
 		
 		return all;
	}

 	@Override
	public C caseUnionPattern(POUnionPattern node, S arg)
	{
 		C all = node.left.apply(this, arg);
 		all.addAll(node.right.apply(this, arg));
 		return all;
	}

 	abstract protected C newCollection();
}
