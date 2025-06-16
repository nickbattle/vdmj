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
import com.fujitsu.vdmj.tc.patterns.TCConcatenationPattern;
import com.fujitsu.vdmj.tc.patterns.TCExpressionPattern;
import com.fujitsu.vdmj.tc.patterns.TCMapPattern;
import com.fujitsu.vdmj.tc.patterns.TCMapUnionPattern;
import com.fujitsu.vdmj.tc.patterns.TCMapletPattern;
import com.fujitsu.vdmj.tc.patterns.TCNamePatternPair;
import com.fujitsu.vdmj.tc.patterns.TCObjectPattern;
import com.fujitsu.vdmj.tc.patterns.TCPattern;
import com.fujitsu.vdmj.tc.patterns.TCRecordPattern;
import com.fujitsu.vdmj.tc.patterns.TCSeqPattern;
import com.fujitsu.vdmj.tc.patterns.TCSetPattern;
import com.fujitsu.vdmj.tc.patterns.TCTuplePattern;
import com.fujitsu.vdmj.tc.patterns.TCUnionPattern;

/**
 * This TCPattern visitor visits all of the leaves of a pattern tree and calls
 * the basic processing methods for the simple patterns.
 */
public abstract class TCLeafPatternVisitor<E, C extends Collection<E>, S> extends TCPatternVisitor<C, S>
{
	protected TCVisitorSet<E, C, S> visitorSet = new TCVisitorSet<E, C, S>()
	{
		@Override
		protected void setVisitors()
		{
			patternVisitor = TCLeafPatternVisitor.this;
		}

		@Override
		protected C newCollection()
		{
			return TCLeafPatternVisitor.this.newCollection();
		}
	};
	
	@Override
	public C caseConcatenationPattern(TCConcatenationPattern node, S arg)
	{
 		C all = node.left.apply(this, arg);
 		all.addAll(node.right.apply(this, arg));
 		return all;
	}

 	@Override
	public C caseExpressionPattern(TCExpressionPattern node, S arg)
	{
		return visitorSet.applyExpressionVisitor(node.exp, arg);
	}

 	@Override
	public C caseMapPattern(TCMapPattern node, S arg)
	{
 		C all = newCollection();
 		
 		for (TCMapletPattern maplet: node.maplets)
 		{
 			all.addAll(maplet.apply(this, arg));
 		}
 		
 		return all;
	}
 	
 	@Override
 	public C caseMapletPattern(TCMapletPattern node, S arg)
 	{
		C all = node.from.apply(this, arg);
		all.addAll(node.to.apply(this, arg));
		return all;
 	}

 	@Override
	public C caseMapUnionPattern(TCMapUnionPattern node, S arg)
	{
 		C all = node.left.apply(this, arg);
 		all.addAll(node.right.apply(this, arg));
 		return all;
	}

 	@Override
	public C caseObjectPattern(TCObjectPattern node, S arg)
	{
 		C all = newCollection();
 		
 		for (TCNamePatternPair pair: node.fieldlist)
 		{
 			all.addAll(pair.pattern.apply(this, arg));
 		}
 		
 		return all;
	}

 	@Override
	public C caseRecordPattern(TCRecordPattern node, S arg)
	{
 		C all = newCollection();
 		
 		for (TCPattern pattern: node.plist)
 		{
 			all.addAll(pattern.apply(this, arg));
 		}
 		
 		return all;
	}

 	@Override
	public C caseSeqPattern(TCSeqPattern node, S arg)
	{
		C all = newCollection();
 		
 		for (TCPattern pattern: node.plist)
 		{
 			all.addAll(pattern.apply(this, arg));
 		}
 		
 		return all;
	}

 	@Override
	public C caseSetPattern(TCSetPattern node, S arg)
	{
		C all = newCollection();
 		
 		for (TCPattern pattern: node.plist)
 		{
 			all.addAll(pattern.apply(this, arg));
 		}
 		
 		return all;
	}

 	@Override
	public C caseTuplePattern(TCTuplePattern node, S arg)
	{
		C all = newCollection();
 		
 		for (TCPattern pattern: node.plist)
 		{
 			all.addAll(pattern.apply(this, arg));
 		}
 		
 		return all;
	}

 	@Override
	public C caseUnionPattern(TCUnionPattern node, S arg)
	{
 		C all = node.left.apply(this, arg);
 		all.addAll(node.right.apply(this, arg));
 		return all;
	}

 	abstract protected C newCollection();
}
