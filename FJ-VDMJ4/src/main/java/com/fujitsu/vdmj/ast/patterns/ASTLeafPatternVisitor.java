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
 *
 ******************************************************************************/

package com.fujitsu.vdmj.ast.patterns;

import java.util.Collection;

import com.fujitsu.vdmj.ast.expressions.ASTLeafExpressionVisitor;

/**
 * This TCPattern visitor visits all of the leaves of a pattern tree and calls
 * the basic processing methods for the simple patterns.
 */
public abstract class ASTLeafPatternVisitor<E, C extends Collection<E>, S> extends ASTPatternVisitor<C, S>
{
 	@Override
	public C caseConcatenationPattern(ASTConcatenationPattern node, S arg)
	{
 		C all = newCollection();
 		
 		all.addAll(node.left.apply(this, arg));
 		all.addAll(node.right.apply(this, arg));
 		
 		return all;
	}

 	@Override
	public C caseExpressionPattern(ASTExpressionPattern node, S arg)
	{
		ASTLeafExpressionVisitor<E, C, S> expVisitor = getExpressionVisitor();
		return (expVisitor != null ? node.exp.apply(expVisitor, arg) : newCollection());
	}

 	@Override
	public C caseMapPattern(ASTMapPattern node, S arg)
	{
 		C all = newCollection();
 		
 		for (ASTMapletPattern maplet: node.maplets)
 		{
 			all.addAll(maplet.from.apply(this, arg));
 			all.addAll(maplet.to.apply(this, arg));
 		}
 		
 		return all;
	}

 	@Override
	public C caseMapUnionPattern(ASTMapUnionPattern node, S arg)
	{
 		C all = newCollection();
 		
 		all.addAll(node.left.apply(this, arg));
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
 		C all = newCollection();
 		
 		all.addAll(node.left.apply(this, arg));
 		all.addAll(node.right.apply(this, arg));
 		
 		return all;
	}

 	abstract protected C newCollection();

 	abstract protected ASTLeafExpressionVisitor<E, C, S> getExpressionVisitor();
}
