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

package com.fujitsu.vdmj.ast.patterns.visitors;

import java.util.Collection;

import com.fujitsu.vdmj.ast.ASTVisitorSet;
import com.fujitsu.vdmj.ast.patterns.ASTConcatenationPattern;
import com.fujitsu.vdmj.ast.patterns.ASTExpressionPattern;
import com.fujitsu.vdmj.ast.patterns.ASTMapPattern;
import com.fujitsu.vdmj.ast.patterns.ASTMapUnionPattern;
import com.fujitsu.vdmj.ast.patterns.ASTMapletPattern;
import com.fujitsu.vdmj.ast.patterns.ASTNamePatternPair;
import com.fujitsu.vdmj.ast.patterns.ASTObjectPattern;
import com.fujitsu.vdmj.ast.patterns.ASTPattern;
import com.fujitsu.vdmj.ast.patterns.ASTRecordPattern;
import com.fujitsu.vdmj.ast.patterns.ASTSeqPattern;
import com.fujitsu.vdmj.ast.patterns.ASTSetPattern;
import com.fujitsu.vdmj.ast.patterns.ASTTuplePattern;
import com.fujitsu.vdmj.ast.patterns.ASTUnionPattern;

/**
 * This ASTPattern visitor visits all of the leaves of a pattern tree and calls
 * the basic processing methods for the simple patterns.
 */
public abstract class ASTLeafPatternVisitor<E, C extends Collection<E>, S> extends ASTPatternVisitor<C, S>
{
	protected ASTVisitorSet<E, C, S> visitorSet = new ASTVisitorSet<E, C, S>()
	{
		@Override
		protected void setVisitors()
		{
			patternVisitor = ASTLeafPatternVisitor.this;
		}

		@Override
		protected C newCollection()
		{
			return ASTLeafPatternVisitor.this.newCollection();
		}
	};

 	@Override
	public C caseConcatenationPattern(ASTConcatenationPattern node, S arg)
	{
 		C all = node.left.apply(this, arg);
 		all.addAll(node.right.apply(this, arg));
 		return all;
	}

 	@Override
	public C caseExpressionPattern(ASTExpressionPattern node, S arg)
	{
		return visitorSet.applyExpressionVisitor(node.exp, arg);
	}

 	@Override
	public C caseMapPattern(ASTMapPattern node, S arg)
	{
 		C all = newCollection();
 		
 		for (ASTMapletPattern maplet: node.maplets)
 		{
 			all.addAll(maplet.apply(this, arg));
 		}
 		
 		return all;
	}
 	
 	@Override
 	public C caseMapletPattern(ASTMapletPattern node, S arg)
 	{
 		C all = node.from.apply(this, arg);
		all.addAll(node.to.apply(this, arg));
		return all;
 	}

 	@Override
	public C caseMapUnionPattern(ASTMapUnionPattern node, S arg)
	{
 		C all = node.left.apply(this, arg);
 		all.addAll(node.right.apply(this, arg));
 		return all;
	}

 	@Override
	public C caseObjectPattern(ASTObjectPattern node, S arg)
	{
 		C all = newCollection();
 		
 		for (ASTNamePatternPair pair: node.fieldlist)
 		{
 			all.addAll(pair.pattern.apply(this, arg));
 		}
 		
 		return all;
	}

 	@Override
	public C caseRecordPattern(ASTRecordPattern node, S arg)
	{
 		C all = newCollection();
 		
 		for (ASTPattern pattern: node.plist)
 		{
 			all.addAll(pattern.apply(this, arg));
 		}
 		
 		return all;
	}

 	@Override
	public C caseSeqPattern(ASTSeqPattern node, S arg)
	{
		C all = newCollection();
 		
 		for (ASTPattern pattern: node.plist)
 		{
 			all.addAll(pattern.apply(this, arg));
 		}
 		
 		return all;
	}

 	@Override
	public C caseSetPattern(ASTSetPattern node, S arg)
	{
		C all = newCollection();
 		
 		for (ASTPattern pattern: node.plist)
 		{
 			all.addAll(pattern.apply(this, arg));
 		}
 		
 		return all;
	}

 	@Override
	public C caseTuplePattern(ASTTuplePattern node, S arg)
	{
		C all = newCollection();
 		
 		for (ASTPattern pattern: node.plist)
 		{
 			all.addAll(pattern.apply(this, arg));
 		}
 		
 		return all;
	}

 	@Override
	public C caseUnionPattern(ASTUnionPattern node, S arg)
	{
 		C all = node.left.apply(this, arg);
 		all.addAll(node.right.apply(this, arg));
 		return all;
	}

 	abstract protected C newCollection();
}
