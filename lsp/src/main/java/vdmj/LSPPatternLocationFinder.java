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
 *	along with VDMJ.  If not, see &lt;http://www.gnu.org/licenses/&gt;.
 *	SPDX-License-Identifier: GPL-3.0-or-later
 *
 ******************************************************************************/

package vdmj;

import java.util.HashSet;
import java.util.Set;

import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.tc.TCNode;
import com.fujitsu.vdmj.tc.TCVisitorSet;
import com.fujitsu.vdmj.tc.patterns.TCIdentifierPattern;
import com.fujitsu.vdmj.tc.patterns.TCPattern;
import com.fujitsu.vdmj.tc.patterns.TCRecordPattern;
import com.fujitsu.vdmj.tc.patterns.visitors.TCLeafPatternVisitor;

/**
 * Lookup a LexLocation within a pattern, looking for record pattern matches.
 */
public class LSPPatternLocationFinder extends TCLeafPatternVisitor<TCNode, Set<TCNode>, LexLocation>
{
	public LSPPatternLocationFinder(TCVisitorSet<TCNode, Set<TCNode>, LexLocation> visitors)
	{
		visitorSet = visitors;
	}

	@Override
	protected Set<TCNode> newCollection()
	{
		return new HashSet<TCNode>();
	}

	@Override
	public Set<TCNode> casePattern(TCPattern node, LexLocation arg)
	{
		return newCollection();
	}
	
	@Override
	public Set<TCNode> caseIdentifierPattern(TCIdentifierPattern node, LexLocation arg)
	{
		Set<TCNode> all = newCollection();
		
		if (arg.touches(node.name.getLocation()))
		{
			all.add(node);
		}
		
		return all;
	}
	
	@Override
	public Set<TCNode> caseRecordPattern(TCRecordPattern node, LexLocation arg)
	{
		Set<TCNode> all = super.caseRecordPattern(node, arg);

		if (arg.touches(node.typename.getLocation()))
		{
			all.add(node.typename);
		}
		
		return all;
	}
}
