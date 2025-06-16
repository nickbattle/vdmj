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

package vdmj;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.tc.TCNode;
import com.fujitsu.vdmj.tc.TCVisitorSet;
import com.fujitsu.vdmj.tc.patterns.TCBind;
import com.fujitsu.vdmj.tc.patterns.TCIdentifierPattern;
import com.fujitsu.vdmj.tc.patterns.TCSeqBind;
import com.fujitsu.vdmj.tc.patterns.TCSetBind;
import com.fujitsu.vdmj.tc.patterns.TCTypeBind;
import com.fujitsu.vdmj.tc.patterns.visitors.TCLeafBindVisitor;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.TCUnknownType;
import com.fujitsu.vdmj.typechecker.NameScope;

public class LSPBindLocationFinder extends TCLeafBindVisitor<TCNode, Set<TCNode>, LexLocation>
{
	public LSPBindLocationFinder(TCVisitorSet<TCNode, Set<TCNode>, LexLocation> visitors)
	{
		visitorSet = visitors;
	}

	@Override
	protected Set<TCNode> newCollection()
	{
		return new HashSet<TCNode>();
	}

	@Override
	public Set<TCNode> caseBind(TCBind bind, LexLocation arg)
	{
		return newCollection();
	}
	
	@Override
	public Set<TCNode> caseTypeBind(TCTypeBind node, LexLocation arg)
	{
		Set<TCNode> all = super.caseTypeBind(node, arg);
		patternCheck(all, node.type);
		all.addAll(node.unresolved.matchUnresolved(arg));
		return all;
	}
	
	@Override
	public Set<TCNode> caseSetBind(TCSetBind node, LexLocation arg)
	{
		Set<TCNode> all = super.caseSetBind(node, arg);
		patternCheck(all, new TCUnknownType(node.location));
		return all;
	}
	
	@Override
	public Set<TCNode> caseSeqBind(TCSeqBind node, LexLocation arg)
	{
		Set<TCNode> all = super.caseSeqBind(node, arg);
		patternCheck(all, new TCUnknownType(node.location));
		return all;
	}
	
	private void patternCheck(Set<TCNode> all, TCType type)
	{
		Set<TCNode> defs = new HashSet<TCNode>();
		Iterator<TCNode> iter = all.iterator();
		
		while (iter.hasNext())
		{
			TCNode node = iter.next();
			
			if (node instanceof TCIdentifierPattern)
			{
				TCIdentifierPattern p = (TCIdentifierPattern)node;
				iter.remove();
				defs.addAll(p.getDefinitions(type, NameScope.LOCAL));
			}
		}
		
		all.addAll(defs);
	}
}
