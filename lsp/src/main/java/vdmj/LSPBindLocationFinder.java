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

package vdmj;

import java.util.HashSet;
import java.util.Set;

import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.tc.TCNode;
import com.fujitsu.vdmj.tc.TCVisitorSet;
import com.fujitsu.vdmj.tc.patterns.TCBind;
import com.fujitsu.vdmj.tc.patterns.TCSeqBind;
import com.fujitsu.vdmj.tc.patterns.TCSetBind;
import com.fujitsu.vdmj.tc.patterns.TCTypeBind;
import com.fujitsu.vdmj.tc.patterns.visitors.TCLeafBindVisitor;

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
		return node.unresolved.matchUnresolved(arg);
	}
	
	@Override
	public Set<TCNode> caseSetBind(TCSetBind node, LexLocation arg)
	{
		Set<TCNode> all = visitorSet.applyExpressionVisitor(node.set, arg);
		all.addAll(visitorSet.applyPatternVisitor(node.pattern, arg));
		return all;
	}
	
	@Override
	public Set<TCNode> caseSeqBind(TCSeqBind node, LexLocation arg)
	{
		Set<TCNode> all = visitorSet.applyExpressionVisitor(node.sequence, arg);
		all.addAll(visitorSet.applyPatternVisitor(node.pattern, arg));
		return all;
	}
}
