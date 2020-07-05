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

package com.fujitsu.vdmj.tc.patterns;

import java.util.Collection;

import com.fujitsu.vdmj.tc.TCVisitorSet;
import com.fujitsu.vdmj.tc.definitions.TCDefinitionVisitor;
import com.fujitsu.vdmj.tc.expressions.TCExpressionVisitor;
import com.fujitsu.vdmj.tc.statements.TCStatementVisitor;
import com.fujitsu.vdmj.tc.types.TCTypeVisitor;

/**
 * This TCPattern visitor visits all of the leaves of a pattern tree and calls
 * the basic processing methods for the simple patterns.
 */
public abstract class TCLeafPatternVisitor<E, C extends Collection<E>, S>
	extends TCPatternVisitor<C, S>
	implements TCVisitorSet<E, C, S>
{
 	@Override
	public C caseConcatenationPattern(TCConcatenationPattern node, S arg)
	{
 		C all = newCollection();
 		
 		all.addAll(node.left.apply(this, arg));
 		all.addAll(node.right.apply(this, arg));
 		
 		return all;
	}

 	@Override
	public C caseExpressionPattern(TCExpressionPattern node, S arg)
	{
		TCExpressionVisitor<C, S> expVisitor = getExpressionVisitor();
		return (expVisitor != null ? node.exp.apply(expVisitor, arg) : newCollection());
	}

 	@Override
	public C caseMapPattern(TCMapPattern node, S arg)
	{
 		C all = newCollection();
 		
 		for (TCMapletPattern maplet: node.maplets)
 		{
 			all.addAll(maplet.from.apply(this, arg));
 			all.addAll(maplet.to.apply(this, arg));
 		}
 		
 		return all;
	}

 	@Override
	public C caseMapUnionPattern(TCMapUnionPattern node, S arg)
	{
 		C all = newCollection();
 		
 		all.addAll(node.left.apply(this, arg));
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
 		C all = newCollection();
 		
 		all.addAll(node.left.apply(this, arg));
 		all.addAll(node.right.apply(this, arg));
 		
 		return all;
	}

 	abstract protected C newCollection();

	
 	@Override
	public TCDefinitionVisitor<C, S> getDefinitionVisitor()
 	{
 		return null;
 	}
	
 	@Override
	public TCExpressionVisitor<C, S> getExpressionVisitor()
 	{
 		return null;
 	}
 	
 	@Override
	public TCStatementVisitor<C, S> getStatementVisitor()
 	{
 		return null;
 	}

 	@Override
	public TCPatternVisitor<C, S> getPatternVisitor()
 	{
 		return null;
 	}
 	
 	@Override
	public TCTypeVisitor<C, S> getTypeVisitor()
 	{
 		return null;
 	}
}
